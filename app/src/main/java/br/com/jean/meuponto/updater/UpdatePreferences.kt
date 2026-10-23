package br.com.jean.meuponto.updater

import android.content.Context

private const val UPDATE_PREFERENCES_NAME = "meu_ponto_update_preferences"

private const val KEY_LAST_CHECK_AT = "last_check_at"
private const val KEY_DISMISSED_VERSION = "dismissed_version"
private const val KEY_PENDING_NOTES_VERSION = "pending_notes_version"
private const val KEY_PENDING_NOTES = "pending_notes"

/** Armazenamento chave-valor mínimo usado por [UpdatePreferences], sem depender de Android. */
internal interface UpdateStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getLong(key: String, defaultValue: Long): Long
    fun putLong(key: String, value: Long)
}

/** Implementação real em `SharedPreferences`, sem dependência nova. */
internal class SharedPreferencesUpdateStore(context: Context) : UpdateStore {
    private val preferences = context.applicationContext
        .getSharedPreferences(UPDATE_PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? =
        preferences.getString(key, null)

    override fun putString(key: String, value: String?) {
        if (value == null) {
            preferences.edit().remove(key).apply()
        } else {
            preferences.edit().putString(key, value).apply()
        }
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        preferences.getLong(key, defaultValue)

    override fun putLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }
}

/**
 * Preferências do updater: throttle entre verificações, dispensa do banner por versão e notas
 * pendentes para mostrar depois da instalação. O modal não é persistido: ele volta a aparecer em
 * cada verificação bem-sucedida, por decisão de produto.
 */
internal class UpdatePreferences(private val store: UpdateStore) {

    fun lastCheckAt(): Long = store.getLong(KEY_LAST_CHECK_AT, 0L)

    fun recordCheck(now: Long) {
        store.putLong(KEY_LAST_CHECK_AT, now)
    }

    fun dismissedVersion(): String? = store.getString(KEY_DISMISSED_VERSION)

    fun dismissVersion(version: String) {
        store.putString(KEY_DISMISSED_VERSION, version)
    }

    fun savePendingNotes(notes: PendingNotes) {
        store.putString(KEY_PENDING_NOTES_VERSION, notes.version)
        store.putString(KEY_PENDING_NOTES, notes.notes)
    }

    /**
     * Devolve e apaga as notas pendentes apenas quando elas pertencem à versão instalada.
     *
     * Se a versão não bater, apaga do mesmo jeito: notas de outra versão não servem para o
     * "O que há de novo" e não devem reaparecer em outra abertura.
     */
    fun consumePendingNotes(installedVersion: String): PendingNotes? {
        val version = store.getString(KEY_PENDING_NOTES_VERSION)
        val notes = store.getString(KEY_PENDING_NOTES)
        clearPendingNotes()
        return if (version != null && notes != null && version == installedVersion) {
            PendingNotes(version, notes)
        } else {
            null
        }
    }

    fun clearPendingNotes() {
        store.putString(KEY_PENDING_NOTES_VERSION, null)
        store.putString(KEY_PENDING_NOTES, null)
    }
}
