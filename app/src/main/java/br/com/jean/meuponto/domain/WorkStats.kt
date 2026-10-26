package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Motivo pelo qual um dia precisa de revisão.
 *
 * A ordem das constantes é a ordem de exibição no cabeçalho do dia: motivos que impedem o cálculo
 * vêm antes dos que apenas pedem conferência.
 */
enum class DayIssue {
    /** A data do comprovante não foi reconhecida (vazia ou fora do formato `dd/MM/yyyy`). */
    UNKNOWN_DATE,

    /** Entrada e saída no mesmo comprovante resultam em jornada inválida (0 min ou acima de 16h). */
    IMPLAUSIBLE_SHIFT,

    /** O comprovante tem um horário solto, sem indicar se é entrada ou saída. */
    UNTYPED_PUNCH,

    /** Há uma entrada que não encontrou saída dentro do teto de 16h — batida de saída esquecida. */
    MISSING_EXIT,

    /** Há uma saída que não encontrou entrada dentro do teto de 16h — batida de entrada esquecida. */
    MISSING_ENTRY,

    /** O comprovante foi marcado para revisão ([Receipt.needsReview]), sem motivo mais específico. */
    MARKED_FOR_REVIEW
}

data class WorkDayStats(
    val date: String,
    val minutes: Int,
    val receiptCount: Int,
    /** Motivos distintos, na ordem de [DayIssue]; vazio quando o dia está consistente. */
    val issues: List<DayIssue>,
    /** Ocorrências resolvidas no dia (esquecimento, falta, folga, atestado), sem repetição. */
    val occurrences: List<DayOccurrence> = emptyList()
) {
    /** Um dia está pendente quando existe pelo menos um motivo em [issues]. */
    val pending: Boolean get() = issues.isNotEmpty()
}

data class PeriodStats(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val totalMinutes: Int,
    val daysWithRecords: Int,
    val averageMinutes: Int,
    val pendingReceipts: Int,
    val incompleteDays: Int,
    val completeDays: Int,
    val missingWorkdays: Int,
    val daysRemaining: Int,
    val projectedMinutes: Int,
    val previousTotalMinutes: Int?,
    val expectedBalanceMinutes: Int?,
    val days: Map<String, WorkDayStats>
)

/**
 * Chave usada no lugar da data quando o OCR não identificou a data do comprovante.
 * Mantida como texto porque é a chave de agrupamento usada na UI e no banco.
 */
const val REVIEW_DATE_KEY = "revisar"

/**
 * Regras de jornada do app: pareamento de batidas, horas por dia e resumo do período.
 *
 * Regra central (ver [pairPunches]): entrada e saída podem vir em comprovantes diferentes, inclusive
 * em dias diferentes (turno noturno). O pareamento é cronológico e uma entrada só é pareada com a
 * primeira saída posterior dentro de [MAX_SHIFT_MINUTES].
 *
 * Datas e horários chegam como texto ("dd/MM/yyyy" e "HH:mm") do OCR ou do banco; a normalização e
 * a validação acontecem aqui, para que a UI só receba dados já validados.
 */
