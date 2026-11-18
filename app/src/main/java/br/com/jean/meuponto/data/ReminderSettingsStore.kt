package br.com.jean.meuponto.data

import android.content.Context
import androidx.core.content.edit
import br.com.jean.meuponto.domain.DEFAULT_LEAD_MINUTES
import br.com.jean.meuponto.domain.REMINDER_LEAD_OPTIONS
import br.com.jean.meuponto.domain.ReminderSettings

/**
 * Persistência local das configurações de lembrete em `SharedPreferences`.
 *
 * Compartilha o arquivo `meu_ponto_preferences` com as demais preferências simples do app e valida
 * o tempo de antecedência contra [REMINDER_LEAD_OPTIONS].
 */
object ReminderSettingsStore {
    private const val PREFS = "meu_ponto_preferences"
    private const val KEY_ENABLED = "reminder_enabled"
    private const val KEY_LEAD_MINUTES = "reminder_lead_minutes"

    fun load(context: Context): ReminderSettings {
        val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val enabled = preferences.getBoolean(KEY_ENABLED, false)
        val storedLead = preferences.getInt(KEY_LEAD_MINUTES, DEFAULT_LEAD_MINUTES)
        val leadMinutes = storedLead.takeIf { it in REMINDER_LEAD_OPTIONS } ?: DEFAULT_LEAD_MINUTES
        return ReminderSettings(enabled = enabled, leadMinutes = leadMinutes)
    }

    fun save(context: Context, settings: ReminderSettings) {
        val validLead = settings.leadMinutes.takeIf { it in REMINDER_LEAD_OPTIONS } ?: DEFAULT_LEAD_MINUTES
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_ENABLED, settings.enabled)
            putInt(KEY_LEAD_MINUTES, validLead)
        }
    }
}
