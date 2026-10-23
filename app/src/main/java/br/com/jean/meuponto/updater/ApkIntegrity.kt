package br.com.jean.meuponto.updater

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/** Tamanho de um SHA-256 em hexadecimal; digest publicado em outro tamanho é inválido. */
internal const val SHA256_HEX_LENGTH = 64

/** Mensagem única para divergência de checksum, exibida no diálogo e verificável em teste. */
internal const val CHECKSUM_MISMATCH_MESSAGE =
    "O APK baixado não confere com o checksum publicado. Tente baixar novamente."

private const val SHA256_ALGORITHM = "SHA-256"
private const val SHA256_PREFIX = "sha256:"
private const val HASH_BUFFER_SIZE = 8_192

/**
 * Normaliza um digest SHA-256 publicado para hexadecimal minúsculo.
 *
 * Aceita `sha256:ABC...` (prefixo sem diferenciar maiúsculas) e `ABC...`; devolve `null` quando o
 * valor está vazio, tem tamanho diferente de [SHA256_HEX_LENGTH] ou contém caractere não hexadecimal.
 */
internal fun normalizeSha256(digest: String?): String? {
    val raw = digest?.trim().orEmpty()
    if (raw.isEmpty()) return null

    val hash = if (raw.startsWith(SHA256_PREFIX, ignoreCase = true)) {
        raw.substring(SHA256_PREFIX.length).trim()
    } else {
        raw
    }
    if (hash.length != SHA256_HEX_LENGTH) return null
    if (!hash.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null

    return hash.lowercase()
}

/**
 * Compara o digest publicado com o calculado localmente.
 *
 * `false` quando o esperado está ausente ou malformado: nesse caso o download não deve ser aceito.
 */
internal fun sha256Matches(expected: String?, actual: String): Boolean {
    val expectedHash = normalizeSha256(expected) ?: return false
    val actualHash = normalizeSha256(actual) ?: return false
    return expectedHash == actualHash
}

/** Calcula o SHA-256 de [file] em hexadecimal minúsculo; propaga erro de leitura do arquivo. */
internal fun sha256OfFile(file: File): String {
    val digest = MessageDigest.getInstance(SHA256_ALGORITHM)
    file.inputStream().use { input ->
        val buffer = ByteArray(HASH_BUFFER_SIZE)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().toHex()
}

/** SHA-256 do certificado DER (não da assinatura), no mesmo formato usado na comparação. */
internal fun certificateSha256(certificate: ByteArray): String =
    MessageDigest.getInstance(SHA256_ALGORITHM).digest(certificate).toHex()

/**
 * `true` quando os conjuntos de certificados são iguais e ambos não vazios.
 *
 * Conjunto vazio significa que a leitura falhou; devolver `false` obriga o chamador a tratar como
 * [ApkSignatureStatus.UNKNOWN], nunca como assinatura compatível.
 */
internal fun signaturesMatch(installed: Set<String>, downloaded: Set<String>): Boolean {
    if (installed.isEmpty() || downloaded.isEmpty()) return false

    val installedHashes = installed.mapTo(mutableSetOf<String>()) { it.trim().lowercase() }
    val downloadedHashes = downloaded.mapTo(mutableSetOf<String>()) { it.trim().lowercase() }
    return installedHashes == downloadedHashes
}

/**
 * Lê os hashes dos certificados do app instalado.
 *
 * Devolve `null` quando o pacote não é encontrado ou o certificado não pode ser lido; conjunto vazio
 * também é tratado como leitura indisponível por [signaturesMatch].
 */
@Suppress("DEPRECATION")
internal fun readInstalledCertificateHashes(context: Context): Set<String>? {
    val packageManager = context.packageManager
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            info.signingInfo?.apkContentsSigners
                ?.mapNotNullTo(mutableSetOf<String>()) { it.certificateHashOrNull() }
        } else {
            packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                .signatures
                ?.mapNotNullTo(mutableSetOf<String>()) { it.certificateHashOrNull() }
        }
    } catch (_: Exception) {
        // Pacote ausente ou leitura bloqueada: sem referência, o chamador trata como UNKNOWN.
        null
    }
}

/** Lê os hashes dos certificados de um APK ainda não instalado; `null` se o arquivo for inválido. */
@Suppress("DEPRECATION")
internal fun readApkCertificateHashes(context: Context, apkFile: File): Set<String>? {
    if (!apkFile.isFile) return null

    val packageManager = context.packageManager
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_SIGNING_CERTIFICATES
            ) ?: return null
            info.signingInfo?.apkContentsSigners
                ?.mapNotNullTo(mutableSetOf<String>()) { it.certificateHashOrNull() }
        } else {
            val info = packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_SIGNATURES
            ) ?: return null
            info.signatures
                ?.mapNotNullTo(mutableSetOf<String>()) { it.certificateHashOrNull() }
        }
    } catch (_: Exception) {
        // APK inválido ou leitura bloqueada: o chamador trata como UNKNOWN.
        null
    }
}

private fun Signature.certificateHashOrNull(): String? = try {
    certificateSha256(toByteArray())
} catch (_: NoSuchAlgorithmException) {
    // SHA-256 é obrigatório na plataforma; se faltar, não há como comparar com segurança.
    null
}

private fun ByteArray.toHex(): String = joinToString("") { byte ->
    (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
}
