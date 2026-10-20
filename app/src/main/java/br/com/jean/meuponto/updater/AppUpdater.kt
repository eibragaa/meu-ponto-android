package br.com.jean.meuponto.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionName: String,
    val releaseName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkFileName: String,
    val apkSize: Long
)

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateDownloadState
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}

object AppUpdater {
    const val GITHUB_OWNER = "eibragaa"
    const val GITHUB_REPO = "meu-ponto-android"
    private const val GITHUB_API_LATEST = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

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

    suspend fun checkForUpdates(currentVersion: String): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(GITHUB_API_LATEST)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "MeuPonto-App")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 10_000
                readTimeout = 15_000
            }

            val statusCode = connection.responseCode
            if (statusCode != HttpURLConnection.HTTP_OK) {
                return@runCatching null
            }

            val jsonString = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(jsonString)

            val tagName = json.optString("tag_name", "").trim()
            val releaseName = json.optString("name", tagName).trim()
            val releaseNotes = json.optString("body", "").trim()
            val assets = json.optJSONArray("assets") ?: return@runCatching null

            var apkUrl: String? = null
            var apkFileName: String? = null
            var apkSize: Long = 0L

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    val downloadUrl = asset.optString("browser_download_url", "")
                    if (downloadUrl.isNotBlank()) {
                        apkUrl = downloadUrl
                        apkFileName = name
                        apkSize = asset.optLong("size", 0L)
                        // If it matches Meu-Ponto-*.apk, favor it immediately
                        if (name.contains("Meu-Ponto", ignoreCase = true)) {
                            break
                        }
                    }
                }
            }

            if (apkUrl.isNullOrBlank() || apkFileName.isNullOrBlank()) {
                return@runCatching null
            }

            val remoteVersion = tagName.trimStart('v', 'V')
            if (isNewerVersion(remoteVersion, currentVersion)) {
                AppUpdateInfo(
                    versionName = remoteVersion,
                    releaseName = releaseName.ifBlank { "Versão $remoteVersion" },
                    releaseNotes = releaseNotes,
                    apkUrl = apkUrl,
                    apkFileName = apkFileName,
                    apkSize = apkSize
                )
            } else {
                null
            }
        }
    }

    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        fileName: String,
        onProgress: (progress: Float, downloaded: Long, total: Long) -> Unit
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

        if (targetFile.exists()) {
            targetFile.delete()
        }
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }

        targetFile
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
