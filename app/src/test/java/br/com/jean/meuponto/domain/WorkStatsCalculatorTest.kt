package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WorkStatsCalculatorTest {

    private fun receipt(
        date: String = "22/10/2026",
        entry: String? = null,
        exit: String? = null,
        workTime: String = "",
        needsReview: Boolean = false
    ) = Receipt(
        capturedAt = 0L,
        workDate = date,
        workTime = workTime,
        imagePath = "",
        recognizedText = "",
        entryTime = entry,
        exitTime = exit,
        needsReview = needsReview
    )

    @Test
    fun `calcula horas de entrada e saida no mesmo comprovante`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(entry = "08:00", exit = "17:00"))
        )

        assertEquals(540, days.getValue("22/10/2026").minutes)
        assertFalse(days.getValue("22/10/2026").pending)
    }

    @Test
    fun `calcula horas de comprovantes separados no mesmo dia`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(
                receipt(entry = "08:00"),
                receipt(exit = "17:00")
            )
        )

        assertEquals(540, days.getValue("22/10/2026").minutes)
        assertFalse(days.getValue("22/10/2026").pending)
    }

    @Test
    fun `marca batida unica como pendente`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(entry = "08:00"))
        )

        assertEquals(0, days.getValue("22/10/2026").minutes)
        assertTrue(days.getValue("22/10/2026").pending)
    }

    @Test
    fun `calcula jornada noturna no mesmo comprovante`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(entry = "22:00", exit = "06:00"))
        )

        assertEquals(480, days.getValue("22/10/2026").minutes)
    }

    @Test
    fun `mesma jornada conta igual em um comprovante ou em dois`() {
        val juntos = WorkStatsCalculator.dayStats(
            listOf(receipt(entry = "22:00", exit = "06:00"))
        )
        val separados = WorkStatsCalculator.dayStats(
            listOf(
                receipt(date = "22/10/2026", entry = "22:00"),
                receipt(date = "23/10/2026", exit = "06:00")
            )
        )

        assertEquals(480, juntos.getValue("22/10/2026").minutes)
        assertEquals(480, separados.getValue("22/10/2026").minutes)
        assertFalse(juntos.getValue("22/10/2026").pending)
        assertFalse(separados.getValue("22/10/2026").pending)
    }

    @Test
    fun `jornada acima de 16 horas no mesmo comprovante fica pendente`() {
        // 08:00 de um dia até 01:00 do dia seguinte = 17h, acima do teto de 16h.
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(entry = "08:00", exit = "01:00"))
        )

        assertEquals(0, days.getValue("22/10/2026").minutes)
        assertTrue(days.getValue("22/10/2026").pending)
        assertNull(WorkStatsCalculator.durationMinutes("08:00", "01:00"))
    }

    @Test
    fun `duracao aceita de 1 minuto ate 16 horas`() {
        assertEquals(1, WorkStatsCalculator.durationMinutes("08:00", "08:01"))
        assertEquals(60, WorkStatsCalculator.durationMinutes("08:00", "09:00"))
        assertEquals(960, WorkStatsCalculator.durationMinutes("00:00", "16:00"))
        assertEquals(480, WorkStatsCalculator.durationMinutes("22:00", "06:00"))
        assertNull(WorkStatsCalculator.durationMinutes("00:00", "16:01"))
        assertNull(WorkStatsCalculator.durationMinutes("08:00", "08:00"))
        assertNull(WorkStatsCalculator.durationMinutes(null, "08:00"))
    }

    @Test
    fun `pareia entrada e saida de turno que cruza a meia-noite`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(
                receipt(date = "21/10/2026", entry = "22:42"),
                receipt(date = "22/10/2026", exit = "06:57")
            )
        )

        assertEquals(495, days.getValue("21/10/2026").minutes)
        assertFalse(days.getValue("21/10/2026").pending)
        assertFalse(days.getValue("22/10/2026").pending)
    }

    @Test
    fun `pareia sequencia de turnos noturnos separados por comprovante`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(
                receipt(date = "21/10/2026", entry = "22:42"),
                receipt(date = "22/10/2026", exit = "06:57"),
                receipt(date = "22/10/2026", entry = "22:43"),
                receipt(date = "23/10/2026", exit = "07:00")
            )
        )

        assertEquals(495, days.getValue("21/10/2026").minutes)
        assertEquals(497, days.getValue("22/10/2026").minutes)
        assertEquals(0, days.getValue("23/10/2026").minutes)
        assertFalse(days.getValue("23/10/2026").pending)
    }

    @Test
    fun `saida sem entrada anterior fica pendente`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(date = "22/10/2026", exit = "06:57"))
        )

        assertEquals(0, days.getValue("22/10/2026").minutes)
        assertTrue(days.getValue("22/10/2026").pending)
    }

    @Test
    fun `entrada sem saida em ate 16 horas fica pendente`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(
                receipt(date = "21/10/2026", entry = "08:00"),
                receipt(date = "22/10/2026", exit = "09:00")
            )
        )

        assertEquals(0, days.getValue("21/10/2026").minutes)
        assertTrue(days.getValue("21/10/2026").pending)
        assertTrue(days.getValue("22/10/2026").pending)
    }

    @Test
    fun `horario sem tipo fica pendente`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(date = "22/10/2026", workTime = "22:43"))
        )

        assertEquals(0, days.getValue("22/10/2026").minutes)
        assertTrue(days.getValue("22/10/2026").pending)
    }

    @Test
    fun `calcula varios pares de batidas`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(
                receipt(entry = "08:00"),
                receipt(exit = "12:00"),
                receipt(entry = "13:00"),
                receipt(exit = "17:00")
            )
        )

        assertEquals(480, days.getValue("22/10/2026").minutes)
    }

    @Test
    fun `data vazia fica no grupo de revisao`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(date = "", entry = "08:00", needsReview = true))
        )

        assertTrue(days.getValue("revisar").pending)
    }

    @Test
    fun `resumo do periodo calcula total e dias restantes`() {
        val settings = PayrollSettings(
            periodStart = LocalDate.of(2026, 10, 1),
            periodEnd = LocalDate.of(2026, 10, 31)
        )
        val receipts = listOf(
            receipt(date = "22/10/2026", entry = "08:00", exit = "17:00"),
            receipt(date = "21/10/2026", entry = "09:00", exit = "17:00")
        )

        val stats = WorkStatsCalculator.periodStats(
            receipts = receipts,
            settings = settings,
            today = LocalDate.of(2026, 10, 22)
        )

        assertEquals(1020, stats.totalMinutes)
        assertEquals(2, stats.daysWithRecords)
        assertEquals(9, stats.daysRemaining)
        assertEquals(0, stats.incompleteDays)
    }

    @Test
    fun `resumo ignora comprovante fora do periodo`() {
        val settings = PayrollSettings(
            periodStart = LocalDate.of(2026, 10, 1),
            periodEnd = LocalDate.of(2026, 10, 31)
        )
        val receipts = listOf(
            receipt(date = "30/09/2026", entry = "08:00", exit = "17:00"),
            receipt(date = "22/10/2026", entry = "08:00", exit = "17:00")
        )

        val stats = WorkStatsCalculator.periodStats(
            receipts = receipts,
            settings = settings,
            today = LocalDate.of(2026, 10, 22)
        )

        assertEquals(540, stats.totalMinutes)
    }

    @Test
    fun `conta dias uteis sem registro e saldo esperado`() {
        val settings = PayrollSettings(
            periodStart = LocalDate.of(2026, 10, 19),
            periodEnd = LocalDate.of(2026, 10, 31),
            expectedDailyMinutes = 8 * 60
        )
        val receipts = listOf(
            receipt(date = "21/10/2026", entry = "08:00", exit = "17:00"),
            receipt(date = "22/10/2026", entry = "09:00", exit = "17:00")
        )

        val stats = WorkStatsCalculator.periodStats(
            receipts = receipts,
            settings = settings,
            today = LocalDate.of(2026, 10, 22)
        )

        assertEquals(2, stats.missingWorkdays)
        assertEquals(1020 - (8 * 60 * 4), stats.expectedBalanceMinutes)
    }

    @Test
    fun `normaliza data e hora do OCR`() {
        assertEquals("22/10/2026", WorkStatsCalculator.normalizeWorkDate("Data: 22-10-2026"))
        assertEquals("10:20", WorkStatsCalculator.normalizeClockTime("Hora 10:20"))
        assertNull(WorkStatsCalculator.normalizeWorkDate("sem data"))
        assertNull(WorkStatsCalculator.normalizeClockTime("sem hora"))
    }

    @Test
    fun `formata minutos`() {
        assertEquals("0h", formatMinutes(0))
        assertEquals("45min", formatMinutes(45))
        assertEquals("8h", formatMinutes(480))
        assertEquals("8h05", formatMinutes(485))
    }

    @Test
    fun `dia consistente nao tem motivo de revisao`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(entry = "08:00", exit = "17:00"))
        )

        assertEquals(emptyList<DayIssue>(), days.getValue("22/10/2026").issues)
    }

    @Test
    fun `saida sem entrada informa o motivo`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(date = "22/10/2026", exit = "06:57"))
        )

        assertEquals(listOf(DayIssue.MISSING_ENTRY), days.getValue("22/10/2026").issues)
    }

    @Test
    fun `entrada sem saida informa o motivo`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(date = "22/10/2026", entry = "22:43"))
        )

        assertEquals(listOf(DayIssue.MISSING_EXIT), days.getValue("22/10/2026").issues)
    }

    @Test
    fun `jornada acima de 16 horas informa o motivo`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(entry = "08:00", exit = "01:00"))
        )

        assertEquals(listOf(DayIssue.IMPLAUSIBLE_SHIFT), days.getValue("22/10/2026").issues)
    }

    @Test
    fun `horario sem tipo informa o motivo`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(workTime = "22:43"))
        )

        assertEquals(listOf(DayIssue.UNTYPED_PUNCH), days.getValue("22/10/2026").issues)
    }

    @Test
    fun `data desconhecida informa o motivo`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(date = "", entry = "08:00"))
        )

        assertEquals(listOf(DayIssue.UNKNOWN_DATE), days.getValue("revisar").issues)
    }

    @Test
    fun `comprovante marcado para revisao sem outro motivo`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(entry = "08:00", exit = "17:00", needsReview = true))
        )

        assertEquals(listOf(DayIssue.MARKED_FOR_REVIEW), days.getValue("22/10/2026").issues)
    }

    @Test
    fun `motivo especifico substitui a marcacao generica`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(receipt(entry = "08:00", needsReview = true))
        )

        assertEquals(listOf(DayIssue.MISSING_EXIT), days.getValue("22/10/2026").issues)
    }

    @Test
    fun `motivos ficam em ordem de prioridade`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(
                receipt(date = "22/10/2026", entry = "08:00", needsReview = true),
                receipt(date = "22/10/2026", workTime = "22:43")
            )
        )

        assertEquals(
            listOf(DayIssue.UNTYPED_PUNCH, DayIssue.MISSING_EXIT),
            days.getValue("22/10/2026").issues
        )
    }

    @Test
    fun `entrada e saida no mesmo minuto nao viram jornada`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(
                receipt(date = "22/10/2026", entry = "08:00"),
                receipt(date = "22/10/2026", exit = "08:00")
            )
        )

        assertEquals(0, days.getValue("22/10/2026").minutes)
        assertEquals(listOf(DayIssue.IMPLAUSIBLE_SHIFT), days.getValue("22/10/2026").issues)
    }

    @Test
    fun `turno acima de 16 horas pendente nos dois dias com motivos distintos`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(
                receipt(date = "21/10/2026", entry = "08:00"),
                receipt(date = "22/10/2026", exit = "09:00")
            )
        )

        assertEquals(listOf(DayIssue.MISSING_EXIT), days.getValue("21/10/2026").issues)
        assertEquals(listOf(DayIssue.MISSING_ENTRY), days.getValue("22/10/2026").issues)
    }
}
