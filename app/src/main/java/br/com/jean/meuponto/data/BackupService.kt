package br.com.jean.meuponto.data

import android.content.Context
import android.net.Uri
import br.com.jean.meuponto.domain.DayOccurrence
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
 * Backup e restauração manual do banco de comprovantes.
 *
 * Formato do arquivo, na ordem: `BACKUP_MAGIC` + 1 byte de versão + salt(16) + iv(12) + payload
 * AES-GCM. Sem senha, o arquivo é apenas o ZIP (imagens + `manifest.json`).
 *
 * O manifest guarda os campos da tabela `receipts`, exceto a imagem, que vai em `images/<id>.jpg`.
 * Registros manuais sem imagem são devidamente preservados com `imageEntry = ""`.
 * A restauração é defensiva por natureza (arquivo vem de fora): valida número de entradas, tamanho
 * por arquivo, path traversal e quantidade de registros antes de tocar no banco.
 */

internal val BACKUP_MAGIC = "MEUPONTO_BACKUP_V2".toByteArray(Charsets.US_ASCII)
internal const val BACKUP_VERSION: Byte = 1
internal const val BACKUP_ITERATIONS = 120_000
internal const val MAX_RESTORE_ZIP_ENTRIES = 2_000
internal const val MAX_RESTORE_IMAGE_BYTES = 48L * 1024L * 1024L
internal const val MAX_RESTORE_MANIFEST_BYTES = 8L * 1024L * 1024L
internal const val MAX_RESTORE_MANIFEST_ITEMS = 5_000

/**
 * Gera o backup em [destination] e devolve quantos comprovantes entraram no manifest.
 *
 * Comprovantes com imagem no disco têm o arquivo empacotado; comprovantes manuais sem imagem
 * são preservados no manifest. O ZIP é montado em `cacheDir` e apagado no `finally`, porque o destino
 * é um `Uri` de SAF.
 */
