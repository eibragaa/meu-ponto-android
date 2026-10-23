package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import org.junit.Assert.assertEquals
import org.junit.Test

/** Textos mostrados no card, no cabeçalho do dia e no PDF para as novas ocorrências. */
class ReceiptLabelsTest {

    private fun receipt(
        date: String = "22/10/2026",
        entry: String? = null,
        exit: String? = null,
        workTime: String = "",
        occurrence: DayOccurrence? = null
    ) = Receipt(
        capturedAt = 0L,
        workDate = date,
        workTime = workTime,
        imagePath = "",
        recognizedText = "",
        entryTime = entry,
        exitTime = exit,
        occurrence = occurrence
    )

    @Test
    fun `cada ocorrencia tem um nome curto`() {
        assertEquals("Esquecimento", occurrenceLabel(DayOccurrence.FORGOT_PUNCH))
        assertEquals("Falta", occurrenceLabel(DayOccurrence.ABSENCE))
        assertEquals("Folga", occurrenceLabel(DayOccurrence.DAY_OFF))
        assertEquals("Atestado", occurrenceLabel(DayOccurrence.MEDICAL_LEAVE))
    }

    @Test
    fun `dia com ocorrencias junta os nomes em ordem`() {
        assertEquals(
            "Falta · Atestado",
            dayOccurrenceSummary(listOf(DayOccurrence.ABSENCE, DayOccurrence.MEDICAL_LEAVE))
        )
    }

    @Test
    fun `esquecimento mostra a batida que existe`() {
        assertEquals(
            "Entrada 08:00 · saída esquecida",
            receiptTimeLabel(receipt(entry = "08:00", occurrence = DayOccurrence.FORGOT_PUNCH))
        )
        assertEquals(
            "Saída 17:00 · entrada esquecida",
            receiptTimeLabel(receipt(exit = "17:00", occurrence = DayOccurrence.FORGOT_PUNCH))
        )
    }

    @Test
    fun `falta folga e atestado aparecem no lugar do horario`() {
        assertEquals("Folga", receiptTimeLabel(receipt(occurrence = DayOccurrence.DAY_OFF)))
    }

    @Test
    fun `comprovante normal mantem o texto de horario`() {
        assertEquals("Horário 08:00", receiptTimeLabel(receipt(workTime = "08:00")))
        assertEquals("Horário pendente", receiptTimeLabel(receipt()))
    }

    @Test
    fun `dica do card diz o que falta fazer`() {
        assertEquals("Revisar data", receiptReviewHint(receipt(date = "")))
        assertEquals(
            "Diga se é entrada ou saída",
            receiptReviewHint(receipt(workTime = "08:00"))
        )
        assertEquals(
            "Escolha o horário ou a ocorrência",
            receiptReviewHint(receipt())
        )
        assertEquals(
            "Revisar data/horário",
            receiptReviewHint(receipt(entry = "08:00"))
        )
    }
}
