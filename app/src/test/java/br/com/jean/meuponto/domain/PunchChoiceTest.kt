package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Escolha da folha de edição e leitura dos horários do OCR, sem presumir a ordem. */
class PunchChoiceTest {

    private fun receipt(
        entry: String? = null,
        exit: String? = null,
        workTime: String = "",
        occurrence: DayOccurrence? = null
    ) = Receipt(
        capturedAt = 0L,
        workDate = "22/10/2026",
        workTime = workTime,
        imagePath = "",
        recognizedText = "",
        entryTime = entry,
        exitTime = exit,
        occurrence = occurrence
    )

    @Test
    fun `comprovante com entrada e saída começa como entrada e saída`() {
        assertEquals(PunchChoice.BOTH, punchChoiceOf(receipt(entry = "08:00", exit = "17:00")))
    }

    @Test
    fun `comprovante com uma batida começa como só entrada ou só saída`() {
        assertEquals(PunchChoice.ENTRY, punchChoiceOf(receipt(entry = "08:00")))
        assertEquals(PunchChoice.EXIT, punchChoiceOf(receipt(exit = "17:00")))
    }

    @Test
    fun `horario sem tipo nao escolhe nada sozinho`() {
        assertNull(punchChoiceOf(receipt(workTime = "08:00")))
    }

    @Test
    fun `falta folga e atestado começam como sem horário`() {
        assertEquals(PunchChoice.NONE, punchChoiceOf(receipt(occurrence = DayOccurrence.ABSENCE)))
        assertEquals(PunchChoice.NONE, punchChoiceOf(receipt(occurrence = DayOccurrence.DAY_OFF)))
        assertEquals(PunchChoice.NONE, punchChoiceOf(receipt(occurrence = DayOccurrence.MEDICAL_LEAVE)))
    }

    @Test
    fun `comprovante sem horario e sem ocorrencia precisa de escolha`() {
        assertNull(punchChoiceOf(receipt()))
    }

    @Test
    fun `esquecimento só combina com entrada ou saída sozinha`() {
        assertEquals(setOf(DayOccurrence.FORGOT_PUNCH), PunchChoice.ENTRY.occurrences)
        assertEquals(setOf(DayOccurrence.FORGOT_PUNCH), PunchChoice.EXIT.occurrences)
        assertEquals(emptySet<DayOccurrence>(), PunchChoice.BOTH.occurrences)
    }

    @Test
    fun `sem horario so combina com falta folga ou atestado`() {
        assertEquals(
            setOf(DayOccurrence.ABSENCE, DayOccurrence.DAY_OFF, DayOccurrence.MEDICAL_LEAVE),
            PunchChoice.NONE.occurrences
        )
    }

    @Test
    fun `ocr sem horario nao cria batida`() {
        val punches = WorkStatsCalculator.ocrPunches(emptyList())

        assertEquals("", punches.workTime)
        assertNull(punches.entry)
        assertNull(punches.exit)
    }

    @Test
    fun `ocr com um horario deixa a batida sem tipo`() {
        val punches = WorkStatsCalculator.ocrPunches(listOf("08:00"))

        assertEquals("08:00", punches.workTime)
        assertNull(punches.entry)
        assertNull(punches.exit)
    }

    @Test
    fun `ocr com dois horarios usa a ordem de leitura e ignora o excedente`() {
        val punches = WorkStatsCalculator.ocrPunches(listOf("08:00", "17:00", "18:30"))

        assertEquals("08:00", punches.workTime)
        assertEquals("08:00", punches.entry)
        assertEquals("17:00", punches.exit)
    }
}
