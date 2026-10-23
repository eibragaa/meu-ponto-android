package br.com.jean.meuponto.ui

import br.com.jean.meuponto.data.Receipt
import br.com.jean.meuponto.domain.WorkDayStats

/**
 * Agrupamento da lista de comprovantes em meses, dias e grupos de filtro.
 */

internal data class FilterOption(val key: String, val label: String)

internal val filters = listOf(
    FilterOption("all", "Todos"),
    FilterOption("today", "Hoje"),
    FilterOption("time", "Com horário")
)

internal val groupModes = listOf(
    FilterOption("month", "Mês"),
    FilterOption("day", "Dia"),
    FilterOption("week", "Semana"),
    FilterOption("none", "Lista")
)

internal val punchModes = listOf(
    FilterOption("both", "Entrada e saída"),
    FilterOption("entry", "Só entrada"),
    FilterOption("exit", "Só saída"),
    FilterOption("none", "Sem horário")
)

internal data class ReceiptGroup(val title: String, val receipts: List<Receipt>)

internal data class DaySection(
    val key: String,
    val title: String,
    val receipts: List<Receipt>,
    val minutes: Int,
    val receiptCount: Int,
    val pending: Boolean
)

internal data class MonthSection(
    val key: String,
    val title: String,
    val days: List<DaySection>,
    val receiptCount: Int,
    val minutes: Int
)

internal fun buildMonthSections(
    receipts: List<Receipt>,
    dayStats: Map<String, WorkDayStats>
): List<MonthSection> {
    return receipts
        .groupBy { monthKey(it.workDate) }
        .map { (key, items) ->
            val days = items
                .groupBy { it.workDate.ifBlank { "revisar" } }
                .map { (dayKey, dayReceipts) ->
                    val dayStat = dayStats[dayKey]
                    DaySection(
                        key = dayKey,
                        title = dayTitle(dayKey),
                        receipts = dayReceipts,
                        minutes = dayStat?.minutes ?: 0,
                        receiptCount = dayStat?.receiptCount ?: dayReceipts.size,
                        pending = dayStat?.pending ?: dayReceipts.any { it.needsReview }
                    )
                }
                .sortedWith(
                    compareBy<DaySection> { if (it.key == "revisar") 0 else 1 }
                        .thenByDescending { it.key }
                )
            MonthSection(
                key = key,
                title = monthTitle(key),
                days = days,
                receiptCount = items.size,
                minutes = days.sumOf { it.minutes }
            )
        }
        .sortedWith(
            compareBy<MonthSection> { if (it.key == "revisar") 0 else 1 }
                .thenByDescending { it.key }
        )
}

internal fun groupReceipts(receipts: List<Receipt>, mode: String): List<ReceiptGroup> {
    if (mode == "none") return listOf(ReceiptGroup("", receipts))
    return receipts.groupBy { receipt ->
        when (mode) {
            "day" -> receipt.workDate.ifBlank { "Data pendente" }
            "week" -> weekLabel(receipt.workDate)
            else -> ""
        }
    }.map { (title, items) -> ReceiptGroup(title, items) }
}
