package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
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

    fun durationMinutes(entry: String?, exit: String?): Int? {
        val entryTime = parseClockTime(entry) ?: return null
        val exitTime = parseClockTime(exit) ?: return null
        var minutes = Duration.between(entryTime, exitTime).toMinutes().toInt()
        if (minutes < 0) minutes += 24 * 60
        return minutes.takeIf { it > 0 }
    }

    fun durationMinutes(receipt: Receipt): Int? =
        durationMinutes(receipt.entryTime, receipt.exitTime)

    fun dayStats(receipts: List<Receipt>): Map<String, WorkDayStats> =
        receipts.groupBy { it.workDate.ifBlank { "revisar" } }.mapValues { (date, dayReceipts) ->
            calculateDay(date, dayReceipts)
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

    private fun calculateDay(date: String, receipts: List<Receipt>): WorkDayStats {
        val punches = mutableListOf<Punch>()
        var pending = receipts.any { it.needsReview }
        var minutes = 0

        receipts.forEach { receipt ->
            val entry = parseClockTime(receipt.entryTime)
            val exit = parseClockTime(receipt.exitTime)
            if (entry != null && exit != null) {
                val worked = durationMinutes(receipt.entryTime, receipt.exitTime)
                if (worked != null) {
                    minutes += worked
                } else {
                    pending = true
                }
            } else {
                entry?.let { punches += Punch(it, PunchKind.ENTRY) }
                exit?.let { punches += Punch(it, PunchKind.EXIT) }
                if (entry == null && exit == null) {
                    parseClockTime(receipt.workTime)?.let { punches += Punch(it, PunchKind.UNKNOWN) }
                }
            }
        }

        val openEntries = ArrayDeque<LocalTime>()
        punches.sortedBy { it.time }.forEach { punch ->
            when (punch.kind) {
                PunchKind.ENTRY -> openEntries.addLast(punch.time)
                PunchKind.EXIT -> {
                    val entry = openEntries.removeFirstOrNull()
                    if (entry == null) {
                        pending = true
                    } else {
                        var worked = Duration.between(entry, punch.time).toMinutes().toInt()
                        if (worked < 0) worked += 24 * 60
                        if (worked > 0) minutes += worked else pending = true
                    }
                }
                PunchKind.UNKNOWN -> pending = true
            }
        }
        if (openEntries.isNotEmpty()) pending = true

        return WorkDayStats(
            date = date,
            minutes = minutes,
            receiptCount = receipts.size,
            pending = pending
        )
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

    private data class Punch(val time: LocalTime, val kind: PunchKind)

    private enum class PunchKind { ENTRY, EXIT, UNKNOWN }
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
