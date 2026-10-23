package br.com.jean.meuponto.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** Formatação de minutos das telas de resumo e do PDF. */
class FormattingTest {

    @Test
    fun `formata minutos em horas e minutos`() {
        assertEquals("0h", formatMinutes(0))
        assertEquals("1min", formatMinutes(1))
        assertEquals("59min", formatMinutes(59))
        assertEquals("1h", formatMinutes(60))
        assertEquals("1h01", formatMinutes(61))
        assertEquals("1h30", formatMinutes(90))
        assertEquals("8h", formatMinutes(480))
        assertEquals("24h", formatMinutes(1440))
    }

    @Test
    fun `minutos negativos nao viram hora negativa`() {
        assertEquals("0h", formatMinutes(-30))
    }

    @Test
    fun `formata saldo com sinal`() {
        assertEquals("+0h", formatSignedMinutes(0))
        assertEquals("+1h01", formatSignedMinutes(61))
        assertEquals("+8h", formatSignedMinutes(480))
        assertEquals("-1min", formatSignedMinutes(-1))
        assertEquals("-1h30", formatSignedMinutes(-90))
        assertEquals("-8h", formatSignedMinutes(-480))
    }
}
