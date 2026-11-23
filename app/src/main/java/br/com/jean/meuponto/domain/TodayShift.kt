package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class TodayShiftState {
    NO_RECORD,       // Nenhuma batida registrada hoje
    IN_PROGRESS,     // Em turno agora (entrada aberta)
    COMPLETED        // Jornada finalizada (todas as batidas fechadas)
}

data class TodayShiftStatus(
    val state: TodayShiftState,
    val entryTime: String? = null,
    val exitTime: String? = null,
    val minutesWorked: Int = 0,
    val estimatedExitTime: String? = null,
    val rawEntryTime: LocalTime? = null
)

fun calculateTodayShiftStatus(
    receipts: List<Receipt>,
    now: LocalDateTime = LocalDateTime.now(),
    expectedDailyMinutes: Int = 480
): TodayShiftStatus {
    if (receipts.isEmpty()) {
        return TodayShiftStatus(state = TodayShiftState.NO_RECORD)
    }

    val pairing = PunchPairing.pair(receipts)
    val todayDate = now.toLocalDate()
    val todayKey = todayDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    // Verifica se há um turno em aberto (entrada sem saída dentro de MAX_SHIFT_MINUTES)
    val openShift = pairing.shifts.firstOrNull { shift ->
        if (shift.occurrence != null || shift.entry == null || shift.exit != null) {
            false
        } else {
            val entryDate = WorkStatsCalculator.parseWorkDate(shift.entry.receipt.workDate)
            if (entryDate == null) false
            else {
                val entryAt = entryDate.atTime(shift.entry.time)
                val elapsed = Duration.between(entryAt, now).toMinutes()
                elapsed in 0L..WorkStatsCalculator.MAX_SHIFT_MINUTES.toLong()
            }
        }
    }

    val clockFmt = DateTimeFormatter.ofPattern("HH:mm")

    if (openShift != null) {
        val entryDate = WorkStatsCalculator.parseWorkDate(openShift.entry!!.receipt.workDate) ?: todayDate
        val entryAt = entryDate.atTime(openShift.entry.time)
        val elapsedMinutes = Duration.between(entryAt, now).toMinutes().coerceAtLeast(0).toInt()

        // Soma minutos de turnos já concluídos hoje (ex: turno da manhã)
        val completedMinutesToday = pairing.shifts
            .filter { it.key == todayKey && it != openShift && it.minutes > 0 }
            .sumOf { it.minutes }

        val remainingMinutesNeeded = (expectedDailyMinutes - completedMinutesToday).coerceAtLeast(0)
        val estimatedExit = openShift.entry.time.plusMinutes(remainingMinutesNeeded.toLong())

        return TodayShiftStatus(
            state = TodayShiftState.IN_PROGRESS,
            entryTime = openShift.entry.time.format(clockFmt),
            minutesWorked = completedMinutesToday + elapsedMinutes,
            estimatedExitTime = estimatedExit.format(clockFmt),
            rawEntryTime = openShift.entry.time
        )
    }

    // Se não há turno aberto, verifica se há turnos concluídos hoje
    val todayCompletedShifts = pairing.shifts.filter { it.key == todayKey && it.occurrence == null }
    if (todayCompletedShifts.isNotEmpty()) {
        val firstEntry = todayCompletedShifts.mapNotNull { it.entry?.time }.minOrNull()
        val lastExit = todayCompletedShifts.mapNotNull { it.exit?.time }.maxOrNull()
        val totalMinutes = todayCompletedShifts.sumOf { it.minutes }

        return TodayShiftStatus(
            state = TodayShiftState.COMPLETED,
            entryTime = firstEntry?.format(clockFmt),
            exitTime = lastExit?.format(clockFmt),
            minutesWorked = totalMinutes,
            rawEntryTime = firstEntry
        )
    }

    return TodayShiftStatus(state = TodayShiftState.NO_RECORD)
}
