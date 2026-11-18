package br.com.jean.meuponto.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import br.com.jean.meuponto.domain.PunchHabit
import br.com.jean.meuponto.domain.ReminderSettings
import br.com.jean.meuponto.domain.nextReminderAt
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Agenda o lembrete diário local via [AlarmManager.setInexactRepeating] (RTC_WAKEUP).
 *
 * Não solicita permissão de alarme exato: o atraso eventual pela otimização de bateria é aceito,
 * e [ReminderReceiver] filtra na hora do disparo os dias da semana e as folgas configuradas.
 */
object ReminderScheduler {
    internal const val EXTRA_WORKDAYS = "extra_workdays"

    /** Request code fixo para atualizar ou cancelar sempre o mesmo alarme de lembrete. */
    private const val REMINDER_ALARM_REQUEST_CODE = 2001

    fun reschedule(
        context: Context,
        settings: ReminderSettings,
        habit: PunchHabit,
        now: LocalDateTime = LocalDateTime.now()
    ) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        cancelExisting(appContext, alarmManager)

        val entryTime = habit.entryTime
        if (!settings.enabled || entryTime == null || habit.workdays.isEmpty()) return

        val triggerAt = nextReminderAt(now, entryTime, settings.leadMinutes)
        val triggerAtMillis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pendingIntent = buildPendingIntent(
            context = appContext,
            workdays = habit.workdays,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelExisting(context: Context, alarmManager: AlarmManager) {
        val existing = buildPendingIntent(
            context = context,
            workdays = emptySet(),
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(existing)
        existing.cancel()
    }

    private fun buildPendingIntent(
        context: Context,
        workdays: Set<DayOfWeek>,
        flags: Int
    ): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            if (workdays.isNotEmpty()) {
                putExtra(EXTRA_WORKDAYS, workdays.map { it.value }.toIntArray())
            }
        }
        return PendingIntent.getBroadcast(context, REMINDER_ALARM_REQUEST_CODE, intent, flags)
    }
}
