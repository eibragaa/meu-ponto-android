package br.com.jean.meuponto.updater

import org.json.JSONException
import org.json.JSONObject

/** Limite de caracteres das notas guardadas/exibidas; evita releases gigantes na UI. */
internal const val MAX_RELEASE_NOTES_CHARS = 4_000

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVER_ERROR_MIN = 500
private const val HTTP_SERVER_ERROR_MAX = 599

/** Asset de APK extraído da release. */
internal data class ApkAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long
)

/** Campos da release usados pelo updater, já separados do JSON. */
internal data class ReleasePayload(
    val tag: String,
    val name: String,
    val body: String,
    val releaseUrl: String,
    val assets: List<ApkAsset>
)

/**
 * Lê o JSON de `/releases/latest`.
 *
 * É a única função do updater que conhece `org.json`; devolve `null` quando o corpo não é um
 * release válido (JSON malformado ou sem `tag_name`), para o chamador tratar como
 * [UpdateFailure.INVALID_RESPONSE].
 */
internal fun parseLatestRelease(json: String): ReleasePayload? {
    return try {
        val root = JSONObject(json)
        val tag = root.optString("tag_name", "").trim()
        if (tag.isBlank()) return null

        val assetsJson = root.optJSONArray("assets")
        val assets = (0 until (assetsJson?.length() ?: 0)).mapNotNull { index ->
            val asset = assetsJson?.optJSONObject(index) ?: return@mapNotNull null
            val name = asset.optString("name", "").trim()
            if (name.isBlank()) return@mapNotNull null
            ApkAsset(
                name = name,
                downloadUrl = asset.optString("browser_download_url", "").trim(),
                size = asset.optLong("size", 0L)
            )
        }

        ReleasePayload(
            tag = tag,
            name = root.optString("name", "").trim(),
            body = root.optString("body", "").trim().take(MAX_RELEASE_NOTES_CHARS),
            releaseUrl = root.optString("html_url", "").trim(),
            assets = assets
        )
    } catch (_: JSONException) {
        null
    }
}

/**
 * Decide o resultado da consulta a partir de uma release já parseada.
 *
 * Função pura: não faz rede nem Android. Prefere o APK cujo nome contém `Meu-Ponto`; se não houver,
 * usa o primeiro `.apk` com URL de download.
 */
internal fun evaluateRelease(payload: ReleasePayload, currentVersion: String): UpdateCheckResult {
    val remoteVersion = payload.tag.trim().trimStart('v', 'V')
    if (remoteVersion.isBlank()) {
        return UpdateCheckResult.Failed(UpdateFailure.INVALID_RESPONSE)
    }

    if (!AppUpdater.isNewerVersion(remoteVersion, currentVersion)) {
        return UpdateCheckResult.UpToDate
    }

    val apk = payload.assets
        .filter { asset ->
            asset.name.endsWith(".apk", ignoreCase = true) && asset.downloadUrl.isNotBlank()
        }
        .let { apks ->
            apks.firstOrNull { it.name.contains("Meu-Ponto", ignoreCase = true) } ?: apks.firstOrNull()
        }
        ?: return UpdateCheckResult.Failed(UpdateFailure.NO_APK)

    return UpdateCheckResult.Available(
        AppUpdateInfo(
            versionName = remoteVersion,
            releaseName = payload.name.ifBlank { "Versão $remoteVersion" },
            releaseNotes = payload.body.take(MAX_RELEASE_NOTES_CHARS),
            apkUrl = apk.downloadUrl,
            apkFileName = apk.name,
            apkSize = apk.size,
            releaseUrl = payload.releaseUrl
        )
    )
}

/**
 * Traduz um status HTTP de erro em motivo tipado.
 *
 * HTTP 404 não é falha de rede: significa que ainda não existe release e o chamador deve tratar como
 * [UpdateCheckResult.UpToDate] usando [isReleaseMissingStatus].
 */
internal fun failureForHttpStatus(status: Int): UpdateFailure = when (status) {
    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN, HTTP_TOO_MANY_REQUESTS -> UpdateFailure.RATE_LIMITED
    in HTTP_SERVER_ERROR_MIN..HTTP_SERVER_ERROR_MAX -> UpdateFailure.SERVER_ERROR
    else -> UpdateFailure.SERVER_ERROR
}

/** `true` quando o GitHub respondeu que não há release publicada (HTTP 404). */
internal fun isReleaseMissingStatus(status: Int): Boolean = status == HTTP_NOT_FOUND

/**
 * Resume as notas para o banner: primeira linha útil, sem marcação de Markdown básica.
 *
 * Linhas em branco e títulos vazios são ignorados. Texto acima de [maxChars] é cortado com reticências.
 */
internal fun summarizeReleaseNotes(notes: String, maxChars: Int = 120): String {
    if (maxChars <= 0) return ""

    val firstLine = notes.lineSequence()
        .map(::stripBasicMarkdown)
        .firstOrNull { it.isNotBlank() }
        ?: return ""

    if (firstLine.length <= maxChars) return firstLine
    if (maxChars == 1) return "…"
    return firstLine.take(maxChars - 1).trimEnd() + "…"
}

private fun stripBasicMarkdown(line: String): String {
    return line.trim()
        .trimStart('#', '-', '*', '>', ' ')
        .replace("*", "")
        .trim()
}
