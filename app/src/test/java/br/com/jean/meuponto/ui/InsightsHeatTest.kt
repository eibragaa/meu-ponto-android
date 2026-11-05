package br.com.jean.meuponto.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Nível de preenchimento da grade do mês ([dayHeatLevel]): regra pura, sem Compose. */
class InsightsHeatTest {

    @Test
    fun `mesmo mes retorna uma unica grade`() {
        assertEquals(
            listOf(YearMonth.of(2026, 10)),
            monthsInPeriod(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        )
    }

    @Test
    fun `periodo que cruza outubro e novembro retorna os dois meses`() {
        assertEquals(
            listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 11)),
            monthsInPeriod(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30))
        )
    }

    @Test
    fun `periodo que cruza dezembro e janeiro preserva a ordem do ano`() {
        assertEquals(
            listOf(YearMonth.of(2026, 12), YearMonth.of(2027, 1)),
            monthsInPeriod(LocalDate.of(2026, 12, 15), LocalDate.of(2027, 1, 14))
        )
    }

    @Test
    fun `periodo invertido nao cria uma grade`() {
        assertEquals(
            emptyList<YearMonth>(),
            monthsInPeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 31))
        )
    }

    @Test
    fun zeroMinutesIsNoneWithOrWithoutTarget() {
        assertEquals(HeatLevel.NONE, dayHeatLevel(0, 480))
        assertEquals(HeatLevel.NONE, dayHeatLevel(0, null))
    }

    @Test
    fun belowHalfTargetIsLow() {
        assertEquals(HeatLevel.LOW, dayHeatLevel(100, 480))
        assertEquals(HeatLevel.LOW, dayHeatLevel(239, 480))
    }

    @Test
    fun fromHalfTargetUpToTargetIsMedium() {
        assertEquals(HeatLevel.MEDIUM, dayHeatLevel(240, 480))
        assertEquals(HeatLevel.MEDIUM, dayHeatLevel(300, 480))
        assertEquals(HeatLevel.MEDIUM, dayHeatLevel(479, 480))
    }

    @Test
    fun exactlyTargetIsFull() {
        assertEquals(HeatLevel.FULL, dayHeatLevel(480, 480))
    }

    @Test
    fun aboveTargetIsFull() {
        assertEquals(HeatLevel.FULL, dayHeatLevel(600, 480))
    }

    @Test
    fun nullTargetWithWorkedMinutesIsLowNotFull() {
        assertEquals(HeatLevel.LOW, dayHeatLevel(480, null))
        assertEquals(HeatLevel.LOW, dayHeatLevel(1, null))
    }

    @Test
    fun nonPositiveTargetIsTreatedAsNoTarget() {
        assertEquals(HeatLevel.LOW, dayHeatLevel(480, 0))
        assertEquals(HeatLevel.NONE, dayHeatLevel(0, 0))
    }
}
