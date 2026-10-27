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
}
