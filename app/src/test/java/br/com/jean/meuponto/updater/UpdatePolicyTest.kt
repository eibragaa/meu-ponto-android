package br.com.jean.meuponto.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {

    private val now = 1_000_000_000L

    @Test
    fun `abertura fria verifica mesmo com checagem recente`() {
        assertTrue(shouldCheck(CheckTrigger.ColdStart, now, lastCheckAt = now))
        assertTrue(shouldCheck(CheckTrigger.ColdStart, now, lastCheckAt = now - 1_000L))
    }

    @Test
    fun `foreground dentro do intervalo nao verifica`() {
        val lastCheck = now - AUTO_CHECK_INTERVAL_MS + 1_000L

        assertFalse(shouldCheck(CheckTrigger.Foreground, now, lastCheckAt = lastCheck))
    }

    @Test
    fun `foreground depois do intervalo verifica`() {
        val lastCheck = now - AUTO_CHECK_INTERVAL_MS - 1_000L

        assertTrue(shouldCheck(CheckTrigger.Foreground, now, lastCheckAt = lastCheck))
    }

    @Test
    fun `foreground no limite exato do intervalo verifica`() {
        val lastCheck = now - AUTO_CHECK_INTERVAL_MS

        assertTrue(shouldCheck(CheckTrigger.Foreground, now, lastCheckAt = lastCheck))
    }

    @Test
    fun `foreground sem checagem anterior verifica`() {
        assertTrue(shouldCheck(CheckTrigger.Foreground, now, lastCheckAt = 0L))
    }

    @Test
    fun `relogio para tras trata foreground como devido`() {
        assertTrue(shouldCheck(CheckTrigger.Foreground, now, lastCheckAt = now + 60_000L))
    }

    @Test
    fun `manual ignora o intervalo`() {
        assertTrue(shouldCheck(CheckTrigger.Manual, now, lastCheckAt = now))
    }

    @Test
    fun `modal nao repete para a mesma versao`() {
        assertFalse(shouldShowPrompt("0.5.2", lastPromptedVersion = "0.5.2"))
    }

    @Test
    fun `modal aparece para versao nova ou nunca mostrada`() {
        assertTrue(shouldShowPrompt("0.5.2", lastPromptedVersion = "0.5.1"))
        assertTrue(shouldShowPrompt("0.5.2", lastPromptedVersion = null))
    }

    @Test
    fun `banner nao aparece para versao dispensada`() {
        assertFalse(shouldShowBanner("0.5.2", dismissedVersion = "0.5.2"))
    }

    @Test
    fun `banner aparece para outra versao ou sem dispensa`() {
        assertTrue(shouldShowBanner("0.5.2", dismissedVersion = "0.5.1"))
        assertTrue(shouldShowBanner("0.5.2", dismissedVersion = null))
    }

    @Test
    fun `versao vazia nao abre modal nem banner`() {
        assertFalse(shouldShowPrompt("", lastPromptedVersion = null))
        assertFalse(shouldShowBanner("", dismissedVersion = null))
    }
}
