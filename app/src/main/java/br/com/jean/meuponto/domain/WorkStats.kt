package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import kotlin.math.abs

data class WorkDayStats(
    val date: String,
    val minutes: Int,
    val receiptCount: Int,
    val pending: Boolean
)

data class PeriodStats(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val totalMinutes: Int,
    val daysWithRecords: Int,
    val averageMinutes: Int,
    val pendingReceipts: Int,
    val incompleteDays: Int,
    val completeDays: Int,
    val missingWorkdays: Int,
    val daysRemaining: Int,
    val projectedMinutes: Int,
    val previousTotalMinutes: Int?,
    val expectedBalanceMinutes: Int?,
    val days: Map<String, WorkDayStats>
)

/**
 * Chave usada no lugar da data quando o OCR não identificou a data do comprovante.
 * Mantida como texto porque é a chave de agrupamento usada na UI e no banco.
 */
const val REVIEW_DATE_KEY = "revisar"

/**
 * Regras de jornada do app: pareamento de batidas, horas por dia e resumo do período.
 *
 * Regra central (ver [pairPunches]): entrada e saída podem vir em comprovantes diferentes, inclusive
 * em dias diferentes (turno noturno). O pareamento é cronológico e uma entrada só é pareada com a
 * primeira saída posterior dentro de [MAX_SHIFT_MINUTES].
 *
 * Datas e horários chegam como texto ("dd/MM/yyyy" e "HH:mm") do OCR ou do banco; a normalização e
 * a validação acontecem aqui, para que a UI só receba dados já validados.
 */
