package br.com.jean.meuponto.updater

/**
 * Dados de uma release mais nova que a instalada.
 *
 * [releaseUrl] é a página do release no GitHub e pode vir vazia quando a API não a informa.
 */
data class AppUpdateInfo(
    val versionName: String,
    val releaseName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkFileName: String,
    val apkSize: Long,
    val releaseUrl: String = ""
)

/** Resultado de uma consulta à API de releases do GitHub. */
sealed interface UpdateCheckResult {
    /** Não há versão mais nova; inclui o caso de release inexistente (HTTP 404). */
    data object UpToDate : UpdateCheckResult

    /** Há uma versão mais nova com APK disponível para instalação. */
    data class Available(val info: AppUpdateInfo) : UpdateCheckResult

    /** A consulta falhou; a UI decide entre silêncio (automático) e mensagem (manual). */
    data class Failed(val reason: UpdateFailure) : UpdateCheckResult
}

/** Motivo tipado de falha na consulta de atualização. */
enum class UpdateFailure {
    OFFLINE,
    RATE_LIMITED,
    SERVER_ERROR,
    INVALID_RESPONSE,
    NO_APK
}

/** Notas de uma versão guardadas para o "O que há de novo" depois da instalação. */
data class PendingNotes(
    val version: String,
    val notes: String
)
