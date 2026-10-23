package br.com.jean.meuponto.updater

/** Evento que pode disparar uma verificação de atualização. */
internal enum class CheckTrigger {
    ColdStart,
    Foreground,
    Manual
}

/**
 * Intervalo mínimo entre verificações automáticas no foreground.
 *
 * 30 min equilibra o aviso "ao abrir de novo" com o limite de 60 requisições/hora da API pública do
 * GitHub; a verificação manual ignora esse intervalo.
 */
internal const val AUTO_CHECK_INTERVAL_MS = 30 * 60 * 1000L

/**
 * Decide se a consulta ao GitHub deve acontecer.
 *
 * [CheckTrigger.ColdStart] e [CheckTrigger.Manual] sempre verificam. Relógio para trás
 * (`now < lastCheckAt`) é tratado como devido para não travar o aviso após ajuste de hora.
 */
internal fun shouldCheck(trigger: CheckTrigger, now: Long, lastCheckAt: Long): Boolean = when (trigger) {
    CheckTrigger.ColdStart, CheckTrigger.Manual -> true
    CheckTrigger.Foreground -> lastCheckAt <= 0L ||
        now < lastCheckAt ||
        now - lastCheckAt >= AUTO_CHECK_INTERVAL_MS
}

/** `true` quando o modal ainda não foi mostrado para [version]. */
internal fun shouldShowPrompt(version: String, lastPromptedVersion: String?): Boolean =
    version.isNotBlank() && version != lastPromptedVersion

/** `true` quando o banner daquela [version] não foi dispensado pelo usuário. */
internal fun shouldShowBanner(version: String, dismissedVersion: String?): Boolean =
    version.isNotBlank() && version != dismissedVersion
