package br.com.jean.meuponto.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterTest {

    @Test
    fun testIsNewerVersion_simpleHigherMinorOrPatch() {
        assertTrue(AppUpdater.isNewerVersion("0.4.1", "0.4.0"))
        assertTrue(AppUpdater.isNewerVersion("0.5.0", "0.4.0"))
        assertTrue(AppUpdater.isNewerVersion("1.0.0", "0.4.0"))
        assertTrue(AppUpdater.isNewerVersion("v0.4.1", "0.4.0"))
        assertTrue(AppUpdater.isNewerVersion("v1.0.0", "v0.4.0"))
    }

    @Test
    fun testIsNewerVersion_equalOrOlder() {
        assertFalse(AppUpdater.isNewerVersion("0.4.0", "0.4.0"))
        assertFalse(AppUpdater.isNewerVersion("v0.4.0", "0.4.0"))
        assertFalse(AppUpdater.isNewerVersion("0.3.3", "0.4.0"))
        assertFalse(AppUpdater.isNewerVersion("v0.3.3", "0.4.0"))
        assertFalse(AppUpdater.isNewerVersion("0.2.5", "0.3.0"))
    }

    @Test
    fun testIsNewerVersion_differentLengthParts() {
        assertTrue(AppUpdater.isNewerVersion("0.4.0.1", "0.4.0"))
        assertFalse(AppUpdater.isNewerVersion("0.4", "0.4.0"))
        assertTrue(AppUpdater.isNewerVersion("0.4.1", "0.4"))
    }

    @Test
    fun `compara cada parte como numero e nao como texto`() {
        assertTrue(AppUpdater.isNewerVersion("0.5.10", "0.5.9"))
        assertTrue(AppUpdater.isNewerVersion("10.0", "9.9"))
        assertTrue(AppUpdater.isNewerVersion("1.0.0", "0.99.99"))
    }

    @Test
    fun `ignora sufixo de pre-lancamento`() {
        assertTrue(AppUpdater.isNewerVersion("0.5.2-beta", "0.5.1"))
        assertFalse(AppUpdater.isNewerVersion("0.5.1-beta", "0.5.1"))
    }

    @Test
    fun `versao remota invalida nao oferece atualizacao`() {
        assertFalse(AppUpdater.isNewerVersion("", "0.5.0"))
        assertFalse(AppUpdater.isNewerVersion("abc", "0.5.0"))
        assertFalse(AppUpdater.isNewerVersion("0.5.0", "0.5.0.0"))
    }
}
