package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt

/**
 * Status de completude de batidas de um dia, para o indicador da lista.
 *
 * Reusa o pareamento de [PunchPairing] (mesmo passe que alimenta [WorkStatsCalculator.dayStats] e
 * [WorkStatsCalculator.journeys]), sem duplicar a regra de entrada/saída (regra 3 do padrão).
 */
enum class DayPunchStatus {
    /** Dia tem entrada e saída (ou ocorrência resolvendo o que faltava). */
    COMPLETE,

    /** Tem entrada mas falta saída. */
    MISSING_EXIT,

    /** Tem saída mas falta entrada. */
    MISSING_ENTRY,

    /** Sem comprovantes. */
    EMPTY
}

/**
 * Calcula o status de batidas de um dia a partir dos seus comprovantes.
 *
 * Batida sozinha (só entrada ou só saída) aparece como faltando o par correspondente: é o novo
 * indicador leve da lista, distinto do "Revisar" (vermelho), que só surge quando a batida fica sem
 * par dentro do teto de 16h (ver [WorkStatsCalculator.MAX_SHIFT_MINUTES]).
 */
fun dayPunchStatus(receipts: List<Receipt>): DayPunchStatus {
    if (receipts.isEmpty()) return DayPunchStatus.EMPTY
    val paired = PunchPairing.pair(receipts)
    val dayKeys = receipts.map { it.workDate.ifBlank { REVIEW_DATE_KEY } }.toSet()
    var missingExit = false
    var missingEntry = false
    for (shift in paired.shifts) {
        if (shift.key !in dayKeys) continue
        if (shift.occurrence != null) continue
        if (shift.entry != null && shift.exit == null) missingExit = true
        if (shift.exit != null && shift.entry == null) missingEntry = true
    }
    return when {
        missingExit -> DayPunchStatus.MISSING_EXIT
        missingEntry -> DayPunchStatus.MISSING_ENTRY
        else -> DayPunchStatus.COMPLETE
    }
}

/**
 * Rótulos curtos de batida salvos em um comprovante, para os chips do card.
 * "Falta saída"/"Falta entrada" só aparecem quando o comprovante tem uma batida sem o par.
 */
/**
 * Aplica uma escolha de batida a um comprovante não tipado (um horário solto).
 * Devolve os novos valores de entrada e saída. A data e a revisão ficam a cargo de quem chama.
 */
fun classifyUntyped(receipt: Receipt, choice: PunchChoice): Pair<String?, String?> {
    val time = receipt.entryTime ?: receipt.exitTime ?: receipt.workTime.takeIf { it.isNotBlank() }
    return when (choice) {
        PunchChoice.ENTRY -> time to null
        PunchChoice.EXIT -> null to time
        else -> receipt.entryTime to receipt.exitTime
    }
}

fun punchChips(receipt: Receipt): List<String> = buildList {
    when {
        !receipt.entryTime.isNullOrBlank() && !receipt.exitTime.isNullOrBlank() -> {
            add("Entrada")
            add("Saída")
        }
        !receipt.entryTime.isNullOrBlank() -> {
            add("Entrada")
            add("Falta saída")
        }
        !receipt.exitTime.isNullOrBlank() -> {
            add("Saída")
            add("Falta entrada")
        }
        receipt.workTime.isNotBlank() -> add("Horário (sem tipo)")
        else -> add("Sem horário")
    }
    if (receipt.occurrence != null) {
        // ocorrência resolve o dia: limpa os "falta ..." acima
        clear()
        add(receipt.occurrence.let(::occurrenceLabel))
    }
}