internal suspend fun createBackup(context: Context, dao: ReceiptDao, destination: Uri, password: String): Int = withContext(Dispatchers.IO) {
    val receipts = dao.getAll()
    val manifest = JSONArray()
    val zipFile = File.createTempFile("meu-ponto-backup-", ".zip", context.cacheDir)
    try {
        ZipOutputStream(BufferedOutputStream(zipFile.outputStream())).use { zip ->
            receipts.forEach { receipt ->
                val source = File(receipt.imagePath)
                val hasImage = receipt.imagePath.isNotBlank() && source.isFile
                val imageEntry = if (hasImage) "images/${receipt.id}.jpg" else ""
                if (hasImage) {
                    zip.putNextEntry(ZipEntry(imageEntry))
                    source.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
                manifest.put(
                    JSONObject()
                        .put("capturedAt", receipt.capturedAt)
                        .put("workDate", receipt.workDate)
                        .put("workTime", receipt.workTime)
                        .put("entryTime", receipt.entryTime ?: "")
                        .put("exitTime", receipt.exitTime ?: "")
                        .put("needsReview", receipt.needsReview)
                        .put("occurrence", receipt.occurrence?.name ?: "")
                        .put("recognizedText", receipt.recognizedText)
                        .put("imageEntry", imageEntry)
                )
            }
            zip.putNextEntry(ZipEntry("manifest.json"))
            manifest.toString().byteInputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
        context.contentResolver.openOutputStream(destination)?.use { output ->
            if (password.isNotBlank()) {
                writeEncryptedPayload(zipFile, output, password)
            } else {
                zipFile.inputStream().use { it.copyTo(output) }
            }
        } ?: error("Falha ao abrir o arquivo de destino")
        receipts.size
    } finally {
        zipFile.delete()
    }
}

/**
 * Restaura comprovantes de [source] para o banco.
 *
 * Valida o arquivo ZIP de forma defensiva antes de gravar no banco: path traversal dentro do ZIP,
 * número máximo de arquivos e limite de bytes por imagem. Arquivos duplicados no banco (mesma data,
 * horários e texto) são ignorados para não poluir o histórico.
 */
internal suspend fun restoreBackup(context: Context, dao: ReceiptDao, source: Uri, password: String): Int = withContext(Dispatchers.IO) {
    val restoreDir = File.createTempFile("meu-ponto-restore-", "", context.cacheDir).apply {
        delete()
        mkdirs()
    }
    val zipFile = File.createTempFile("meu-ponto-restore-payload-", ".zip", context.cacheDir)
    try {
        context.contentResolver.openInputStream(source)?.use { input ->
            val buffered = BufferedInputStream(input)
            if (isEncryptedStream(buffered)) {
                readEncryptedPayload(buffered, zipFile, password)
            } else {
                zipFile.outputStream().use { output -> buffered.copyTo(output) }
            }
        } ?: error("Falha ao ler o arquivo de backup")

        var entryCount = 0
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entryCount++
                if (entryCount > MAX_RESTORE_ZIP_ENTRIES) error("Backup excede o limite de arquivos")
                val target = File(restoreDir, entry.name).canonicalFile
                if (!target.path.startsWith(restoreDir.canonicalPath)) {
                    error("Entrada inválida no backup (path traversal)")
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out ->
                        val copied = zip.copyTo(out)
                        val maxBytes = if (entry.name == "manifest.json") MAX_RESTORE_MANIFEST_BYTES else MAX_RESTORE_IMAGE_BYTES
                        if (copied > maxBytes) error("Arquivo no backup excede o tamanho permitido")
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val manifestFile = File(restoreDir, "manifest.json")
        if (!manifestFile.isFile) error("Manifesto do backup ausente")
        val items = JSONArray(manifestFile.readText(Charsets.UTF_8))
        if (items.length() > MAX_RESTORE_MANIFEST_ITEMS) error("Backup excede o limite de registros")
        val destinationDir = File(context.filesDir, "receipts").apply { mkdirs() }
        val restored = buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val imageEntry = item.optString("imageEntry", "")
                val imageSource = if (imageEntry.isNotBlank()) File(restoreDir, imageEntry) else null
                val hasImage = imageSource?.isFile == true

                val candidate = Receipt(
                    id = 0,
                    capturedAt = item.getLong("capturedAt"),
                    workDate = item.getString("workDate"),
                    workTime = item.getString("workTime"),
                    imagePath = if (hasImage) imageSource!!.absolutePath else "",
                    recognizedText = item.optString("recognizedText"),
                    entryTime = item.optString("entryTime").takeIf { it.isNotBlank() },
                    exitTime = item.optString("exitTime").takeIf { it.isNotBlank() },
                    needsReview = item.optBoolean("needsReview", false) ||
                        WorkStatsCalculator.parseWorkDate(item.getString("workDate")) == null,
                    // Backups antigos não têm o campo: ficam sem ocorrência (comprovante normal).
                    occurrence = item.optString("occurrence").takeIf { it.isNotBlank() }
                        ?.let { runCatching { DayOccurrence.valueOf(it) }.getOrNull() }
                )
                val alreadyRestored = dao.getByCapturedAt(candidate.capturedAt).any { existing ->
                    existing.workDate == candidate.workDate &&
                        existing.workTime == candidate.workTime &&
                        existing.entryTime == candidate.entryTime &&
                        existing.exitTime == candidate.exitTime &&
                        existing.recognizedText == candidate.recognizedText &&
                        existing.occurrence == candidate.occurrence
                }
                if (alreadyRestored) continue

                if (hasImage) {
                    val imageDestination = File(
                        destinationDir,
                        "restored_${candidate.capturedAt}_${System.currentTimeMillis()}_$index.jpg"
                    )
                    imageSource!!.copyTo(imageDestination, overwrite = true)
                    add(candidate.copy(imagePath = imageDestination.absolutePath))
                } else {
                    add(candidate.copy(imagePath = ""))
                }
            }
        }
        dao.insertAll(restored)
        restored.size
    } finally {
        restoreDir.deleteRecursively()
        zipFile.delete()
    }
}

/** Verifica se os primeiros bytes batem com [BACKUP_MAGIC] sem consumir o stream. */
private fun isEncryptedStream(stream: BufferedInputStream): Boolean {
    stream.mark(BACKUP_MAGIC.size)
    val header = ByteArray(BACKUP_MAGIC.size)
    val read = stream.read(header)
    stream.reset()
    return read == BACKUP_MAGIC.size && header.contentEquals(BACKUP_MAGIC)
}

private fun writeEncryptedPayload(source: File, destination: java.io.OutputStream, password: String) {
    destination.write(BACKUP_MAGIC)
    destination.write(BACKUP_VERSION.toInt())

    val random = SecureRandom()
    val salt = ByteArray(16).also { random.nextBytes(it) }
    val iv = ByteArray(12).also { random.nextBytes(it) }
    destination.write(salt)
    destination.write(iv)

    val key = deriveKey(password, salt)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
    }

    CipherOutputStream(destination, cipher).use { cipherOut ->
        source.inputStream().use { input -> input.copyTo(cipherOut) }
    }
}

private fun readEncryptedPayload(source: BufferedInputStream, destination: File, password: String) {
    if (password.isBlank()) error("Este backup é protegido por senha")
    val magic = ByteArray(BACKUP_MAGIC.size)
    if (source.read(magic) != BACKUP_MAGIC.size || !magic.contentEquals(BACKUP_MAGIC)) {
        error("Formato de backup inválido")
    }
    val version = source.read()
    if (version != BACKUP_VERSION.toInt()) error("Versão de backup não suportada: $version")

    val salt = ByteArray(16)
    if (source.read(salt) != salt.size) error("Cabeçalho de backup corrompido")
    val iv = ByteArray(12)
    if (source.read(iv) != iv.size) error("Cabeçalho de backup corrompido")

    val key = deriveKey(password, salt)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
    }

    CipherInputStream(source, cipher).use { cipherIn ->
        destination.outputStream().use { output ->
            try {
                cipherIn.copyTo(output)
            } catch (e: Exception) {
                error("Senha incorreta ou arquivo corrompido")
            }
        }
    }
}

private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(password.toCharArray(), salt, BACKUP_ITERATIONS, 256)
    return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
}
