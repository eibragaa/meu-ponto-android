package br.com.jean.meuponto.updater

import org.json.JSONException
import org.json.JSONObject
import java.text.Normalizer

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
    val size: Long,
    val digest: String = ""
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
                size = asset.optLong("size", 0L),
                digest = asset.optString("digest", "")
                    .trim()
                    .takeUnless { it.equals("null", ignoreCase = true) }
                    .orEmpty()
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
            releaseUrl = payload.releaseUrl,
            apkDigest = apk.digest
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
 * Resumo pronto para o banner: destaca novidades e resume correções/melhorias.
 *
 * [text] é o conteúdo principal; [isFeature] indica se ele veio de uma seção de novidades e merece
 * o rótulo "Novidades" na UI.
 */
internal data class BannerSummary(val text: String, val isFeature: Boolean)

/** Linha candidata a resumo; [isBullet] prioriza itens de correção/novidade sobre texto solto. */
private data class SummaryCandidate(val text: String, val isBullet: Boolean)

private enum class ReleaseSection { NONE, FEATURES, FIXES, OTHER }

/** Nome/versão do release usado como título solto nas notas (ex.: `Meu Ponto v0.5.1`). */
private val RELEASE_NAME_LINE = Regex(
    """^(?:meu\s+ponto\s*[:\-–—]?\s*)?v?\d+(?:\.\d+)*(?:[-+][\w.-]+)?$""",
    RegexOption.IGNORE_CASE
)

/**
 * Cabeçalhos de seção que não dizem nada ao usuário no banner.
 *
 * A comparação é sem acento e sem diferenciar maiúsculas para aceitar "Correções" e "Correcoes".
 */
private val KNOWN_SECTION_HEADERS = setOf("correcoes", "novidades", "interno", "observacoes")
private val FEATURE_HEADER_KEYS = setOf("novidade", "feature", "destaque", "novo")
private val FIX_HEADER_KEYS = setOf("correcao", "correcoes", "melhoria", "ajuste")


private val BULLET_PREFIX = Regex("""^(?:[-*+•]|\d+[.)])\s+""")
private val DIACRITICS = Regex("\\p{Mn}+")

/**
 * Resume as notas para o banner: primeira linha útil, de preferência um item de lista.
 *
 * Ignora títulos Markdown (`#...`), o nome/versão do release e cabeçalhos de seção conhecidos;
 * remove marcação básica de lista/negrito. Texto acima de [maxChars] é cortado com reticências.
 */
internal fun summarizeReleaseNotes(notes: String, maxChars: Int = 120): String {
    if (maxChars <= 0) return ""

    val candidates = notes.lineSequence()
        .mapNotNull(::summaryCandidate)
        .toList()

    val summary = candidates.firstOrNull { it.isBullet }?.text
        ?: candidates.firstOrNull()?.text
        ?: return ""

    return truncateSummary(summary, maxChars)
}

/**
 * Extrai o resumo do banner priorizando a seção de novidades.
 *
 * Se houver uma seção "Novidades"/"Features"/"Destaques", usa até dois itens dela. Se houver apenas
 * correções/melhorias, devolve o texto genérico "Correções e melhorias" para não poluir o banner com
 * detalhes técnicos. Sem seções reconhecidas, cai no resumo simples da primeira linha útil.
 */
internal fun summarizeReleaseForBanner(notes: String, maxChars: Int = 140): BannerSummary {
    if (maxChars <= 0) return BannerSummary("", false)

    val features = mutableListOf<String>()
    var section = ReleaseSection.NONE
    var hasHeader = false
    var hasFixSection = false

    notes.lineSequence().forEach { rawLine ->
        val trimmed = rawLine.trim()
        if (trimmed.isBlank()) return@forEach

        val header = sectionHeaderText(trimmed)
        if (header != null) {
            hasHeader = true
            val normalized = stripDiacritics(header).lowercase()
            section = when {
                FEATURE_HEADER_KEYS.any { normalized.contains(it) } -> ReleaseSection.FEATURES
                FIX_HEADER_KEYS.any { normalized.contains(it) } -> {
                    hasFixSection = true
                    ReleaseSection.FIXES
                }
                normalized in KNOWN_SECTION_HEADERS -> ReleaseSection.OTHER
                else -> ReleaseSection.OTHER
            }
            return@forEach
        }

        if (section == ReleaseSection.FEATURES) {
            val text = stripBasicMarkdown(trimmed)
            if (text.isNotBlank() && !RELEASE_NAME_LINE.matches(text)) {
                features += text
            }
        }
    }

    if (features.isNotEmpty()) {
        return BannerSummary(
            text = truncateSummary(features.take(2).joinToString(" · "), maxChars),
            isFeature = true
        )
    }

    if (hasHeader && hasFixSection) {
        return BannerSummary("Correções e melhorias", false)
    }

    return BannerSummary(summarizeReleaseNotes(notes, maxChars), false)
}

private fun sectionHeaderText(rawLine: String): String? {
    if (rawLine.startsWith("#")) return stripBasicMarkdown(rawLine)
    val text = stripBasicMarkdown(rawLine)
    return text.takeIf { isKnownSectionHeader(it) }
}

private fun truncateSummary(text: String, maxChars: Int): String {
    if (text.length <= maxChars) return text
    if (maxChars == 1) return "…"
    return text.take(maxChars - 1).trimEnd() + "…"
}

private fun summaryCandidate(rawLine: String): SummaryCandidate? {
    val trimmed = rawLine.trim()
    if (trimmed.isBlank() || trimmed.startsWith("#")) return null

    val text = stripBasicMarkdown(trimmed)
    if (text.isBlank()) return null
    if (RELEASE_NAME_LINE.matches(text)) return null
    if (isKnownSectionHeader(text)) return null

    return SummaryCandidate(text = text, isBullet = BULLET_PREFIX.containsMatchIn(trimmed))
}

private fun isKnownSectionHeader(text: String): Boolean {
    val normalized = stripDiacritics(text.trim().trimEnd(':'))
        .lowercase()
    return normalized in KNOWN_SECTION_HEADERS
}

private fun stripDiacritics(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(DIACRITICS, "")

private fun stripBasicMarkdown(line: String): String {
    return line.trim()
        .trimStart('#', '-', '*', '>', ' ')
        .replace("*", "")
        .trim()
}
