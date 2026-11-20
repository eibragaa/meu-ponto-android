package br.com.jean.meuponto.ui

import br.com.jean.meuponto.domain.DayIssue
import br.com.jean.meuponto.domain.DayOccurrence
import br.com.jean.meuponto.domain.WorkDayStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Nível de preenchimento da grade do mês ([dayHeatLevel]): regra pura, sem Compose. */
class InsightsHeatTest {

    @Test
    fun `dia completo mostra estado de saida`() {
        val day = WorkDayStats("05/11/2026", 480, 2, emptyList(), exitTime = "17:00")

        assertEquals(DayVisualState.COMPLETE, dayVisualState(day, inPeriod = true))
    }

    @Test
    fun `dia com pendencia mostra estado incompleto`() {
        val day = WorkDayStats("05/11/2026", 0, 1, listOf(DayIssue.MISSING_EXIT))

        assertEquals(DayVisualState.INCOMPLETE, dayVisualState(day, inPeriod = true))
    }

    @Test
    fun `so saida ocorrencia e sem registro nao mostram relogio`() {
        val exitOnly = WorkDayStats("05/11/2026", 0, 1, emptyList())
        val occurrence = WorkDayStats(
            "05/11/2026",
            0,
            1,
            emptyList(),
            occurrences = listOf(DayOccurrence.ABSENCE)
        )

        assertEquals(DayVisualState.NONE, dayVisualState(exitOnly, inPeriod = true))
        assertEquals(DayVisualState.NONE, dayVisualState(occurrence, inPeriod = true))
        assertEquals(DayVisualState.NONE, dayVisualState(null, inPeriod = true))
    }

    @Test
    fun `movimento reduzido inicia diretamente no estado final`() {
        assertEquals(1f, initialDayAnimationProgress(reduceMotion = true))
        assertEquals(0f, initialDayAnimationProgress(reduceMotion = false))
    }

    @Test
    fun `mes inicial prefere hoje dentro do periodo e fim fora`() {
        val start = LocalDate.of(2026, 10, 5)
        val end = LocalDate.of(2026, 11, 5)

        assertEquals(YearMonth.of(2026, 10), initialCompactMonth(start, end, LocalDate.of(2026, 10, 20)))
        assertEquals(YearMonth.of(2026, 11), initialCompactMonth(start, end, LocalDate.of(2026, 12, 1)))
    }

    @Test
    fun `navegacao compacta fica limitada aos meses do periodo`() {
        val start = LocalDate.of(2026, 10, 5)
        val end = LocalDate.of(2026, 11, 5)
        val october = YearMonth.of(2026, 10)
        val november = YearMonth.of(2026, 11)

        assertEquals(october, stepCompactMonth(october, start, end, -1))
        assertEquals(november, stepCompactMonth(october, start, end, 1))
        assertEquals(november, stepCompactMonth(november, start, end, 1))
    }

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

    @Test
    fun `relogio gira apenas em dias incompletos sem movimento reduzido`() {
        assertTrue(shouldAnimateClock(DayVisualState.INCOMPLETE, reduceMotion = false))
        assertFalse(shouldAnimateClock(DayVisualState.INCOMPLETE, reduceMotion = true))
        assertFalse(shouldAnimateClock(DayVisualState.COMPLETE, reduceMotion = false))
        assertFalse(shouldAnimateClock(DayVisualState.NONE, reduceMotion = false))
    }

    @Test
    fun `meses disponiveis incluem meses retroativos com comprovantes ate o periodo atual`() {
        val start = LocalDate.of(2026, 11, 1)
        val end = LocalDate.of(2026, 11, 30)
        val recorded = listOf("15/09/2026", "02/10/2026", "data-invalida", "")

        assertEquals(
            listOf(
                YearMonth.of(2026, 9),
                YearMonth.of(2026, 10),
                YearMonth.of(2026, 11)
            ),
            availableCalendarMonths(start, end, recorded)
        )
    }

    @Test
    fun `meses disponiveis cruzam virada de ano e mantem so periodo sem historico anterior`() {
        val start = LocalDate.of(2026, 2, 1)
        val end = LocalDate.of(2026, 2, 28)

        assertEquals(
            listOf(
                YearMonth.of(2025, 12),
                YearMonth.of(2026, 1),
                YearMonth.of(2026, 2)
            ),
            availableCalendarMonths(start, end, listOf("20/12/2025"))
        )
        assertEquals(
            listOf(YearMonth.of(2026, 2)),
            availableCalendarMonths(start, end, emptyList())
        )
        assertEquals(
            emptyList<YearMonth>(),
            availableCalendarMonths(end, start, listOf("15/01/2026"))
        )
    }

    @Test
    fun `navegacao por meses retroativos avanca recua e respeita limites`() {
        val months = listOf(
            YearMonth.of(2026, 9),
            YearMonth.of(2026, 10),
            YearMonth.of(2026, 11)
        )

        assertEquals(YearMonth.of(2026, 10), stepCalendarMonth(YearMonth.of(2026, 11), months, -1))
        assertEquals(YearMonth.of(2026, 9), stepCalendarMonth(YearMonth.of(2026, 10), months, -1))
        assertEquals(YearMonth.of(2026, 9), stepCalendarMonth(YearMonth.of(2026, 9), months, -1))
        assertEquals(YearMonth.of(2026, 11), stepCalendarMonth(YearMonth.of(2026, 10), months, 1))
        assertEquals(YearMonth.of(2026, 11), stepCalendarMonth(YearMonth.of(2026, 11), months, 1))
    }

    @Test
    fun `identifica corretamente se o mes pertence ao periodo atual`() {
        val start = LocalDate.of(2026, 10, 21)
        val end = LocalDate.of(2026, 11, 20)

        assertFalse(isMonthInPeriod(YearMonth.of(2026, 9), start, end))
        assertTrue(isMonthInPeriod(YearMonth.of(2026, 10), start, end))
        assertTrue(isMonthInPeriod(YearMonth.of(2026, 11), start, end))
        assertFalse(isMonthInPeriod(YearMonth.of(2026, 12), start, end))
        assertFalse(isMonthInPeriod(YearMonth.of(2026, 11), end, start))
    }
}
