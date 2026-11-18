package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import java.time.Duration
import java.time.LocalDateTime

/**
 * Indica se existe uma batida de entrada em aberto (sem saída pareada e sem ocorrência) dentro da
 * janela plausível de turno ([WorkStatsCalculator.MAX_SHIFT_MINUTES]) e não posterior a [now].
 *
 * Reusa [PunchPairing.pair] para compartilhar exatamente a mesma regra de pareamento entre dias
 * (inclusive turnos noturnos) sem duplicar lógica.
 */
fun hasOpenEntry(receipts: List<Receipt>, now: LocalDateTime): Boolean {
    if (receipts.isEmpty()) return false
    return PunchPairing.pair(receipts).shifts.any { shift ->
        if (shift.occurrence != null || shift.entry == null || shift.exit != null) {
            false
        } else {
            val entryDate = WorkStatsCalculator.parseWorkDate(shift.entry.receipt.workDate)
            if (entryDate == null) {
                false
            } else {
                val entryAt = entryDate.atTime(shift.entry.time)
                val elapsedMinutes = Duration.between(entryAt, now).toMinutes()
                elapsedMinutes in 0L..WorkStatsCalculator.MAX_SHIFT_MINUTES.toLong()
            }
        }
    }
}
