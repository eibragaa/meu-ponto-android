package br.com.jean.meuponto.domain


import br.com.jean.meuponto.data.Receipt

/**
 * Rótulos de exibição derivados de um comprovante (usados por UI e exportação de PDF).
 */

internal fun receiptTimeLabel(receipt: Receipt): String {
    val occurrence = receipt.occurrence ?: return punchLabel(receipt)
    return if (occurrence == DayOccurrence.FORGOT_PUNCH) forgotPunchLabel(receipt) else occurrenceLabel(occurrence)
}

private fun punchLabel(receipt: Receipt): String = when {
    !receipt.entryTime.isNullOrBlank() && !receipt.exitTime.isNullOrBlank() ->
        "Entrada ${receipt.entryTime} · Saída ${receipt.exitTime}"
    !receipt.entryTime.isNullOrBlank() -> "Entrada ${receipt.entryTime} · saída pendente"
    !receipt.exitTime.isNullOrBlank() -> "Saída ${receipt.exitTime} · entrada pendente"
    receipt.workTime.isNotBlank() -> "Horário ${receipt.workTime}"
    else -> "Horário pendente"
}

private fun forgotPunchLabel(receipt: Receipt): String = when {
    !receipt.entryTime.isNullOrBlank() -> "Entrada ${receipt.entryTime} · saída esquecida"
    !receipt.exitTime.isNullOrBlank() -> "Saída ${receipt.exitTime} · entrada esquecida"
    else -> occurrenceLabel(DayOccurrence.FORGOT_PUNCH)
}

/** Nome de exibição de uma ocorrência (chips da folha de edição e cabeçalho do dia). */
internal fun occurrenceLabel(occurrence: DayOccurrence): String = when (occurrence) {
    DayOccurrence.FORGOT_PUNCH -> "Esquecimento"
    DayOccurrence.ABSENCE -> "Falta"
    DayOccurrence.DAY_OFF -> "Folga"
    DayOccurrence.MEDICAL_LEAVE -> "Atestado"
}

/** Ocorrências de um dia em uma linha, na ordem do enum. Lista vazia devolve texto vazio. */
internal fun dayOccurrenceSummary(occurrences: List<DayOccurrence>): String =
    occurrences.joinToString(" · ", transform = ::occurrenceLabel)

/**
 * Ação curta exibida no card de um comprovante marcado para revisão, dizendo o que falta fazer.
 */
internal fun receiptReviewHint(receipt: Receipt): String = when {
    receipt.workDate.isBlank() -> "Revisar data"
    receipt.entryTime == null && receipt.exitTime == null && receipt.workTime.isNotBlank() ->
        "Diga se é entrada ou saída"
    receipt.entryTime == null && receipt.exitTime == null -> "Escolha o horário ou a ocorrência"
    else -> "Revisar data/horário"
}

/** Texto curto do motivo de revisão de um dia, usado no cabeçalho da lista. */
internal fun dayIssueLabel(issue: DayIssue): String = when (issue) {
    DayIssue.UNKNOWN_DATE -> "data não reconhecida"
    DayIssue.IMPLAUSIBLE_SHIFT -> "jornada acima de 16h"
    DayIssue.UNTYPED_PUNCH -> "horário sem entrada/saída"
    DayIssue.MISSING_EXIT -> "entrada sem saída"
    DayIssue.MISSING_ENTRY -> "saída sem entrada"
    DayIssue.MARKED_FOR_REVIEW -> "comprovante marcado para revisão"
}

/**
 * Junta os motivos de um dia em uma linha, na ordem de prioridade de [DayIssue].
 *
 * Lista vazia devolve texto vazio: quem chama só exibe quando o dia está pendente.
 */
internal fun dayIssueSummary(issues: List<DayIssue>): String =
    issues.distinct().joinToString(" · ", transform = ::dayIssueLabel)
