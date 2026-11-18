package br.com.jean.meuponto.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.jean.meuponto.domain.PayrollSettingsStore
import br.com.jean.meuponto.domain.isReminderDay
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Recebe o alarme diário agendado por [ReminderScheduler] e só publica a notificação se o
 * lembrete continuar ativado e hoje for um dia habitual de trabalho fora de `daysOff`.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val settings = ReminderSettingsStore.load(context)
        if (!settings.enabled) return

        val workdayValues = intent?.getIntArrayExtra(ReminderScheduler.EXTRA_WORKDAYS) ?: return
        val workdays = buildSet {
            for (dayValue in workdayValues) {
                if (dayValue in 1..DayOfWeek.entries.size) {
                    add(DayOfWeek.of(dayValue))
                }
            }
        }
        if (workdays.isEmpty()) return

        val daysOff = PayrollSettingsStore.load(context).daysOff
        if (isReminderDay(LocalDate.now(), workdays, daysOff)) {
            ReminderNotifier.notify(context)
        }
    }
}
