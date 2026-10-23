package br.com.jean.meuponto.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdatePreferencesTest {

    private class InMemoryUpdateStore : UpdateStore {
        private val strings = mutableMapOf<String, String>()
        private val longs = mutableMapOf<String, Long>()

        override fun getString(key: String): String? =
            strings[key]

        override fun putString(key: String, value: String?) {
            if (value == null) {
                strings.remove(key)
            } else {
                strings[key] = value
            }
        }

        override fun getLong(key: String, defaultValue: Long): Long =
            longs[key] ?: defaultValue

        override fun putLong(key: String, value: Long) {
            longs[key] = value
        }
    }

    private val store = InMemoryUpdateStore()
    private val preferences = UpdatePreferences(store)

    @Test
    fun `comeca sem checagem dispensa ou notas pendentes`() {
        assertEquals(0L, preferences.lastCheckAt())
        assertNull(preferences.dismissedVersion())
        assertNull(preferences.consumePendingNotes("0.5.1"))
    }

    @Test
    fun `registra o instante da ultima checagem`() {
        preferences.recordCheck(1_000L)
        assertEquals(1_000L, preferences.lastCheckAt())

        preferences.recordCheck(2_000L)
        assertEquals(2_000L, preferences.lastCheckAt())
    }

    @Test
    fun `guarda a versao dispensada no banner`() {
        preferences.dismissVersion("0.5.2")
        assertEquals("0.5.2", preferences.dismissedVersion())

        preferences.dismissVersion("0.5.3")
        assertEquals("0.5.3", preferences.dismissedVersion())
    }

    @Test
    fun `consome notas pendentes da versao instalada e apaga`() {
        val pending = PendingNotes(version = "0.5.2", notes = "Correções")
        preferences.savePendingNotes(pending)

        assertEquals(pending, preferences.consumePendingNotes("0.5.2"))
        assertNull(preferences.consumePendingNotes("0.5.2"))
    }

    @Test
    fun `consome e apaga notas de outra versao sem devolver`() {
        preferences.savePendingNotes(PendingNotes(version = "0.5.2", notes = "Correções"))

        assertNull(preferences.consumePendingNotes("0.5.1"))
        assertNull(preferences.consumePendingNotes("0.5.2"))
    }

    @Test
    fun `salvar notas novas sobrescreve as anteriores`() {
        preferences.savePendingNotes(PendingNotes(version = "0.5.1", notes = "Antigas"))
        preferences.savePendingNotes(PendingNotes(version = "0.5.2", notes = "Novas"))

        assertEquals(PendingNotes(version = "0.5.2", notes = "Novas"), preferences.consumePendingNotes("0.5.2"))
        assertNull(preferences.consumePendingNotes("0.5.1"))
    }

    @Test
    fun `limpar notas pendentes nao devolve nada depois`() {
        preferences.savePendingNotes(PendingNotes(version = "0.5.2", notes = "Correções"))
        preferences.clearPendingNotes()

        assertNull(preferences.consumePendingNotes("0.5.2"))
    }
}