object WorkStatsCalculator {
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT)
    private val datePattern = Regex("\\b(\\d{2})[./-](\\d{2})[./-](\\d{4})\\b")
    private val timePattern = Regex("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b")

    fun normalizeWorkDate(value: String): String? {
        val match = datePattern.find(value) ?: return null
        val normalized = "${match.groupValues[1]}/${match.groupValues[2]}/${match.groupValues[3]}"
        return normalized.takeIf { parseWorkDate(it) != null }
    }

    fun normalizeClockTime(value: String): String? {
        val match = timePattern.find(value) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        return "%02d:%02d".format(hour, minute)
    }

    fun parseWorkDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value.trim(), dateFormatter) }.getOrNull()

    fun parseClockTime(value: String?): LocalTime? {
        val normalized = value?.let(::normalizeClockTime) ?: return null
        return runCatching { LocalTime.parse(normalized, timeFormatter) }.getOrNull()
    }

    /**
     * Duração em minutos entre dois horários do **mesmo** comprovante.
     *
     * Saída menor que entrada significa turno que cruzou a meia-noite (soma um dia). Aplica o mesmo
     * teto de [MAX_SHIFT_MINUTES] usado no pareamento entre comprovantes: acima disso o dado é
     * tratado como erro de digitação/OCR e a duração fica indefinida, em vez de inflar as horas.
     */
    fun durationMinutes(entry: String?, exit: String?): Int? {
        val entryTime = parseClockTime(entry) ?: return null
        val exitTime = parseClockTime(exit) ?: return null
        val minutes = Duration.between(entryTime, exitTime).toMinutes().toInt()
            .let { if (it < 0) it + MINUTES_PER_DAY else it }
        return minutes.takeIf { it in 1..MAX_SHIFT_MINUTES }
    }

    fun durationMinutes(receipt: Receipt): Int? =
        durationMinutes(receipt.entryTime, receipt.exitTime)

    /**
     * Horas por dia considerando todos os comprovantes recebidos.
     *
     * Duas origens de minutos, sem sobreposição:
     * 1. comprovante com entrada e saída no mesmo registro -> [durationMinutes];
     * 2. comprovante com apenas uma das batidas -> pareamento cronológico em [pairPunches], que
     *    credita a duração no dia da *entrada* (turno noturno conta no dia em que começou).
     *
     * O dia fica com [WorkDayStats.pending] quando há pelo menos um [DayIssue]: jornada implausível,
     * batida sem tipo, batida esquecida (sem par dentro do teto de 16h), comprovante marcado para
     * revisão ([Receipt.needsReview]) ou data desconhecida ([REVIEW_DATE_KEY]). O motivo fica em
     * [WorkDayStats.issues] para a UI explicar o "Revisar". Uma batida só já classificada (só entrada
     * ou só saída) **não** é pendência: o par pode chegar depois, em outro comprovante.
     */
    fun dayStats(receipts: List<Receipt>): Map<String, WorkDayStats> {
        val paired = pairPunches(receipts)
        return receipts.groupBy { it.workDate.ifBlank { REVIEW_DATE_KEY } }.mapValues { (date, dayReceipts) ->
            var minutes = paired.minutesByDate[date] ?: 0
            val issues = paired.issuesByDate[date].orEmpty().toMutableSet()
            dayReceipts.forEach { receipt ->
                if (parseWorkDate(receipt.workDate) == null) issues += DayIssue.UNKNOWN_DATE
                // Comprovante com ocorrência já está resolvido: não soma horas nem gera motivo.
                if (receipt.occurrence != null) return@forEach
                if (parseClockTime(receipt.entryTime) != null && parseClockTime(receipt.exitTime) != null) {
                    val worked = durationMinutes(receipt.entryTime, receipt.exitTime)
                    if (worked != null) minutes += worked else issues += DayIssue.IMPLAUSIBLE_SHIFT
                }
            }
            // "Marcado para revisão" só vale para comprovante **sem horário legível**: aí o dia não
            // tem outro motivo para explicar a pendência. Com horário lido, o motivo específico
            // (batida sem par, sem tipo, jornada implausível) já descreve o que falta — repetir a
            // marcação genérica só mantinha o selo "Revisar" aceso depois da correção.
            if (dayReceipts.any { it.needsReview && it.occurrence == null && !it.hasReadablePunch() }) {
                issues += DayIssue.MARKED_FOR_REVIEW
            }
            WorkDayStats(
                date = date,
                minutes = minutes,
                receiptCount = dayReceipts.size,
                issues = issues.sorted(),
                occurrences = dayReceipts.mapNotNull { it.occurrence }.distinct().sorted()
            )
        }
    }

    /**
     * Comprovantes da aba/filtro "Revisar": os marcados para revisão e todos os comprovantes de dias
     * pendentes. A batida que falta pode estar em outro comprovante do mesmo dia, então o dia inteiro
     * aparece para o usuário ver o contexto.
     */
    fun receiptsNeedingReview(receipts: List<Receipt>): List<Receipt> {
        val pendingDays = dayStats(receipts).filterValues { it.pending }.keys
        return receipts.filter { receipt ->
            (receipt.needsReview && receipt.occurrence == null) ||
                receipt.workDate.ifBlank { REVIEW_DATE_KEY } in pendingDays
        }
    }

    /** Resultado da leitura de horários feita pelo OCR. */
    data class OcrPunches(val workTime: String, val entry: String?, val exit: String?)

    /**
     * Interpreta os horários lidos pelo OCR **sem presumir a ordem**:
     * - um horário só não diz se é entrada ou saída: fica sem tipo, para o usuário escolher;
     * - dois ou mais: o primeiro vira entrada e o segundo saída. Se a leitura veio invertida, a folha
     *   de edição tem o botão "Inverter"; a jornada invertida também aparece como pendente.
     */
    fun ocrPunches(times: List<String>): OcrPunches = when (times.size) {
        0 -> OcrPunches(workTime = "", entry = null, exit = null)
        1 -> OcrPunches(workTime = times[0], entry = null, exit = null)
        else -> OcrPunches(workTime = times[0], entry = times[0], exit = times[1])
    }

    fun periodStats(
        receipts: List<Receipt>,
        settings: PayrollSettings,
        today: LocalDate = LocalDate.now()
    ): PeriodStats {
        val start = settings.effectiveStart(today)
        val end = settings.effectiveEnd(today)
        val validRange = !end.isBefore(start)
        val allDays = dayStats(receipts)
        val periodDays = if (validRange) {
            allDays.filterKeys { key ->
                parseWorkDate(key)?.let { !it.isBefore(start) && !it.isAfter(end) } == true
            }
        } else {
            emptyMap()
        }

        val totalMinutes = periodDays.values.sumOf { it.minutes }
        val daysWithRecords = periodDays.values.count { it.receiptCount > 0 }
        val incompleteDays = periodDays.values.count { it.pending }
        val completeDays = periodDays.values.count { !it.pending && it.minutes > 0 }
        val pendingReceipts = receipts.count { receipt ->
            receipt.needsReview && (
                receipt.workDate.isBlank() ||
                    parseWorkDate(receipt.workDate)?.let { !it.isBefore(start) && !it.isAfter(end) } == true
                )
        }
        val averageMinutes = if (daysWithRecords > 0) totalMinutes / daysWithRecords else 0

        val elapsedEnd = when {
            !validRange -> start.minusDays(1)
            today.isBefore(start) -> start.minusDays(1)
            else -> minOf(today, end)
        }
        val elapsedWorkdays = countWorkdays(start, elapsedEnd, settings.daysOff)
        val missingWorkdays = countMissingWorkdays(start, elapsedEnd, periodDays, settings.daysOff)
        val expectedBalance = settings.expectedDailyMinutes?.let { daily ->
            totalMinutes - (daily * elapsedWorkdays)
        }

        val daysRemaining = if (validRange && !today.isAfter(end)) {
            ChronoUnit.DAYS.between(today, end).toInt().coerceAtLeast(0)
        } else {
            0
        }
        val remainingWorkdays = if (validRange) {
            countWorkdays(maxOf(today.plusDays(1), start), end, settings.daysOff)
        } else {
            0
        }
        val projectedMinutes = totalMinutes + (averageMinutes * remainingWorkdays)

        val previousTotal = if (validRange) {
            val length = ChronoUnit.DAYS.between(start, end) + 1
            val previousStart = start.minusDays(length)
            val previousEnd = start.minusDays(1)
            val previousDays = allDays.filterKeys { key ->
                parseWorkDate(key)?.let { !it.isBefore(previousStart) && !it.isAfter(previousEnd) } == true
            }
            previousDays.values.takeIf { days -> days.any { it.receiptCount > 0 } }?.sumOf { it.minutes }
        } else {
            null
        }

        return PeriodStats(
            periodStart = start,
            periodEnd = end,
            totalMinutes = totalMinutes,
            daysWithRecords = daysWithRecords,
            averageMinutes = averageMinutes,
            pendingReceipts = pendingReceipts,
            incompleteDays = incompleteDays,
            completeDays = completeDays,
            missingWorkdays = missingWorkdays,
            daysRemaining = daysRemaining,
            projectedMinutes = projectedMinutes,
            previousTotalMinutes = previousTotal,
            expectedBalanceMinutes = expectedBalance,
            days = periodDays
        )
    }

    fun isWorkday(date: LocalDate, daysOff: Set<LocalDate>): Boolean {
        if (date in daysOff) return false
        return date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY
    }

    /**
     * Pareia batidas soltas (uma por comprovante) em ordem cronológica.
     *
     * Comprovantes que já trazem entrada e saída no mesmo registro são ignorados aqui: eles são
     * somados por [durationMinutes] em [dayStats], o que evita contar a mesma jornada duas vezes.
     * Comprovantes com [Receipt.occurrence] também ficam de fora: a batida esquecida não pode ser
     * pareada com uma batida de outro dia.
     *
     * Uma batida sozinha (entrada sem saída, ou saída sem entrada) **não** vira pendência por si: a
     * batida que falta pode chegar depois, em outro comprovante. Só quando uma batida fica sem par
     * dentro de [MAX_SHIFT_MINUTES] é que vira [DayIssue.MISSING_EXIT]/[DayIssue.MISSING_ENTRY]
     * (batida esquecida).
     */
    private fun pairPunches(receipts: List<Receipt>): PairResult {
        val minutesByDate = mutableMapOf<String, Int>()
        val issuesByDate = mutableMapOf<String, MutableSet<DayIssue>>()
        fun report(key: String, issue: DayIssue) {
            val issues = issuesByDate.getOrPut(key) { mutableSetOf() }
            issues += issue
        }
        val dated = mutableListOf<Punch>()

        receipts.forEach { receipt ->
            if (receipt.occurrence != null) return@forEach
            val key = receipt.workDate.ifBlank { REVIEW_DATE_KEY }
            val entry = parseClockTime(receipt.entryTime)
            val exit = parseClockTime(receipt.exitTime)
            // Par completo no mesmo comprovante: contabilizado por durationMinutes, não aqui.
            if (entry != null && exit != null) return@forEach
            val date = parseWorkDate(receipt.workDate)
            val kinds = when {
                entry != null -> listOf(entry to PunchKind.ENTRY)
                exit != null -> listOf(exit to PunchKind.EXIT)
                else -> listOfNotNull(parseClockTime(receipt.workTime)?.let { it to PunchKind.UNKNOWN })
            }
            kinds.forEach { (time, kind) ->
                // Data não reconhecida: o motivo (UNKNOWN_DATE) é do dia, marcado em dayStats.
                if (date != null) dated += Punch(key, date.atTime(time), kind)
            }
        }

        // Batida sem tipo (OCR leu só um horário) só vira entrada/saída quando o dia não deixa
        // dúvida: há exatamente uma batida tipada e a sem tipo está do lado oposto no relógio.
        // Assim "entrada 08:00 + horário solto 18:30" fecha o dia sem obrigar o usuário a editar.
        // Com duas batidas sem tipo, ou sem batida tipada, o dia fica pendente para o usuário escolher.
        val untypedByDay = dated.filter { it.kind == PunchKind.UNKNOWN }.groupBy { it.key }
        val typed = dated.filter { it.kind != PunchKind.UNKNOWN }
        val resolvedUntyped = mutableListOf<Punch>()
        untypedByDay.forEach { (key, untypedOfDay) ->
            val typedOfDay = typed.filter { it.key == key }
            val unknown = untypedOfDay.singleOrNull()
            val typedPunch = typedOfDay.singleOrNull()
            val inferred = when {
                unknown == null || typedPunch == null -> null
                typedPunch.kind == PunchKind.ENTRY && unknown.at.isAfter(typedPunch.at) -> PunchKind.EXIT
                typedPunch.kind == PunchKind.EXIT && unknown.at.isBefore(typedPunch.at) -> PunchKind.ENTRY
                else -> null
            }
            if (inferred != null && unknown != null) {
                resolvedUntyped += unknown.copy(kind = inferred)
            } else {
                untypedOfDay.forEach { report(it.key, DayIssue.UNTYPED_PUNCH) }
            }
        }

        val open = ArrayDeque<Punch>()
        (typed + resolvedUntyped).sortedWith(compareBy<Punch> { it.at }.thenBy { if (it.kind == PunchKind.ENTRY) 0 else 1 })
            .forEach { punch ->
                when (punch.kind) {
                    PunchKind.ENTRY -> open.addLast(punch)
                    PunchKind.EXIT -> {
                        // Entrada aberta há mais de 16h é considerada esquecida: não pareia e deixa
                        // o dia daquela entrada pendente, em vez de creditar uma jornada irreal.
                        var hadStaleEntry = false
                        while (open.isNotEmpty() &&
                            Duration.between(open.first().at, punch.at).toMinutes() > MAX_SHIFT_MINUTES
                        ) {
                            report(open.removeFirst().key, DayIssue.MISSING_EXIT)
                            hadStaleEntry = true
                        }
                        val entry = open.removeFirstOrNull()
                        if (entry == null) {
                            // Saída sozinha não é pendência: a entrada pode chegar em outro
                            // comprovante depois. Só vira "saída sem entrada" quando havia uma entrada
                            // que expirou (>16h), o que indica a batida intermediária esquecida.
                            if (hadStaleEntry) report(punch.key, DayIssue.MISSING_ENTRY)
                        } else {
                            val worked = Duration.between(entry.at, punch.at).toMinutes().toInt()
                            if (worked > 0) {
                                minutesByDate[entry.key] = (minutesByDate[entry.key] ?: 0) + worked
                            } else {
                                // Entrada e saída no mesmo minuto: não é jornada, é erro de digitação/OCR.
                                report(entry.key, DayIssue.IMPLAUSIBLE_SHIFT)
                                report(punch.key, DayIssue.IMPLAUSIBLE_SHIFT)
                            }
                        }
                    }
                    PunchKind.UNKNOWN -> report(punch.key, DayIssue.UNTYPED_PUNCH)
                }
            }

        return PairResult(minutesByDate, issuesByDate)
    }

    /** `true` quando o comprovante não tem nenhum horário que o pareamento consiga usar. */
    private fun Receipt.hasReadablePunch(): Boolean =
        parseClockTime(entryTime) != null ||
            parseClockTime(exitTime) != null ||
            parseClockTime(workTime) != null

    private fun countWorkdays(start: LocalDate, end: LocalDate, daysOff: Set<LocalDate>): Int {
        if (end.isBefore(start)) return 0
        var count = 0
        var cursor = start
        while (!cursor.isAfter(end)) {
            if (isWorkday(cursor, daysOff)) count++
            cursor = cursor.plusDays(1)
        }
        return count
    }

    private fun countMissingWorkdays(
        start: LocalDate,
        end: LocalDate,
        days: Map<String, WorkDayStats>,
        daysOff: Set<LocalDate>
    ): Int {
        if (end.isBefore(start)) return 0
        val formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")
        var count = 0
        var cursor = start
        while (!cursor.isAfter(end)) {
            if (isWorkday(cursor, daysOff)) {
                val key = cursor.format(formatter)
                if ((days[key]?.receiptCount ?: 0) == 0) count++
            }
            cursor = cursor.plusDays(1)
        }
        return count
    }

    private data class Punch(val key: String, val at: LocalDateTime, val kind: PunchKind)

    private data class PairResult(
        val minutesByDate: Map<String, Int>,
        val issuesByDate: Map<String, Set<DayIssue>>
    )

    private enum class PunchKind { ENTRY, EXIT, UNKNOWN }

    /** Teto de uma jornada plausível: acima disso o pareamento é recusado e o dia fica pendente. */
    private const val MAX_SHIFT_MINUTES = 16 * 60

    private const val MINUTES_PER_DAY = 24 * 60
}

fun formatMinutes(minutes: Int): String {
    if (minutes <= 0) return "0h"
    if (minutes < 60) return "${minutes}min"
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) "${hours}h" else "${hours}h${remainder.toString().padStart(2, '0')}"
}

fun formatSignedMinutes(minutes: Int): String {
    val prefix = if (minutes >= 0) "+" else "-"
    return prefix + formatMinutes(abs(minutes))
}
