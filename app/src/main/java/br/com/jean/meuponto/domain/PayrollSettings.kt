package br.com.jean.meuponto.domain

import android.content.Context
import java.time.LocalDate

/**
 * Configuração do período de folha e da jornada esperada.
 *
 * Tudo é opcional: sem configuração, o app assume o mês corrente ([effectiveStart]/[effectiveEnd])
 * e nenhuma jornada diária esperada. [daysOff] existe para não contar feriado/folga como falta.
 */
data class PayrollSettings(
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val expectedDailyMinutes: Int? = null,
    val daysOff: Set<LocalDate> = emptySet()
) {
    val isValid: Boolean
        get() = periodStart != null && periodEnd != null && !periodEnd.isBefore(periodStart)

    fun effectiveStart(today: LocalDate = LocalDate.now()): LocalDate =
        periodStart ?: today.withDayOfMonth(1)

    fun effectiveEnd(today: LocalDate = LocalDate.now()): LocalDate =
        periodEnd ?: today.withDayOfMonth(today.lengthOfMonth())
}

object PayrollSettingsStore {
    private const val PREFS = "meu_ponto_preferences"
    private const val KEY_START = "payroll_period_start"
    private const val KEY_END = "payroll_period_end"
    private const val KEY_EXPECTED = "payroll_expected_daily_minutes"
    private const val KEY_DAYS_OFF = "payroll_days_off"

    fun load(context: Context): PayrollSettings {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val start = preferences.getString(KEY_START, null)?.toLocalDateOrNull()
        val end = preferences.getString(KEY_END, null)?.toLocalDateOrNull()
        val expected = if (preferences.contains(KEY_EXPECTED)) {
            preferences.getInt(KEY_EXPECTED, 0).takeIf { it > 0 }
        } else {
            null
        }
        val daysOff = preferences.getStringSet(KEY_DAYS_OFF, emptySet())
            .orEmpty()
            .mapNotNull { it.toLocalDateOrNull() }
            .toSet()

        return PayrollSettings(
            periodStart = start,
            periodEnd = end,
            expectedDailyMinutes = expected,
            daysOff = daysOff
        )
    }

    fun save(context: Context, settings: PayrollSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (settings.periodStart != null) {
                putString(KEY_START, settings.periodStart.toString())
            } else {
                remove(KEY_START)
            }
            if (settings.periodEnd != null) {
                putString(KEY_END, settings.periodEnd.toString())
            } else {
                remove(KEY_END)
            }
            if (settings.expectedDailyMinutes != null) {
                putInt(KEY_EXPECTED, settings.expectedDailyMinutes)
            } else {
                remove(KEY_EXPECTED)
            }
            putStringSet(KEY_DAYS_OFF, settings.daysOff.map { it.toString() }.toSet())
        }.apply()
    }

    private fun String.toLocalDateOrNull(): LocalDate? =
        runCatching { LocalDate.parse(this) }.getOrNull()
}
