package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import org.junit.Assert.assertEquals
import org.junit.Test

class PunchProgressTest {

    private fun receipt(
        date: String = "22/10/2026",
        entry: String? = null,
        exit: String? = null,
        occurrence: DayOccurrence? = null
    ) = Receipt(
        capturedAt = 0L,
        workDate = date,
        workTime = entry ?: exit ?: "",
        imagePath = "",
        recognizedText = "",
        entryTime = entry,
        exitTime = exit,
        occurrence = occurrence
    )

    @Test
    fun `dia vazio fica EMPTY`() {
        assertEquals(DayPunchStatus.EMPTY, dayPunchStatus(emptyList()))
    }

    @Test
    fun `entrada e saida no mesmo comprovante fica COMPLETE`() {
        assertEquals(DayPunchStatus.COMPLETE, dayPunchStatus(listOf(receipt(entry = "08:00", exit = "17:00"))))
    }

    @Test
    fun `so entrada fica MISSING_EXIT`() {
        assertEquals(DayPunchStatus.MISSING_EXIT, dayPunchStatus(listOf(receipt(entry = "08:00"))))
    }

    @Test
    fun `so saida fica MISSING_ENTRY`() {
        assertEquals(DayPunchStatus.MISSING_ENTRY, dayPunchStatus(listOf(receipt(exit = "17:00"))))
    }

    @Test
    fun `entrada e saida em comprovantes separados fica COMPLETE`() {
        assertEquals(
            DayPunchStatus.COMPLETE,
            dayPunchStatus(listOf(receipt(entry = "08:00"), receipt(exit = "17:00")))
        )
    }

    @Test
    fun `turno que cruza a meia-noite fica COMPLETE no dia da entrada`() {
        assertEquals(
            DayPunchStatus.COMPLETE,
            dayPunchStatus(listOf(receipt(date = "22/10/2026", entry = "22:00"), receipt(date = "23/10/2026", exit = "06:00")))
        )
    }

    @Test
    fun `esquecimento resolve o dia`() {
        assertEquals(
            DayPunchStatus.COMPLETE,
            dayPunchStatus(listOf(receipt(entry = "08:00", occurrence = DayOccurrence.FORGOT_PUNCH)))
        )
    }

    @Test
    fun `falta resolve o dia`() {
        assertEquals(
            DayPunchStatus.COMPLETE,
            dayPunchStatus(listOf(receipt(occurrence = DayOccurrence.ABSENCE)))
        )
    }

    @Test
    fun `punchChips mostra entrada e saida quando ambos presentes`() {
        assertEquals(listOf("Entrada", "Saída"), punchChips(receipt(entry = "08:00", exit = "17:00")))
    }

    @Test
    fun `punchChips mostra falta de saida quando so entrada`() {
        assertEquals(listOf("Entrada", "Falta saída"), punchChips(receipt(entry = "08:00")))
    }

    @Test
    fun `punchChips mostra falta de entrada quando so saida`() {
        assertEquals(listOf("Saída", "Falta entrada"), punchChips(receipt(exit = "17:00")))
    }

    @Test
    fun `punchChips mostra ocorrencia quando presente`() {
        assertEquals(listOf("Esquecimento"), punchChips(receipt(entry = "08:00", occurrence = DayOccurrence.FORGOT_PUNCH)))
    }
}
