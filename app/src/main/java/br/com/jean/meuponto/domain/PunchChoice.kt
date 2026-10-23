package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt

/**
 * Forma como um comprovante registra o ponto. É a escolha feita na folha de edição.
 *
 * [occurrences] são as ocorrências que podem acompanhar cada escolha:
 * - entrada ou saída sozinha aceita [DayOccurrence.FORGOT_PUNCH] (a outra batida foi esquecida);
 * - sem horário exige uma ausência: falta, folga ou atestado.
 */
enum class PunchChoice(val label: String, val occurrences: Set<DayOccurrence>) {
    BOTH("Entrada e saída", emptySet()),
    ENTRY("Só entrada", setOf(DayOccurrence.FORGOT_PUNCH)),
    EXIT("Só saída", setOf(DayOccurrence.FORGOT_PUNCH)),
    NONE(
        "Sem horário",
        setOf(DayOccurrence.ABSENCE, DayOccurrence.DAY_OFF, DayOccurrence.MEDICAL_LEAVE)
    )
}

/**
 * Escolha que representa o que já está gravado no comprovante.
 *
 * Devolve `null` quando ainda falta decidir, por exemplo um horário lido pelo OCR sem tipo, ou um
 * comprovante sem horário e sem ocorrência. A folha começa sem escolha nesses casos.
 */
fun punchChoiceOf(receipt: Receipt): PunchChoice? = when {
    receipt.entryTime != null && receipt.exitTime != null -> PunchChoice.BOTH
    receipt.entryTime != null -> PunchChoice.ENTRY
    receipt.exitTime != null -> PunchChoice.EXIT
    receipt.occurrence?.isDayAbsence == true -> PunchChoice.NONE
    else -> null
}
