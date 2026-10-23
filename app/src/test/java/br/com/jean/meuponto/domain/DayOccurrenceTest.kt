package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ocorrências (esquecimento, falta, folga, atestado) resolvem o dia sem entrar no pareamento. */
class DayOccurrenceTest {

    private fun receipt(
        date: String = "22/10/2026",
        entry: String? = null,
        exit: String? = null,
        needsReview: Boolean = false,
        occurrence: DayOccurrence? = null
    ) = Receipt(
        capturedAt = 0L,
        workDate = date,
        workTime = entry ?: exit ?: "",
        imagePath = "",
        recognizedText = "",
        entryTime = entry,
        exitTime = exit,
        needsReview = needsReview,
        occurrence = occurrence
    )

    @Test
    fun `esquecimento de saida nao pareia com saida de outro dia`() {
        val days = WorkStatsCalculator.dayStats(
            listOf(
                receipt(date = "22/10/2026", entry = "22:00", occurrence = DayOccurrence.FORGOT_PUNCH),
                receipt(date = "23/10/2026", exit = "06:00")
            )
        )

        assertEquals(0, days.getValue("22/10/2026").minutes)
        assertFalse(days.getValue("22/10/2026").pending)
        // A saída de 23/10 não tem entrada aberta para parear: fica pendente no próprio dia.
        assertEquals(listOf(DayIssue.MISSING_ENTRY), days.getValue("23/10/2026").issues)
    }

    @Test
    fun `esquecimento deixa o dia sem pendencia e sem horas`() {
        val day = WorkStatsCalculator.dayStats(
            listOf(receipt(entry = "08:00", occurrence = DayOccurrence.FORGOT_PUNCH))
        ).getValue("22/10/2026")

        assertFalse(day.pending)
        assertEquals(0, day.minutes)
        assertEquals(listOf(DayOccurrence.FORGOT_PUNCH), day.occurrences)
    }

    @Test
    fun `falta folga e atestado resolvem o dia sem horas`() {
        listOf(DayOccurrence.ABSENCE, DayOccurrence.DAY_OFF, DayOccurrence.MEDICAL_LEAVE).forEach { kind ->
            val day = WorkStatsCalculator.dayStats(listOf(receipt(occurrence = kind))).getValue("22/10/2026")

            assertFalse("$kind não pode deixar o dia pendente", day.pending)
            assertEquals(0, day.minutes)
            assertEquals(listOf(kind), day.occurrences)
        }
    }

    @Test
    fun `comprovante com ocorrencia nao fica marcado para revisao`() {
        val day = WorkStatsCalculator.dayStats(
            listOf(receipt(needsReview = true, occurrence = DayOccurrence.ABSENCE))
        ).getValue("22/10/2026")

        assertFalse(day.pending)
        assertTrue(day.issues.isEmpty())
    }

    @Test
    fun `ocorrencia em um comprovante nao altera as horas de outro do mesmo dia`() {
        val day = WorkStatsCalculator.dayStats(
            listOf(
                receipt(entry = "08:00", exit = "12:00"),
                receipt(entry = "13:00", occurrence = DayOccurrence.FORGOT_PUNCH)
            )
        ).getValue("22/10/2026")

        assertEquals(240, day.minutes)
        assertFalse(day.pending)
    }

    @Test
    fun `ocorrencia sem data continua pendente pela data`() {
        val day = WorkStatsCalculator.dayStats(
            listOf(receipt(date = "", occurrence = DayOccurrence.ABSENCE))
        ).getValue(REVIEW_DATE_KEY)

        assertEquals(listOf(DayIssue.UNKNOWN_DATE), day.issues)
    }

    @Test
    fun `filtro revisar traz os comprovantes de dias pendentes e os marcados`() {
        val entryOnly = receipt(date = "22/10/2026", entry = "08:00")
        val absentSameDay = receipt(date = "22/10/2026", occurrence = DayOccurrence.ABSENCE)
        val complete = receipt(date = "21/10/2026", entry = "08:00", exit = "17:00")
        val marked = receipt(date = "20/10/2026", exit = "17:00", needsReview = true)

        val result = WorkStatsCalculator.receiptsNeedingReview(
            listOf(entryOnly, absentSameDay, complete, marked)
        )

        assertEquals(listOf(entryOnly, marked), result)
    }

    @Test
    fun `filtro revisar nao traz dia resolvido por ocorrencia`() {
        val result = WorkStatsCalculator.receiptsNeedingReview(
            listOf(receipt(entry = "08:00", occurrence = DayOccurrence.FORGOT_PUNCH))
        )

        assertTrue(result.isEmpty())
    }
}
