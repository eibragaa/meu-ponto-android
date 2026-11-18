package br.com.jean.meuponto.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderSettingsTest {

    @Test
    fun `configuracao padrao inicia desativada com 30 minutos de antecedencia`() {
        val defaults = ReminderSettings()

        assertFalse(defaults.enabled)
        assertEquals(30, defaults.leadMinutes)
        assertTrue(DEFAULT_LEAD_MINUTES in REMINDER_LEAD_OPTIONS)
    }

    @Test
    fun `nextReminderAt agenda para hoje quando horario ainda nao passou`() {
        val now = LocalDateTime.of(2026, 10, 22, 7, 0)

        val trigger = nextReminderAt(now, entryTime = LocalTime.of(8, 0), leadMinutes = 30)

        assertEquals(LocalDateTime.of(2026, 10, 22, 7, 30), trigger)
    }

    @Test
    fun `nextReminderAt agenda para amanha quando horario ja passou ou e igual a agora`() {
        val exact = LocalDateTime.of(2026, 10, 22, 7, 30)
        val passed = LocalDateTime.of(2026, 10, 22, 8, 15)

        assertEquals(
            LocalDateTime.of(2026, 10, 23, 7, 30),
            nextReminderAt(exact, entryTime = LocalTime.of(8, 0), leadMinutes = 30)
        )
        assertEquals(
            LocalDateTime.of(2026, 10, 23, 7, 30),
            nextReminderAt(passed, entryTime = LocalTime.of(8, 0), leadMinutes = 30)
        )
    }

    @Test
    fun `nextReminderAt calcula lembrete na noite anterior para entrada de madrugada`() {
        val now = LocalDateTime.of(2026, 10, 22, 20, 0)

        val trigger = nextReminderAt(now, entryTime = LocalTime.of(0, 10), leadMinutes = 30)

        assertEquals(LocalDateTime.of(2026, 10, 22, 23, 40), trigger)
    }

    @Test
    fun `isReminderDay confirma dia habitual fora de folga`() {
        // 22/10/2026 é quinta-feira.
        val thursday = LocalDate.of(2026, 10, 22)
        val workdays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)

        assertTrue(isReminderDay(thursday, workdays, daysOff = emptySet()))
    }

    @Test
    fun `isReminderDay recusa dia fora do habito dia livre ou habito vazio`() {
        val thursday = LocalDate.of(2026, 10, 22)
        val friday = LocalDate.of(2026, 10, 23)
        val workdays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)

        assertFalse(isReminderDay(friday, workdays, daysOff = emptySet()))
        assertFalse(isReminderDay(thursday, workdays, daysOff = setOf(thursday)))
        assertFalse(isReminderDay(thursday, emptySet(), daysOff = emptySet()))
    }
}
