package br.com.jean.meuponto.ui


import br.com.jean.meuponto.domain.REVIEW_DATE_KEY
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

/** Chave de agrupamento do mês (`yyyy-MM`) ou [REVIEW_DATE_KEY] quando a data não é válida. */
internal fun monthKey(workDate: String): String =
    WorkStatsCalculator.parseWorkDate(workDate)
        ?.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        ?: REVIEW_DATE_KEY

/** Título legível do mês a partir da chave `yyyy-MM` ("Outubro 2026"). */
internal fun monthTitle(key: String): String {
    if (key == REVIEW_DATE_KEY) return "Precisa de revisão"
    val date = runCatching { LocalDate.parse("$key-01") }.getOrNull() ?: return "Período"
    return date.format(monthTitleFormatter).replaceFirstChar { it.toString().uppercase(PtBrLocale) }
}

/** Título do dia a partir de `dd/MM/yyyy` ("22/10 · Quinta-feira"). */
internal fun dayTitle(key: String): String {
    if (key == REVIEW_DATE_KEY) return "Data pendente"
    val date = WorkStatsCalculator.parseWorkDate(key) ?: return "Data a revisar"
    return date.format(dayTitleFormatter).replaceFirstChar { it.toString().uppercase(PtBrLocale) }
}

/** Chave do mês corrente, usada para abrir a lista com o mês atual expandido. */
internal fun currentMonthKey(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

/**
 * Rótulo da semana (segunda a domingo) que contém a data, no formato "20/10 – 26/10".
 * `Calendar` aqui é legado consciente: `firstDayOfWeek = MONDAY`/`minimalDaysInFirstWeek = 4`
 * (ISO-8601) resolve o início da semana sem código manual.
 */
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
