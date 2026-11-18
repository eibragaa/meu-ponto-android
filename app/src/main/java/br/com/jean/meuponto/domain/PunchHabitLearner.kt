package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Hábito de batida aprendido a partir do histórico de comprovantes do usuário.
 *
 * [entryTime] é o horário típico de entrada arredondado para a meia hora mais próxima (`null` se
 * não houver entradas legíveis); [workdays] são os dias da semana com recorrência suficiente.
 */
data class PunchHabit(
    val entryTime: LocalTime?,
    val workdays: Set<DayOfWeek>
)

/**
 * Aprende o horário típico de entrada e os dias habituais de trabalho sem I/O nem heurística opaca.
 *
 * Ocorrências de dia inteiro (falta, folga, atestado) ficam fora do aprendizado; comprovantes de
 * esquecimento ([DayOccurrence.FORGOT_PUNCH]) continuam contando como dia trabalhado.
 */
object PunchHabitLearner {
    /** Mínimo de datas distintas em que um dia da semana deve aparecer para virar dia habitual. */
    internal const val MIN_RECURRENCE_DATES = 2

    /** Passo de agrupamento (em minutos) para tolerar pequenas variações no horário de entrada. */
    private const val ROUND_STEP_MINUTES = 30

    /** Metade do passo: no empate exato de 15 minutos, o arredondamento sobe para o próximo bloco. */
    private const val ROUND_HALF_STEP_MINUTES = 15

    private const val MINUTES_PER_HOUR = 60

    /** Total de minutos em 24 horas para virar corretamente perto da meia-noite (23:50 -> 00:00). */
    private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

    /**
     * Infere [PunchHabit] a partir de [receipts]. Função pura: datas ou horários inválidos são
     * ignorados sem lançar exceções.
     */
    fun learn(receipts: List<Receipt>): PunchHabit {
        val workedReceipts = receipts.filterNot { it.occurrence?.isDayAbsence == true }
        return PunchHabit(
            entryTime = learnEntryTime(workedReceipts),
            workdays = learnWorkdays(workedReceipts)
        )
    }

    private fun learnEntryTime(receipts: List<Receipt>): LocalTime? {
        val frequencyBySlot = receipts
            .mapNotNull { WorkStatsCalculator.parseClockTime(it.entryTime) }
            .map(::roundToNearestHalfHour)
            .groupingBy { it }
            .eachCount()

        return frequencyBySlot.entries
            .minWithOrNull(compareByDescending<Map.Entry<LocalTime, Int>> { it.value }.thenBy { it.key })
            ?.key
    }

    private fun learnWorkdays(receipts: List<Receipt>): Set<DayOfWeek> {
        val distinctDates = receipts
            .mapNotNull { WorkStatsCalculator.parseWorkDate(it.workDate) }
            .toSet()

        return distinctDates
            .groupingBy { it.dayOfWeek }
            .eachCount()
            .filterValues { count -> count >= MIN_RECURRENCE_DATES }
            .keys
    }

    /**
     * Arredonda [time] para a meia hora mais próxima (`:00` ou `:30`), subindo no empate (`:15` e
     * `:45`) e voltando para `00:00` após `23:45`.
     */
    internal fun roundToNearestHalfHour(time: LocalTime): LocalTime {
        val totalMinutes = time.hour * MINUTES_PER_HOUR + time.minute
        val roundedMinutes =
            ((totalMinutes + ROUND_HALF_STEP_MINUTES) / ROUND_STEP_MINUTES) * ROUND_STEP_MINUTES % MINUTES_PER_DAY
        return LocalTime.of(roundedMinutes / MINUTES_PER_HOUR, roundedMinutes % MINUTES_PER_HOUR)
    }
}
