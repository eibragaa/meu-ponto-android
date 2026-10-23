package br.com.jean.meuponto.data

import android.content.Context
import android.net.Uri
import br.com.jean.meuponto.domain.WorkStatsCalculator
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup e restauração manual (ZIP + JSON, opcionalmente cifrado com AES-GCM).
 */

internal val BackupMagic = "MEUPONTO_BACKUP_V2".toByteArray(Charsets.US_ASCII)
internal const val BackupVersion: Byte = 1
internal const val BackupIterations = 120_000
internal const val MaxRestoreZipEntries = 2_000
internal const val MaxRestoreImageBytes = 48L * 1024L * 1024L
internal const val MaxRestoreManifestBytes = 8L * 1024L * 1024L
internal const val MaxRestoreManifestItems = 5_000

internal suspend fun createBackup(context: Context, dao: ReceiptDao, destination: Uri, password: String): Int = withContext(Dispatchers.IO) {
    val receipts = dao.getAll()
    val manifest = JSONArray()
    val zipFile = File.createTempFile("meu-ponto-backup-", ".zip", context.cacheDir)
    try {
        ZipOutputStream(BufferedOutputStream(zipFile.outputStream())).use { zip ->
            receipts.forEach { receipt ->
                val source = File(receipt.imagePath)
                if (!source.isFile) return@forEach
                val imageEntry = "images/${receipt.id}.jpg"
                zip.putNextEntry(ZipEntry(imageEntry))
                source.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
                manifest.put(
                    JSONObject()
                        .put("capturedAt", receipt.capturedAt)
                        .put("workDate", receipt.workDate)
                        .put("workTime", receipt.workTime)
                        .put("entryTime", receipt.entryTime ?: "")
                        .put("exitTime", receipt.exitTime ?: "")
                        .put("needsReview", receipt.needsReview)
                        .put("recognizedText", receipt.recognizedText)
                        .put("imageEntry", imageEntry)
                )
            }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        if (password.isBlank()) {
            context.contentResolver.openOutputStream(destination)?.use { output ->
                zipFile.inputStream().use { it.copyTo(output) }
            } ?: error("Destino de backup indisponível")
        } else {
            encryptBackup(zipFile, context, destination, password)
        }
        manifest.length()
    } finally {
        zipFile.delete()
    }
}

internal suspend fun restoreBackup(context: Context, dao: ReceiptDao, sourceUri: Uri, password: String): Int = withContext(Dispatchers.IO) {
    val restoreDir = File(context.cacheDir, "restore_${System.currentTimeMillis()}").apply { mkdirs() }
    val zipFile = File.createTempFile("meu-ponto-restore-", ".zip", context.cacheDir)
    try {
        decryptOrCopyBackup(context, sourceUri, zipFile, password)
        zipFile.inputStream().use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entries = 0
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries++
                    if (entries > MaxRestoreZipEntries) error("Backup excede o limite de arquivos")
                    if (!entry.isDirectory &&
                        entry.name != "manifest.json" &&
                        !entry.name.startsWith("images/")
                    ) {
                        error("Backup com arquivo inesperado")
                    }
                    val maximumBytes = when {
                        entry.isDirectory -> 0L
                        entry.name == "manifest.json" -> MaxRestoreManifestBytes
                        else -> MaxRestoreImageBytes
                    }
                    if (entry.size > maximumBytes) error("Backup excede o tamanho permitido")
                    val target = File(restoreDir, entry.name)
                    val rootPath = restoreDir.canonicalPath + File.separator
                    if (!target.canonicalPath.startsWith(rootPath)) error("Backup inválido")
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { output ->
                            val buffer = ByteArray(16384)
                            var total = 0
                            while (true) {
                                val read = zip.read(buffer)
                                if (read == -1) break
                                total += read
                                if (total > maximumBytes) error("Backup excede o tamanho permitido")
                                output.write(buffer, 0, read)
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
        } ?: error("Arquivo de backup indisponível")

        val manifestFile = File(restoreDir, "manifest.json")
        if (!manifestFile.isFile) error("Manifesto do backup ausente")
        val items = JSONArray(manifestFile.readText(Charsets.UTF_8))
        if (items.length() > MaxRestoreManifestItems) error("Backup excede o limite de registros")
        val destinationDir = File(context.filesDir, "receipts").apply { mkdirs() }
        val restored = buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val imageSource = File(restoreDir, item.getString("imageEntry"))
                if (!imageSource.isFile) continue
                val candidate = Receipt(
                    id = 0,
                    capturedAt = item.getLong("capturedAt"),
                    workDate = item.getString("workDate"),
                    workTime = item.getString("workTime"),
                    imagePath = imageSource.absolutePath,
                    recognizedText = item.optString("recognizedText"),
                    entryTime = item.optString("entryTime").takeIf { it.isNotBlank() },
                    exitTime = item.optString("exitTime").takeIf { it.isNotBlank() },
                    needsReview = item.optBoolean("needsReview", false) ||
                        WorkStatsCalculator.parseWorkDate(item.getString("workDate")) == null
                )
                val alreadyRestored = dao.getByCapturedAt(candidate.capturedAt).any { existing ->
                    existing.workDate == candidate.workDate &&
                        existing.workTime == candidate.workTime &&
                        existing.entryTime == candidate.entryTime &&
                        existing.exitTime == candidate.exitTime &&
                        existing.recognizedText == candidate.recognizedText
                }
                if (alreadyRestored) continue
                val imageDestination = File(
                    destinationDir,
                    "restored_${candidate.capturedAt}_${System.currentTimeMillis()}_$index.jpg"
                )
                imageSource.copyTo(imageDestination, overwrite = true)
                add(candidate.copy(imagePath = imageDestination.absolutePath))
            }
        }
        dao.insertAll(restored)
        restored.size
    } finally {
        restoreDir.deleteRecursively()
        zipFile.delete()
    }
}

