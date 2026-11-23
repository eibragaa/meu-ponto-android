package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TodayShiftTest {

    private val today = LocalDate.of(2026, 11, 23)
    private val todayStr = "23/11/2026"

    private fun receipt(id: Long, date: String, entry: String? = null, exit: String? = null) = Receipt(
        id = id,
        capturedAt = id,
        workDate = date,
        workTime = "",
        imagePath = "",
        recognizedText = "",
        entryTime = entry,
        exitTime = exit
    )

    @Test
    fun `lista vazia retorna estado NO_RECORD`() {
        val now = LocalDateTime.of(today, LocalTime.of(10, 0))
        val status = calculateTodayShiftStatus(emptyList(), now)
        assertEquals(TodayShiftState.NO_RECORD, status.state)
        assertEquals(0, status.minutesWorked)
        assertNull(status.entryTime)
        assertNull(status.exitTime)
        assertNull(status.estimatedExitTime)
    }

    @Test
    fun `entrada aberta hoje retorna IN_PROGRESS com horario e previsao`() {
        val receipts = listOf(
            receipt(1, todayStr, entry = "07:00")
        )
        val now = LocalDateTime.of(today, LocalTime.of(10, 0)) // 3h trabalhadas
        val status = calculateTodayShiftStatus(receipts, now, expectedDailyMinutes = 480)

        assertEquals(TodayShiftState.IN_PROGRESS, status.state)
        assertEquals("07:00", status.entryTime)
        assertEquals(180, status.minutesWorked) // 3h = 180 min
        assertEquals("15:00", status.estimatedExitTime) // 07:00 + 8h = 15:00
        assertNull(status.exitTime)
    }

    @Test
    fun `jornada concluida hoje retorna COMPLETED com total trabalhado`() {
        val receipts = listOf(
            receipt(1, todayStr, entry = "07:00", exit = "16:00")
        )
        val now = LocalDateTime.of(today, LocalTime.of(18, 0))
        val status = calculateTodayShiftStatus(receipts, now)

        assertEquals(TodayShiftState.COMPLETED, status.state)
        assertEquals("07:00", status.entryTime)
        assertEquals("16:00", status.exitTime)
        assertEquals(540, status.minutesWorked) // 9 horas = 540 min
    }

    @Test
    fun `turno noturno aberto desde ontem e reconhecido como IN_PROGRESS`() {
        val yesterdayStr = "22/11/2026"
        val receipts = listOf(
            receipt(1, yesterdayStr, entry = "23:50")
        )
        val now = LocalDateTime.of(today, LocalTime.of(4, 0)) // 4h10 decorridos
        val status = calculateTodayShiftStatus(receipts, now, expectedDailyMinutes = 480)

        assertEquals(TodayShiftState.IN_PROGRESS, status.state)
        assertEquals("23:50", status.entryTime)
        assertEquals(250, status.minutesWorked) // 250 min
    }
}
