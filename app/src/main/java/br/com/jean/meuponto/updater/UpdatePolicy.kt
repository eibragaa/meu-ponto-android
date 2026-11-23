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
 * 24 horas (uma vez ao dia) evita consultas repetidas ao GitHub enquanto o app está aberto ou em
 * segundo plano; a verificação manual ou ao entrar no app (ColdStart) segue ativa.
 */
internal const val AUTO_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

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

/** `true` quando o banner daquela [version] não foi dispensado pelo usuário. */
internal fun shouldShowBanner(version: String, dismissedVersion: String?): Boolean =
    version.isNotBlank() && version != dismissedVersion