private fun deriveBackupKey(password: String, salt: ByteArray): SecretKeySpec {
    val spec = PBEKeySpec(password.toCharArray(), salt, BackupIterations, 256)
    return try {
        SecretKeySpec(
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
            "AES"
        )
    } finally {
        spec.clearPassword()
    }
}

private fun encryptBackup(source: File, context: Context, destination: Uri, password: String) {
    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(Cipher.ENCRYPT_MODE, deriveBackupKey(password, salt), GCMParameterSpec(128, iv))
    }
    context.contentResolver.openOutputStream(destination)?.use { output ->
        output.write(BackupMagic)
        output.write(byteArrayOf(BackupVersion))
        output.write(salt)
        output.write(iv)
        CipherOutputStream(output, cipher).use { encrypted ->
            source.inputStream().use { it.copyTo(encrypted) }
        }
    } ?: error("Destino de backup indisponível")
}

private fun decryptOrCopyBackup(context: Context, sourceUri: Uri, destination: File, password: String) {
    context.contentResolver.openInputStream(sourceUri)?.use { input ->
        val header = ByteArray(BackupMagic.size)
        var read = 0
        while (read < header.size) {
            val count = input.read(header, read, header.size - read)
            if (count <= 0) break
            read += count
        }
        val encrypted = read == BackupMagic.size && header.contentEquals(BackupMagic)
        if (!encrypted) {
            destination.outputStream().use { output ->
                output.write(header, 0, read)
                input.copyTo(output)
            }
            return
        }
        val version = input.read()
        if (version != BackupVersion.toInt()) error("Versão de backup não suportada")
        val salt = ByteArray(16).also { input.readFully(it) }
        val iv = ByteArray(12).also { input.readFully(it) }
        if (password.isBlank()) error("Este backup precisa de uma senha")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, deriveBackupKey(password, salt), GCMParameterSpec(128, iv))
        }
        try {
            CipherInputStream(input, cipher).use { decrypted ->
                destination.outputStream().use { output -> decrypted.copyTo(output) }
            }
        } catch (_: Exception) {
            throw IllegalArgumentException("Senha incorreta ou backup danificado")
        }
    } ?: error("Arquivo de backup indisponível")
}

private fun java.io.InputStream.readFully(buffer: ByteArray) {
    var offset = 0
    while (offset < buffer.size) {
        val count = read(buffer, offset, buffer.size - offset)
        if (count <= 0) error("Backup incompleto")
        offset += count
    }
}
