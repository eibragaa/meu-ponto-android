package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import java.time.format.DateTimeFormatter
import java.util.IdentityHashMap

/**
 * Jornada: um turno mostrado como um único item da lista. Pode ser um par de batidas (no mesmo
 * comprovante ou em dois), uma batida sem par, uma batida sem tipo ou uma ocorrência.
 *
 * Invariante: cada comprovante aparece em exatamente uma jornada ([receiptIds]), e a soma de [minutes]
 * das jornadas de um dia é igual aos minutos de [WorkStatsCalculator.dayStats] para aquele dia.
 */
data class Journey(
    /** Chave estável para a lista: ids dos comprovantes separados por ":", na ordem entrada -> saída. */
    val id: String,
    /** Dia creditado: o dia da entrada (turno de madrugada conta onde começou). [REVIEW_DATE_KEY] sem data. */
    val day: String,
    /** Horário de entrada "HH:mm", ou null quando não há entrada. */
    val entryTime: String?,
    /** Horário de saída "HH:mm", ou null quando não há saída. */
    val exitTime: String?,
    val entryReceiptId: Long?,
    /** Igual a [entryReceiptId] quando entrada e saída estão no mesmo comprovante. */
    val exitReceiptId: Long?,
    /** Comprovantes de origem, na ordem entrada -> saída. Um comprovante sem horário também aparece aqui. */
    val receiptIds: List<Long>,
    val minutes: Int,
    /** Pendência desta jornada: a mais prioritária, pela ordem de [DayIssue]. Null quando não há. */
    val issue: DayIssue?,
    /** Ocorrência (falta, folga, atestado, esquecimento) quando o comprovante a registra. */
    val occurrence: DayOccurrence?,
    /** `true` quando a saída cai em outro dia que a entrada (turno de madrugada). */
    val crossesMidnight: Boolean
)

private val clockFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Agrupa os comprovantes em jornadas, pelo dia creditado. Usa o mesmo passe de [dayStats]
 * ([PunchPairing]), então horas e jornadas não podem divergir.
 *
 * A chave do mapa é o dia da entrada; comprovante sem data cai em [REVIEW_DATE_KEY]. Dentro de cada
 * dia, as jornadas seguem a ordem em que os comprovantes chegaram na lista.
 */
fun WorkStatsCalculator.journeys(receipts: List<Receipt>): Map<String, List<Journey>> {
    val position = IdentityHashMap<Receipt, Int>().apply {
        receipts.forEachIndexed { index, receipt -> put(receipt, index) }
    }
    return PunchPairing.pair(receipts).shifts
        .sortedBy { shift -> shift.receipts.minOf { position[it] ?: Int.MAX_VALUE } }
        .map(::toJourney)
        .groupBy { it.day }
}

private fun toJourney(shift: PairedShift): Journey {
    val entryReceipt = shift.entry?.receipt
    val exitReceipt = shift.exit?.receipt
    return Journey(
        id = shift.receipts.joinToString(":") { it.id.toString() },
        day = shift.key,
        entryTime = shift.entry?.time?.format(clockFormatter),
        exitTime = shift.exit?.time?.format(clockFormatter),
        entryReceiptId = entryReceipt?.id,
        exitReceiptId = exitReceipt?.id,
        receiptIds = shift.receipts.map { it.id },
        minutes = shift.minutes,
        issue = shift.issues.minOrNull(),
        occurrence = shift.occurrence,
        // Entrada e saída no mesmo comprovante têm a mesma data, então não cruzam a meia-noite.
        crossesMidnight = entryReceipt != null && exitReceipt != null &&
            entryReceipt.workDate != exitReceipt.workDate
    )
}
