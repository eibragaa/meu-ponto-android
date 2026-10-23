package br.com.jean.meuponto.updater

/** Mensagem acionável para falhas da verificação manual de atualização. */
internal fun updateFailureMessage(failure: UpdateFailure): String = when (failure) {
    UpdateFailure.OFFLINE -> "Sem conexão. Verifique a internet e tente de novo."
    UpdateFailure.RATE_LIMITED -> "O GitHub limitou as consultas. Tente novamente em alguns minutos."
    UpdateFailure.SERVER_ERROR -> "O GitHub não respondeu agora. Tente de novo."
    UpdateFailure.INVALID_RESPONSE -> "Não foi possível ler a resposta do GitHub."
    UpdateFailure.NO_APK -> "A release existe, mas não tem APK para instalar."
}
