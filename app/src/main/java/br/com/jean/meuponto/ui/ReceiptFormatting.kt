package br.com.jean.meuponto.ui


import br.com.jean.meuponto.domain.WorkStatsCalculator
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

/**
 * Formatação de datas e rótulos de período (mês, dia, semana).
 */

internal val PtBrLocale: Locale = Locale.forLanguageTag("pt-BR")

private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", PtBrLocale)
private val dayTitleFormatter = DateTimeFormatter.ofPattern("dd/MM · EEEE", PtBrLocale)

internal fun monthKey(workDate: String): String =
    WorkStatsCalculator.parseWorkDate(workDate)
        ?.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        ?: "revisar"

internal fun monthTitle(key: String): String {
    if (key == "revisar") return "Precisa de revisão"
    val date = runCatching { LocalDate.parse("$key-01") }.getOrNull() ?: return "Período"
    return date.format(monthTitleFormatter).replaceFirstChar { it.toString().uppercase(PtBrLocale) }
}

internal fun dayTitle(key: String): String {
    if (key == "revisar") return "Data pendente"
    val date = WorkStatsCalculator.parseWorkDate(key) ?: return "Data a revisar"
    return date.format(dayTitleFormatter).replaceFirstChar { it.toString().uppercase(PtBrLocale) }
}

internal fun currentMonthKey(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

internal fun weekLabel(workDate: String): String {
    val parsed = runCatching {
        SimpleDateFormat("dd/MM/yyyy", PtBrLocale).apply { isLenient = false }.parse(workDate)
    }.getOrNull() ?: return "Data a revisar"
    val start = Calendar.getInstance(PtBrLocale).apply {
        time = parsed
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }
    val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
    val formatter = SimpleDateFormat("dd/MM", PtBrLocale)
    return "${formatter.format(start.time)} – ${formatter.format(end.time)}"
}