object WorkStatsCalculator {
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT)
    private val datePattern = Regex("\\b(\\d{2})[./-](\\d{2})[./-](\\d{4})\\b")
    private val timePattern = Regex("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b")

    fun normalizeWorkDate(value: String): String? {
        val match = datePattern.find(value) ?: return null
        val normalized = "${match.groupValues[1]}/${match.groupValues[2]}/${match.groupValues[3]}"
        return normalized.takeIf { parseWorkDate(it) != null }
    }

    fun normalizeClockTime(value: String): String? {
        val match = timePattern.find(value) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        return "%02d:%02d".format(hour, minute)
    }

    fun parseWorkDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value.trim(), dateFormatter) }.getOrNull()

    fun parseClockTime(value: String?): LocalTime? {
        val normalized = value?.let(::normalizeClockTime) ?: return null
        return runCatching { LocalTime.parse(normalized, timeFormatter) }.getOrNull()
    }

    /**
     * Duração em minutos entre dois horários do **mesmo** comprovante.
     *
     * Saída menor que entrada significa turno que cruzou a meia-noite (soma um dia). Aplica o mesmo
     * teto de [MAX_SHIFT_MINUTES] usado no pareamento entre comprovantes: acima disso o dado é
     * tratado como erro de digitação/OCR e a duração fica indefinida, em vez de inflar as horas.
     */
    fun durationMinutes(entry: String?, exit: String?): Int? {
        val entryTime = parseClockTime(entry) ?: return null
        val exitTime = parseClockTime(exit) ?: return null
        val minutes = Duration.between(entryTime, exitTime).toMinutes().toInt()
            .let { if (it < 0) it + MINUTES_PER_DAY else it }
        return minutes.takeIf { it in 1..MAX_SHIFT_MINUTES }
    }

    fun durationMinutes(receipt: Receipt): Int? =
        durationMinutes(receipt.entryTime, receipt.exitTime)

    /**
     * Horas por dia considerando todos os comprovantes recebidos.
     *
     * Duas origens de minutos, sem sobreposição:
     * 1. comprovante com entrada e saída no mesmo registro -> [durationMinutes];
     * 2. comprovante com apenas uma das batidas -> pareamento cronológico em [pairPunches], que
     *    credita a duração no dia da *entrada* (turno noturno conta no dia em que começou).
     *
     * O dia fica `pending` quando alguma batida não pôde ser pareada, quando o comprovante foi
     * marcado para revisão ([Receipt.needsReview]) ou quando a data é desconhecida
     * ([REVIEW_DATE_KEY]).
     */
    fun dayStats(receipts: List<Receipt>): Map<String, WorkDayStats> {
        val paired = pairPunches(receipts)
        return receipts.groupBy { it.workDate.ifBlank { REVIEW_DATE_KEY } }.mapValues { (date, dayReceipts) ->
            var minutes = paired.minutesByDate[date] ?: 0
            var pending = date in paired.pendingDates || dayReceipts.any { it.needsReview }
            dayReceipts.forEach { receipt ->
                if (parseClockTime(receipt.entryTime) != null && parseClockTime(receipt.exitTime) != null) {
                    val worked = durationMinutes(receipt.entryTime, receipt.exitTime)
                    if (worked != null) minutes += worked else pending = true
                }
            }
            WorkDayStats(
                date = date,
                minutes = minutes,
                receiptCount = dayReceipts.size,
                pending = pending
            )
        }
    }

    fun periodStats(
        receipts: List<Receipt>,
        settings: PayrollSettings,
        today: LocalDate = LocalDate.now()
    ): PeriodStats {
        val start = settings.effectiveStart(today)
        val end = settings.effectiveEnd(today)
        val validRange = !end.isBefore(start)
        val allDays = dayStats(receipts)
        val periodDays = if (validRange) {
            allDays.filterKeys { key ->
                parseWorkDate(key)?.let { !it.isBefore(start) && !it.isAfter(end) } == true
            }
        } else {
            emptyMap()
        }

        val totalMinutes = periodDays.values.sumOf { it.minutes }
        val daysWithRecords = periodDays.values.count { it.receiptCount > 0 }
        val incompleteDays = periodDays.values.count { it.pending }
        val completeDays = periodDays.values.count { !it.pending && it.minutes > 0 }
        val pendingReceipts = receipts.count { receipt ->
            receipt.needsReview && (
                receipt.workDate.isBlank() ||
                    parseWorkDate(receipt.workDate)?.let { !it.isBefore(start) && !it.isAfter(end) } == true
                )
        }
        val averageMinutes = if (daysWithRecords > 0) totalMinutes / daysWithRecords else 0

        val elapsedEnd = when {
            !validRange -> start.minusDays(1)
            today.isBefore(start) -> start.minusDays(1)
            else -> minOf(today, end)
        }
        val elapsedWorkdays = countWorkdays(start, elapsedEnd, settings.daysOff)
        val missingWorkdays = countMissingWorkdays(start, elapsedEnd, periodDays, settings.daysOff)
        val expectedBalance = settings.expectedDailyMinutes?.let { daily ->
            totalMinutes - (daily * elapsedWorkdays)
        }

        val daysRemaining = if (validRange && !today.isAfter(end)) {
            ChronoUnit.DAYS.between(today, end).toInt().coerceAtLeast(0)
        } else {
            0
        }
        val remainingWorkdays = if (validRange) {
            countWorkdays(maxOf(today.plusDays(1), start), end, settings.daysOff)
        } else {
            0
        }
        val projectedMinutes = totalMinutes + (averageMinutes * remainingWorkdays)

        val previousTotal = if (validRange) {
            val length = ChronoUnit.DAYS.between(start, end) + 1
            val previousStart = start.minusDays(length)
            val previousEnd = start.minusDays(1)
            val previousDays = allDays.filterKeys { key ->
                parseWorkDate(key)?.let { !it.isBefore(previousStart) && !it.isAfter(previousEnd) } == true
            }
            previousDays.values.takeIf { days -> days.any { it.receiptCount > 0 } }?.sumOf { it.minutes }
        } else {
            null
        }

        return PeriodStats(
            periodStart = start,
            periodEnd = end,
            totalMinutes = totalMinutes,
            daysWithRecords = daysWithRecords,
            averageMinutes = averageMinutes,
            pendingReceipts = pendingReceipts,
            incompleteDays = incompleteDays,
            completeDays = completeDays,
            missingWorkdays = missingWorkdays,
            daysRemaining = daysRemaining,
            projectedMinutes = projectedMinutes,
            previousTotalMinutes = previousTotal,
            expectedBalanceMinutes = expectedBalance,
            days = periodDays
        )
    }

    fun isWorkday(date: LocalDate, daysOff: Set<LocalDate>): Boolean {
        if (date in daysOff) return false
        return date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY
    }

    /**
     * Pareia batidas soltas (uma por comprovante) em ordem cronológica.
     *
     * Comprovantes que já trazem entrada e saída no mesmo registro são ignorados aqui: eles são
     * somados por [durationMinutes] em [dayStats], o que evita contar a mesma jornada duas vezes.
     */
    private fun pairPunches(receipts: List<Receipt>): PairResult {
        val minutesByDate = mutableMapOf<String, Int>()
        val pendingDates = mutableSetOf<String>()
        val dated = mutableListOf<Punch>()

        receipts.forEach { receipt ->
            val key = receipt.workDate.ifBlank { REVIEW_DATE_KEY }
            val entry = parseClockTime(receipt.entryTime)
            val exit = parseClockTime(receipt.exitTime)
            // Par completo no mesmo comprovante: contabilizado por durationMinutes, não aqui.
            if (entry != null && exit != null) return@forEach
            val date = parseWorkDate(receipt.workDate)
            val kinds = when {
                entry != null -> listOf(entry to PunchKind.ENTRY)
                exit != null -> listOf(exit to PunchKind.EXIT)
                else -> listOfNotNull(parseClockTime(receipt.workTime)?.let { it to PunchKind.UNKNOWN })
            }
            kinds.forEach { (time, kind) ->
                if (date == null || kind == PunchKind.UNKNOWN) {
                    pendingDates += key
                } else {
                    dated += Punch(key, date.atTime(time), kind)
                }
            }
        }

        val open = ArrayDeque<Punch>()
        dated.sortedWith(compareBy<Punch> { it.at }.thenBy { if (it.kind == PunchKind.ENTRY) 0 else 1 })
            .forEach { punch ->
                when (punch.kind) {
                    PunchKind.ENTRY -> open.addLast(punch)
                    PunchKind.EXIT -> {
                        // Entrada aberta há mais de 16h é considerada esquecida: não pareia e deixa
                        // o dia daquela entrada pendente, em vez de creditar uma jornada irreal.
                        while (open.isNotEmpty() &&
                            Duration.between(open.first().at, punch.at).toMinutes() > MAX_SHIFT_MINUTES
                        ) {
                            pendingDates += open.removeFirst().key
                        }
                        val entry = open.removeFirstOrNull()
                        val worked = entry?.let { Duration.between(it.at, punch.at).toMinutes().toInt() } ?: 0
                        if (entry != null && worked > 0) {
                            minutesByDate[entry.key] = (minutesByDate[entry.key] ?: 0) + worked
                        } else {
                            entry?.let { pendingDates += it.key }
                            pendingDates += punch.key
                        }
                    }
                    PunchKind.UNKNOWN -> pendingDates += punch.key
                }
            }
        open.forEach { pendingDates += it.key }

        return PairResult(minutesByDate, pendingDates)
    }

    private fun countWorkdays(start: LocalDate, end: LocalDate, daysOff: Set<LocalDate>): Int {
        if (end.isBefore(start)) return 0
        var count = 0
        var cursor = start
        while (!cursor.isAfter(end)) {
            if (isWorkday(cursor, daysOff)) count++
            cursor = cursor.plusDays(1)
        }
        return count
    }

    private fun countMissingWorkdays(
        start: LocalDate,
        end: LocalDate,
        days: Map<String, WorkDayStats>,
        daysOff: Set<LocalDate>
    ): Int {
        if (end.isBefore(start)) return 0
        val formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")
        var count = 0
        var cursor = start
        while (!cursor.isAfter(end)) {
            if (isWorkday(cursor, daysOff)) {
                val key = cursor.format(formatter)
                if ((days[key]?.receiptCount ?: 0) == 0) count++
            }
            cursor = cursor.plusDays(1)
        }
        return count
    }

    private data class Punch(val key: String, val at: LocalDateTime, val kind: PunchKind)

    private data class PairResult(val minutesByDate: Map<String, Int>, val pendingDates: Set<String>)

    private enum class PunchKind { ENTRY, EXIT, UNKNOWN }

    /** Teto de uma jornada plausível: acima disso o pareamento é recusado e o dia fica pendente. */
    private const val MAX_SHIFT_MINUTES = 16 * 60

    private const val MINUTES_PER_DAY = 24 * 60
}

fun formatMinutes(minutes: Int): String {
    if (minutes <= 0) return "0h"
    if (minutes < 60) return "${minutes}min"
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) "${hours}h" else "${hours}h${remainder.toString().padStart(2, '0')}"
}

fun formatSignedMinutes(minutes: Int): String {
    val prefix = if (minutes >= 0) "+" else "-"
    return prefix + formatMinutes(abs(minutes))
}
