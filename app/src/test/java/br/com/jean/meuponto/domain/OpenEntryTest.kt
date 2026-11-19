package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class OpenEntryTest {

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
    fun `sem comprovantes nao ha entrada aberta`() {
        assertFalse(hasOpenEntry(emptyList(), LocalDateTime.of(2026, 10, 22, 12, 0)))
    }

    @Test
    fun `entrada recente sem saida indica entrada aberta`() {
        val receipts = listOf(receipt(date = "22/10/2026", entry = "08:00"))
        val now = LocalDateTime.of(2026, 10, 22, 12, 30)

        assertTrue(hasOpenEntry(receipts, now))
    }

    @Test
    fun `entrada fechada por saida no mesmo comprovante ou em outro nao fica aberta`() {
        val sameReceipt = listOf(receipt(date = "22/10/2026", entry = "08:00", exit = "17:00"))
        val nightShiftReceipts = listOf(
            receipt(date = "22/10/2026", entry = "22:00"),
            receipt(date = "23/10/2026", exit = "06:00")
        )
        val now = LocalDateTime.of(2026, 10, 23, 7, 0)

        assertFalse(hasOpenEntry(sameReceipt, now))
        assertFalse(hasOpenEntry(nightShiftReceipts, now))
    }

    @Test
    fun `entrada antiga alem da janela de 16 horas nao conta como aberta`() {
        val receipts = listOf(receipt(date = "22/10/2026", entry = "08:00"))
        val atLimit = LocalDateTime.of(2026, 10, 23, 0, 0)
        val beyondLimit = LocalDateTime.of(2026, 10, 23, 0, 1)

        assertTrue(hasOpenEntry(receipts, atLimit))
        assertFalse(hasOpenEntry(receipts, beyondLimit))
    }

    @Test
    fun `entrada com ocorrencia nao conta como aberta`() {
        val receipts = listOf(
            receipt(date = "22/10/2026", entry = "08:00", occurrence = DayOccurrence.FORGOT_PUNCH)
        )
        val now = LocalDateTime.of(2026, 10, 22, 12, 0)

        assertFalse(hasOpenEntry(receipts, now))
    }

    @Test
    fun `entrada no futuro nao conta como aberta`() {
        val receipts = listOf(receipt(date = "22/10/2026", entry = "14:00"))
        val now = LocalDateTime.of(2026, 10, 22, 13, 59)

        assertFalse(hasOpenEntry(receipts, now))
    }

    @Test
    fun `sugere entrada quando nao ha turno aberto`() {
        val closed = listOf(receipt(date = "22/10/2026", entry = "08:00", exit = "17:00"))
        val now = LocalDateTime.of(2026, 10, 22, 18, 0)

        assertEquals(PunchChoice.ENTRY, suggestedNextPunch(emptyList(), now))
        assertEquals(PunchChoice.ENTRY, suggestedNextPunch(closed, now))
    }

    @Test
    fun `sugere saida quando ha entrada aberta dentro da janela`() {
        val open = listOf(receipt(date = "22/10/2026", entry = "08:00"))
        val now = LocalDateTime.of(2026, 10, 22, 12, 30)

        assertEquals(PunchChoice.EXIT, suggestedNextPunch(open, now))
    }
}
