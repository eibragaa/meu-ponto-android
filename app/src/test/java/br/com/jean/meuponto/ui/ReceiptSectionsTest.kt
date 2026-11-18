package br.com.jean.meuponto.ui

import br.com.jean.meuponto.data.Receipt
import br.com.jean.meuponto.domain.WorkStatsCalculator
import br.com.jean.meuponto.domain.journeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptSectionsTest {

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
    fun `saida do turno noturno fica no dia da saida sem jornada propria, com aviso do dia da entrada`() {
        val receipts = listOf(
            receipt(1, "25/10/2026", entry = "23:50"),
            receipt(2, "26/10/2026", exit = "06:57")
        )

        val sections = buildMonthSections(
            receipts,
            WorkStatsCalculator.dayStats(receipts),
            WorkStatsCalculator.journeys(receipts)
        )
        val days = sections.single().days.associateBy { it.key }

        val startDay = days.getValue("25/10/2026")
        assertEquals(listOf(1L, 2L), startDay.journeys.single().receipts.map { it.id })
        assertEquals(427, startDay.minutes)
        assertNull(startDay.shiftStartDay)

        val exitDay = days.getValue("26/10/2026")
        assertTrue(exitDay.journeys.isEmpty())
        assertEquals("25/10/2026", exitDay.shiftStartDay)
        assertEquals(0, exitDay.minutes)
        assertEquals(1, exitDay.receiptCount)
    }

    @Test
    fun `focar dia no modo mes expande o mes alvo e recolhe apenas os demais dias`() {
        val receipts = listOf(
            receipt(1, "20/09/2026", entry = "08:00", exit = "17:00"),
            receipt(2, "25/10/2026", entry = "08:00", exit = "17:00"),
            receipt(3, "26/10/2026", entry = "08:00", exit = "17:00"),
            receipt(4, "", entry = "08:00")
        )

        val focus = focusDayInMonthView("25/10/2026", receipts)

        assertEquals(setOf("2026-10"), focus.expandedMonths)
        assertEquals(setOf("20/09/2026", "26/10/2026", "revisar"), focus.collapsedDays)
    }

    @Test
    fun `focar dia de entrada de turno noturno mantem a jornada completa que cruza meia-noite`() {
        val receipts = listOf(
            receipt(1, "25/10/2026", entry = "23:50"),
            receipt(2, "26/10/2026", exit = "06:57"),
            receipt(3, "27/10/2026", entry = "08:00", exit = "17:00")
        )

        val focus = focusDayInMonthView("25/10/2026", receipts)
        val sections = buildMonthSections(
            receipts,
            WorkStatsCalculator.dayStats(receipts),
            WorkStatsCalculator.journeys(receipts)
        )
        val targetDay = sections.single { it.key in focus.expandedMonths }
            .days.single { it.key !in focus.collapsedDays }

        assertEquals("25/10/2026", targetDay.key)
        assertEquals(427, targetDay.minutes)
        assertEquals(listOf(1L, 2L), targetDay.journeys.single().receipts.map { it.id })
    }

    @Test
    fun `focar dia sem comprovantes expande o mes correspondente e recolhe dias existentes`() {
        val receipts = listOf(
            receipt(1, "25/10/2026", entry = "08:00", exit = "17:00")
        )

        val focus = focusDayInMonthView("03/11/2026", receipts)

        assertEquals(setOf("2026-11"), focus.expandedMonths)
        assertEquals(setOf("25/10/2026"), focus.collapsedDays)
    }
}
