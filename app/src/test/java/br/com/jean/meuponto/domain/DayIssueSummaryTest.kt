package br.com.jean.meuponto.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** Rótulos do motivo de revisão exibidos no cabeçalho do dia. */
class DayIssueSummaryTest {

    @Test
    fun `cada motivo tem um rotulo curto`() {
        assertEquals("data não reconhecida", dayIssueLabel(DayIssue.UNKNOWN_DATE))
        assertEquals("jornada acima de 16h", dayIssueLabel(DayIssue.IMPLAUSIBLE_SHIFT))
        assertEquals("horário sem entrada/saída", dayIssueLabel(DayIssue.UNTYPED_PUNCH))
        assertEquals("entrada sem saída", dayIssueLabel(DayIssue.MISSING_EXIT))
        assertEquals("saída sem entrada", dayIssueLabel(DayIssue.MISSING_ENTRY))
        assertEquals("comprovante marcado para revisão", dayIssueLabel(DayIssue.MARKED_FOR_REVIEW))
    }

    @Test
    fun `junta os motivos na ordem recebida`() {
        assertEquals("saída sem entrada", dayIssueSummary(listOf(DayIssue.MISSING_ENTRY)))
        assertEquals(
            "entrada sem saída · horário sem entrada/saída",
            dayIssueSummary(listOf(DayIssue.MISSING_EXIT, DayIssue.UNTYPED_PUNCH))
        )
    }

    @Test
    fun `nao repete o mesmo motivo`() {
        assertEquals(
            "saída sem entrada",
            dayIssueSummary(listOf(DayIssue.MISSING_ENTRY, DayIssue.MISSING_ENTRY))
        )
    }

    @Test
    fun `dia sem motivo nao produz texto`() {
        assertEquals("", dayIssueSummary(emptyList()))
    }
}
