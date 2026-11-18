package br.com.jean.meuponto.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Antecedência padrão (em minutos) para avisar antes do horário típico de entrada. */
const val DEFAULT_LEAD_MINUTES = 30

/** Opções de antecedência (em minutos) oferecidas na configuração do lembrete. */
val REMINDER_LEAD_OPTIONS = listOf(15, 30, 45, 60, 90, 120)

/**
 * Preferências do lembrete local de batida de ponto.
 *
 * [leadMinutes] indica quantos minutos antes do horário típico de entrada o aviso deve disparar.
 */
data class ReminderSettings(
    val enabled: Boolean = false,
    val leadMinutes: Int = DEFAULT_LEAD_MINUTES
)

/**
 * Calcula o próximo instante de disparo diário a partir de [now], [entryTime] e [leadMinutes].
 *
 * Se o horário alvo (`entryTime - leadMinutes`) ainda não passou na data de [now], agenda para hoje;
 * caso contrário (inclusive se for exatamente igual a [now]), agenda para o dia seguinte. Cobre
 * entradas de madrugada (ex.: 00:10 com 30 min de antecedência dispara às 23:40 do dia anterior).
 */
fun nextReminderAt(
    now: LocalDateTime,
    entryTime: LocalTime,
    leadMinutes: Int
): LocalDateTime {
    val triggerTime = entryTime.minusMinutes(leadMinutes.coerceAtLeast(0).toLong())
    val todayTrigger = now.toLocalDate().atTime(triggerTime)
    return if (todayTrigger.isAfter(now)) todayTrigger else todayTrigger.plusDays(1)
}

/**
 * Indica se [date] deve disparar lembrete: pertence aos dias habituais [workdays] e não está
 * marcada como folga/feriado em [daysOff].
 */
fun isReminderDay(
    date: LocalDate,
    workdays: Set<DayOfWeek>,
    daysOff: Set<LocalDate> = emptySet()
): Boolean = date.dayOfWeek in workdays && date !in daysOff
