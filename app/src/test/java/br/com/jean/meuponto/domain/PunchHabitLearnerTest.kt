package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class PunchHabitLearnerTest {

    private fun receipt(
        date: String = "05/10/2026",
        entry: String? = null,
        exit: String? = null,
        workTime: String = "",
        occurrence: DayOccurrence? = null
    ) = Receipt(
        capturedAt = 0L,
        workDate = date,
        workTime = workTime,
        imagePath = "",
        recognizedText = "",
        entryTime = entry,
        exitTime = exit,
        occurrence = occurrence
    )

    @Test
    fun `lista vazia nao aprende horario nem dias de trabalho`() {
        val habit = PunchHabitLearner.learn(emptyList())

        assertNull(habit.entryTime)
        assertTrue(habit.workdays.isEmpty())
    }

    @Test
    fun `aprende horario tipico de entrada pela moda`() {
        val receipts = listOf(
            receipt(date = "05/10/2026", entry = "08:05"),
            receipt(date = "06/10/2026", entry = "08:10"),
            receipt(date = "07/10/2026", entry = "09:00")
        )

        val habit = PunchHabitLearner.learn(receipts)

        assertEquals(LocalTime.of(8, 0), habit.entryTime)
    }

    @Test
    fun `empate de frequencia escolhe o horario mais cedo`() {
        val receipts = listOf(
            receipt(date = "05/10/2026", entry = "09:00"),
            receipt(date = "06/10/2026", entry = "08:00")
        )

        val habit = PunchHabitLearner.learn(receipts)

        assertEquals(LocalTime.of(8, 0), habit.entryTime)
    }

    @Test
    fun `arredonda para meia hora mais proxima subindo no empate e virando meia-noite`() {
        assertEquals(LocalTime.of(8, 0), PunchHabitLearner.roundToNearestHalfHour(LocalTime.of(8, 14)))
        assertEquals(LocalTime.of(8, 30), PunchHabitLearner.roundToNearestHalfHour(LocalTime.of(8, 15)))
        assertEquals(LocalTime.of(8, 30), PunchHabitLearner.roundToNearestHalfHour(LocalTime.of(8, 44)))
        assertEquals(LocalTime.of(9, 0), PunchHabitLearner.roundToNearestHalfHour(LocalTime.of(8, 45)))
        assertEquals(LocalTime.of(0, 0), PunchHabitLearner.roundToNearestHalfHour(LocalTime.of(23, 50)))
    }

    @Test
    fun `entrada ilegivel ou sem tipo e ignorada no aprendizado de horario`() {
        val receipts = listOf(
            receipt(date = "05/10/2026", entry = "invalida"),
            receipt(date = "06/10/2026", workTime = "08:00"),
            receipt(date = "07/10/2026", exit = "17:00")
        )

        val habit = PunchHabitLearner.learn(receipts)

        assertNull(habit.entryTime)
    }

    @Test
    fun `dia da semana exige pelo menos duas datas distintas`() {
        // Dois comprovantes na mesma segunda-feira contam apenas como uma data.
        val sameDayReceipts = listOf(
            receipt(date = "05/10/2026", entry = "08:00"),
            receipt(date = "05/10/2026", exit = "17:00")
        )
        assertTrue(PunchHabitLearner.learn(sameDayReceipts).workdays.isEmpty())

        // Duas segundas-feiras distintas (05/10 e 12/10 de 2026) entram no hábito.
        val twoMondays = listOf(
            receipt(date = "05/10/2026", entry = "08:00"),
            receipt(date = "12/10/2026", entry = "08:00")
        )
        assertEquals(setOf(DayOfWeek.MONDAY), PunchHabitLearner.learn(twoMondays).workdays)
    }

    @Test
    fun `folga falta e atestado nao contam como dia trabalhado nem horario`() {
        val receipts = listOf(
            receipt(date = "05/10/2026", entry = "08:00", occurrence = DayOccurrence.ABSENCE),
            receipt(date = "12/10/2026", entry = "08:00", occurrence = DayOccurrence.DAY_OFF),
            receipt(date = "19/10/2026", entry = "08:00", occurrence = DayOccurrence.MEDICAL_LEAVE)
        )

        val habit = PunchHabitLearner.learn(receipts)

        assertNull(habit.entryTime)
        assertTrue(habit.workdays.isEmpty())
    }

    @Test
    fun `esquecimento de batida conta como dia trabalhado`() {
        val receipts = listOf(
            receipt(date = "05/10/2026", entry = "08:00", occurrence = DayOccurrence.FORGOT_PUNCH),
            receipt(date = "12/10/2026", entry = "08:05", occurrence = DayOccurrence.FORGOT_PUNCH)
        )

        val habit = PunchHabitLearner.learn(receipts)

        assertEquals(LocalTime.of(8, 0), habit.entryTime)
        assertEquals(setOf(DayOfWeek.MONDAY), habit.workdays)
    }

    @Test
    fun `dia isolado nao entra no habito`() {
        // 05/10 e 12/10 são segundas; 06/10 é uma terça isolada.
        val receipts = listOf(
            receipt(date = "05/10/2026", entry = "08:00"),
            receipt(date = "12/10/2026", entry = "08:00"),
            receipt(date = "06/10/2026", entry = "08:00")
        )

        val habit = PunchHabitLearner.learn(receipts)

        assertEquals(setOf(DayOfWeek.MONDAY), habit.workdays)
    }

    @Test
    fun `fim de semana recorrente entra no habito`() {
        // 10/10/2026 e 17/10/2026 são sábados.
        val receipts = listOf(
            receipt(date = "10/10/2026", entry = "09:00"),
            receipt(date = "17/10/2026", entry = "09:00")
        )

        val habit = PunchHabitLearner.learn(receipts)

        assertEquals(setOf(DayOfWeek.SATURDAY), habit.workdays)
    }

    @Test
    fun `data invalida e ignorada sem erro`() {
        val receipts = listOf(
            receipt(date = "data-errada", entry = "08:00"),
            receipt(date = "", entry = "08:00")
        )

        val habit = PunchHabitLearner.learn(receipts)

        assertEquals(LocalTime.of(8, 0), habit.entryTime)
        assertTrue(habit.workdays.isEmpty())
    }
}
