package br.com.jean.meuponto.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Máscaras dos campos de data/hora do editor.
 *
 * Estes testes existem por causa do P18 (dígitos fora de ordem ao digitar) e do P19 (gravar horário
 * do tipo não escolhido): a máscara é a parte da UI onde o bug aparecia primeiro.
 */
class MaskedInputTest {

    @Test
    fun `mascara de hora monta HHmm a partir dos digitos`() {
        assertEquals("", formatTimeInput(""))
        assertEquals("7", formatTimeInput("7"))
        assertEquals("07", formatTimeInput("07"))
        assertEquals("07:0", formatTimeInput("070"))
        assertEquals("07:00", formatTimeInput("0700"))
        assertEquals("22:43", formatTimeInput("2243"))
    }

    @Test
    fun `mascara de hora nao duplica os dois pontos ao redigitar`() {
        assertEquals("22:43", formatTimeInput("22:43"))
    }

    @Test
    fun `mascara de hora limita a quatro digitos e ignora letras`() {
        assertEquals("22:43", formatTimeInput("224399"))
        assertEquals("", formatTimeInput("abc"))
        assertEquals("22:43", formatTimeInput("2a2b4c3"))
    }

    @Test
    fun `mascara de data monta ddMMyyyy`() {
        assertEquals("", formatDateInput(""))
        assertEquals("2", formatDateInput("2"))
        assertEquals("20", formatDateInput("20"))
        assertEquals("20/1", formatDateInput("201"))
        assertEquals("20/10", formatDateInput("2010"))
        assertEquals("20/10/2026", formatDateInput("20102026"))
    }

    @Test
    fun `mascara de data nao duplica barras e limita a oito digitos`() {
        assertEquals("20/10/2026", formatDateInput("20/10/2026"))
        assertEquals("20/10/2026", formatDateInput("2010202699"))
        assertEquals("", formatDateInput("//--"))
    }

    @Test
    fun `campo mascarado devolve cursor no fim do texto`() {
        val resultado = maskedFieldValue(TextFieldValue("0700", TextRange(0)), ::formatTimeInput)

        assertEquals("07:00", resultado.text)
        assertEquals(TextRange(5), resultado.selection)
    }

    @Test
    fun `valida data real e recusa data impossivel`() {
        assertTrue(isValidWorkDate("20/10/2026"))
        assertTrue(isValidWorkDate("29/02/2024"))
        assertFalse(isValidWorkDate("31/02/2026"))
        assertFalse(isValidWorkDate("20/13/2026"))
        assertFalse(isValidWorkDate("20/10/26"))
        assertFalse(isValidWorkDate(""))
    }
}
