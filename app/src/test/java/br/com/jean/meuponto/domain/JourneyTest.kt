package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Jornadas: cada turno vira um item, creditado no dia da entrada, sem perder comprovante. */
class JourneyTest {

    private fun receipt(
        id: Long,
        date: String = "25/10/2026",
        entry: String? = null,
        exit: String? = null,
        workTime: String = "",
        needsReview: Boolean = false,
        occurrence: DayOccurrence? = null
    ) = Receipt(
        id = id,
        capturedAt = id,
        workDate = date,
        workTime = workTime,
        imagePath = "",
        recognizedText = "",
        entryTime = entry,
        exitTime = exit,
        needsReview = needsReview,
        occurrence = occurrence
    )

    private fun journeysOf(vararg receipts: Receipt) = WorkStatsCalculator.journeys(receipts.toList())

    @Test
    fun `par completo no mesmo comprovante vira uma jornada com o mesmo id nas duas pontas`() {
        val journey = journeysOf(receipt(1, entry = "08:00", exit = "17:00")).getValue("25/10/2026").single()

        assertEquals("1", journey.id)
        assertEquals(1L, journey.entryReceiptId)
        assertEquals(1L, journey.exitReceiptId)
        assertEquals(listOf(1L), journey.receiptIds)
        assertEquals(540, journey.minutes)
        assertEquals("08:00", journey.entryTime)
        assertEquals("17:00", journey.exitTime)
        assertNull(journey.issue)
        assertFalse(journey.crossesMidnight)
    }

    @Test
    fun `madrugada 23h50 de 25 e 06h57 de 26 credita no dia da entrada com os dois comprovantes`() {
        val receipts = listOf(
            receipt(1, date = "25/10/2026", entry = "23:50"),
            receipt(2, date = "26/10/2026", exit = "06:57")
        )

        val journeys = WorkStatsCalculator.journeys(receipts)
        val journey = journeys.getValue("25/10/2026").single()

        assertEquals(setOf("25/10/2026"), journeys.keys)
        assertEquals("1:2", journey.id)
        assertEquals(listOf(1L, 2L), journey.receiptIds)
        assertEquals(1L, journey.entryReceiptId)
        assertEquals(2L, journey.exitReceiptId)
        assertEquals("23:50", journey.entryTime)
        assertEquals("06:57", journey.exitTime)
        assertEquals(427, journey.minutes)
        assertTrue(journey.crossesMidnight)
        assertNull(journey.issue)
    }

    @Test
    fun `madrugada aparece nas horas do dia da entrada e o dia da saida fica com zero`() {
        val receipts = listOf(
            receipt(1, date = "25/10/2026", entry = "23:50"),
            receipt(2, date = "26/10/2026", exit = "06:57")
        )

        val days = WorkStatsCalculator.dayStats(receipts)

        assertEquals(427, days.getValue("25/10/2026").minutes)
        assertFalse(days.getValue("25/10/2026").pending)
        assertEquals(0, days.getValue("26/10/2026").minutes)
        assertFalse(days.getValue("26/10/2026").pending)
    }

    @Test
    fun `entrada ainda sem saida vira jornada sem saida, sem minutos e sem pendencia`() {
        val journey = journeysOf(receipt(1, entry = "08:00")).getValue("25/10/2026").single()

        assertEquals("08:00", journey.entryTime)
        assertNull(journey.exitTime)
        assertNull(journey.exitReceiptId)
        assertEquals(0, journey.minutes)
        assertNull(journey.issue)
    }

    @Test
    fun `saida sem entrada vira jornada sem entrada`() {
        val journey = journeysOf(receipt(1, exit = "17:00")).getValue("25/10/2026").single()

        assertNull(journey.entryTime)
        assertNull(journey.entryReceiptId)
        assertEquals("17:00", journey.exitTime)
        assertEquals(0, journey.minutes)
        assertNull(journey.issue)
    }

    @Test
    fun `horario sem tipo vira jornada sem entrada e saida com pendencia de horario sem tipo`() {
        val journey = journeysOf(receipt(1, workTime = "18:30")).getValue("25/10/2026").single()

        assertNull(journey.entryTime)
        assertNull(journey.exitTime)
        assertEquals(listOf(1L), journey.receiptIds)
        assertEquals(DayIssue.UNTYPED_PUNCH, journey.issue)
    }

    @Test
    fun `comprovante com ocorrencia vira jornada com a ocorrencia e sem horas`() {
        val journey = journeysOf(receipt(1, occurrence = DayOccurrence.ABSENCE)).getValue("25/10/2026").single()

        assertEquals(DayOccurrence.ABSENCE, journey.occurrence)
        assertNull(journey.entryTime)
        assertNull(journey.exitTime)
        assertEquals(0, journey.minutes)
        assertNull(journey.issue)
    }

    @Test
    fun `jornada de mais de 16h no mesmo comprovante vira pendencia da jornada`() {
        val receipts = listOf(receipt(1, entry = "06:00", exit = "23:00"))

        val journey = WorkStatsCalculator.journeys(receipts).getValue("25/10/2026").single()

        assertEquals(0, journey.minutes)
        assertEquals(DayIssue.IMPLAUSIBLE_SHIFT, journey.issue)
        assertEquals(listOf(DayIssue.IMPLAUSIBLE_SHIFT), WorkStatsCalculator.dayStats(receipts).getValue("25/10/2026").issues)
    }

    @Test
    fun `entrada sem saida ha mais de 16h e saida depois dela deixam as duas batidas como pendencia`() {
        val receipts = listOf(
            receipt(1, date = "25/10/2026", entry = "08:00"),
            receipt(2, date = "26/10/2026", exit = "06:00")
        )

        val journeys = WorkStatsCalculator.journeys(receipts)

        assertEquals(DayIssue.MISSING_EXIT, journeys.getValue("25/10/2026").single().issue)
        assertEquals(DayIssue.MISSING_ENTRY, journeys.getValue("26/10/2026").single().issue)
    }

    @Test
    fun `comprovante sem data vai para o dia de revisao`() {
        val journeys = journeysOf(receipt(1, date = "", entry = "08:00", exit = "17:00"))

        assertEquals(listOf(1L), journeys.getValue(REVIEW_DATE_KEY).single().receiptIds)
    }

    @Test
    fun `cada comprovante aparece em exatamente uma jornada e as horas batem com dayStats`() {
        val receipts = listOf(
            receipt(1, date = "22/10/2026", entry = "08:00", exit = "12:00"),
            receipt(2, date = "22/10/2026", entry = "13:00"),
            receipt(3, date = "22/10/2026", exit = "18:00"),
            receipt(4, date = "22/10/2026", entry = "23:50"),
            receipt(5, date = "23/10/2026", exit = "06:57"),
            receipt(6, date = "24/10/2026", workTime = "09:00"),
            receipt(7, date = "23/10/2026", occurrence = DayOccurrence.DAY_OFF),
            receipt(8, date = "", entry = "08:00"),
            receipt(9, date = "24/10/2026", needsReview = true)
        )

        val journeys = WorkStatsCalculator.journeys(receipts)
        val all = journeys.values.flatten()
        val days = WorkStatsCalculator.dayStats(receipts)

        assertEquals(receipts.map { it.id }.sorted(), all.flatMap { it.receiptIds }.sorted())
        days.forEach { (day, stats) ->
            assertEquals("minutos de $day", stats.minutes, all.filter { it.day == day }.sumOf { it.minutes })
        }
    }
}
