package br.com.jean.meuponto.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateDownloadState
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadState

    /** APK baixado tem chave diferente da instalada; não deve ser aberto no instalador (P17). */
    data object SignatureMismatch : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}

object AppUpdater {
    const val GITHUB_OWNER = "eibragaa"
    const val GITHUB_REPO = "meu-ponto-android"
    private const val GITHUB_API_LATEST = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    /** Tempo máximo para abrir a conexão com a API do GitHub. */
    const val UPDATE_CONNECT_TIMEOUT_MS = 10_000

    /** Tempo máximo para ler a resposta da API; o GitHub pode demorar em redes lentas. */
    const val UPDATE_READ_TIMEOUT_MS = 15_000

    fun getInstalledVersionName(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName ?: "0.0.0"
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
            }
        } catch (_: Exception) {
            "0.0.0"
        }
    }

    /**
     * Comparação de versão tolerante ao formato usado nas tags (`v0.5.1`, `0.5.1`, `0.5.1-beta`).
     *
     * Cada parte é comparada numericamente e o que não é dígito é descartado; partes faltantes valem
     * zero, então `0.5` e `0.5.0` são iguais (e não uma atualização).
     */
    fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        val remoteNumbers = remoteVersion.trim().trimStart('v', 'V')
            .split('.')
            .map { it.takeWhile { char -> char.isDigit() }.toIntOrNull() ?: 0 }
        val currentNumbers = currentVersion.trim().trimStart('v', 'V')
            .split('.')
            .map { it.takeWhile { char -> char.isDigit() }.toIntOrNull() ?: 0 }

        val maxParts = maxOf(remoteNumbers.size, currentNumbers.size)
        for (i in 0 until maxParts) {
            val r = remoteNumbers.getOrElse(i) { 0 }
            val c = currentNumbers.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    suspend fun checkForUpdates(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            requestLatestRelease(currentVersion)
        } catch (error: CancellationException) {
            throw error
        } catch (_: UnknownHostException) {
            UpdateCheckResult.Failed(UpdateFailure.OFFLINE)
        } catch (_: IOException) {
            UpdateCheckResult.Failed(UpdateFailure.OFFLINE)
        } catch (_: JSONException) {
            UpdateCheckResult.Failed(UpdateFailure.INVALID_RESPONSE)
        }
    }

    private fun requestLatestRelease(currentVersion: String): UpdateCheckResult {
        val connection = (URL(GITHUB_API_LATEST).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "MeuPonto-App")
            setRequestProperty("Accept", "application/vnd.github.v3+json")
            connectTimeout = UPDATE_CONNECT_TIMEOUT_MS
            readTimeout = UPDATE_READ_TIMEOUT_MS
        }

        return try {
            when (val statusCode = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val jsonString = connection.inputStream
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    val payload = parseLatestRelease(jsonString)
                    if (payload == null) {
                        UpdateCheckResult.Failed(UpdateFailure.INVALID_RESPONSE)
                    } else {
                        evaluateRelease(payload, currentVersion)
                    }
                }
                else -> if (isReleaseMissingStatus(statusCode)) {
                    UpdateCheckResult.UpToDate
                } else {
                    UpdateCheckResult.Failed(failureForHttpStatus(statusCode))
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Baixa o APK do release para `cacheDir/updates`.
     *
     * O GitHub responde com redirect para o CDN, por isso o laço de redirecionamento; o download vai
     * para um `.tmp` e só vira o arquivo final ao terminar, evitando instalar APK truncado. Quando
     * [expectedSha256] é informado, o `.tmp` só é promovido se o SHA-256 conferir; em divergência ele
     * é apagado e o erro pede novo download.
     * O cache é limpo por [br.com.jean.meuponto.data.cleanupUpdateAndPdfCaches].
     */
    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        fileName: String,
        onProgress: (progress: Float, downloaded: Long, total: Long) -> Unit,
        expectedSha256: String? = null
    ): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(updatesDir, fileName)
        val tempFile = File(updatesDir, "$fileName.tmp")

        var currentUrl = apkUrl
        var connection: HttpURLConnection
        var redirects = 0

        while (true) {
            val url = URL(currentUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "MeuPonto-App")
                instanceFollowRedirects = true
                connectTimeout = 15_000
                readTimeout = 30_000
            }

            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == 307 || status == 308
            ) {
                redirects++
                if (redirects > 8) error("Muitos redirecionamentos ao baixar atualização")
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                currentUrl = location ?: error("Redirecionamento sem cabeçalho Location")
                continue
            }

            if (status != HttpURLConnection.HTTP_OK) {
                error("Falha no download (código HTTP $status)")
            }
            break
        }

        val totalBytes = connection.contentLength.toLong()
        var downloadedBytes = 0L

        connection.inputStream.use { input ->
            tempFile.outputStream().use { output ->
                val buffer = ByteArray(16384)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloadedBytes += read
                    val progress = if (totalBytes > 0) {
                        (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    } else {
                        -1f
                    }
                    onProgress(progress, downloadedBytes, totalBytes)
                }
            }
        }

        if (!expectedSha256.isNullOrBlank()) {
            val actualSha256 = sha256OfFile(tempFile)
            if (!sha256Matches(expectedSha256, actualSha256)) {
                tempFile.delete()
                throw IOException(CHECKSUM_MISMATCH_MESSAGE)
            }
        }

        if (targetFile.exists()) {
            targetFile.delete()
        }
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }

        targetFile
    }

    /**
     * Compara o certificado do APK baixado com o do app instalado antes de abrir o instalador (P17).
     *
     * [ApkSignatureStatus.UNKNOWN] cobre APK inválido, pacote instalado não encontrado e falha de
     * leitura; nesse caso o fluxo segue, porque o Android ainda recusa a instalação se as chaves não
     * forem compatíveis. [ApkSignatureStatus.DIFFERENT] é conclusivo e deve bloquear a instalação.
     */
    fun checkApkSignature(context: Context, apkFile: File): ApkSignatureStatus {
        val installed = readInstalledCertificateHashes(context) ?: return ApkSignatureStatus.UNKNOWN
        val downloaded = readApkCertificateHashes(context, apkFile) ?: return ApkSignatureStatus.UNKNOWN
        if (installed.isEmpty() || downloaded.isEmpty()) return ApkSignatureStatus.UNKNOWN

        return if (signaturesMatch(installed, downloaded)) {
            ApkSignatureStatus.SAME
        } else {
            ApkSignatureStatus.DIFFERENT
        }
    }

    fun canInstallUnknownApps(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun createManageUnknownAppSourcesIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun launchInstaller(context: Context, apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
