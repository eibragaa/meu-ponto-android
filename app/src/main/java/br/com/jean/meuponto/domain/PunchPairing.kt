package br.com.jean.meuponto.domain

import br.com.jean.meuponto.data.Receipt
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Uma batida (horário) vinda de um comprovante. O tipo (entrada ou saída) é dado pelo lado do turno. */
internal data class ShiftPunch(val receipt: Receipt, val time: LocalTime)

/**
 * Turno formado pelo pareamento: a unidade que alimenta os minutos e as pendências de
 * [WorkStatsCalculator.dayStats] e, uma a uma, as jornadas de [journeys].
 *
 * [key] é o dia creditado: o dia da entrada, ou o dia do próprio comprovante quando não há entrada.
 * [receipts] são os comprovantes de origem, sem repetição, na ordem entrada -> saída.
 */
internal class PairedShift(
    val key: String,
    val entry: ShiftPunch?,
    val exit: ShiftPunch?,
    val receipts: List<Receipt>,
    val minutes: Int,
    val issues: Set<DayIssue>,
    val occurrence: DayOccurrence? = null
)

/**
 * Saída de um único passe ([PunchPairing.pair]). [minutesByDate] e [issuesByDate] são as horas e as
 * pendências por dia; [shifts] são os turnos. Os três saem do mesmo passe, então não podem divergir.
 */
internal class PairingResult(
    val minutesByDate: Map<String, Int>,
    val issuesByDate: Map<String, Set<DayIssue>>,
    val shifts: List<PairedShift>
)

/**
 * Pareamento de batidas em um único passe. É o único dono da regra de entrada/saída.
 *
 * Entrada e saída podem vir em comprovantes diferentes, inclusive em dias diferentes (turno noturno).
 * O pareamento é cronológico e uma entrada só é pareada com a primeira saída posterior dentro de
 * [WorkStatsCalculator.MAX_SHIFT_MINUTES].
 */
internal object PunchPairing {

    /** [UNKNOWN] é um horário solto, sem dizer se é entrada ou saída. */
    private enum class PunchKind { ENTRY, EXIT, UNKNOWN }

    /** Batida datada: já com o dia de [key] e o instante completo, para o pareamento entre dias. */
    private data class DatedPunch(
        val receipt: Receipt,
        val key: String,
        val at: LocalDateTime,
        val kind: PunchKind
    )

    /**
     * Passe único sobre os comprovantes. Cada comprovante cai em exatamente um [PairedShift], então a
     * lista de jornadas cobre todos os comprovantes sem perder nem repetir nenhum.
     *
     * Comprovante com entrada e saída no mesmo registro forma o próprio turno e fica fora do
     * pareamento. Comprovante com [Receipt.occurrence] também fica fora: a batida esquecida não pode
     * ser pareada com uma batida de outro dia.
     *
     * Uma batida sozinha (entrada ou saída sem par) **não** vira pendência por si: a batida que falta
     * pode chegar depois, em outro comprovante. Só quando uma batida fica sem par dentro de
     * [WorkStatsCalculator.MAX_SHIFT_MINUTES] é que vira [DayIssue.MISSING_EXIT] ou
     * [DayIssue.MISSING_ENTRY] (batida esquecida).
     */
    fun pair(receipts: List<Receipt>): PairingResult {
        val pass = Pass()
        receipts.forEach(pass::classify)
        pass.pairChronologically(pass.resolveUntyped())
        return pass.result()
    }

    /** Estado do passe: acumula minutos, pendências por dia e turnos enquanto percorre os comprovantes. */
    private class Pass {
        private val minutesByDate = mutableMapOf<String, Int>()
        private val issuesByDate = mutableMapOf<String, MutableSet<DayIssue>>()
        private val shifts = mutableListOf<PairedShift>()
        private val dated = mutableListOf<DatedPunch>()

        fun result() = PairingResult(minutesByDate, issuesByDate, shifts)

        /**
         * Classifica um comprovante. Motivos que são do próprio comprovante (data desconhecida,
         * marcado para revisão sem horário legível) valem para o dia e para o turno que ele formar
         * sozinho.
         */
        fun classify(receipt: Receipt) {
            val key = receipt.workDate.ifBlank { REVIEW_DATE_KEY }
            val date = WorkStatsCalculator.parseWorkDate(receipt.workDate)
            val occurrence = receipt.occurrence
            val ownIssues = buildSet {
                if (date == null) add(DayIssue.UNKNOWN_DATE)
                if (occurrence == null && receipt.needsReview && !receipt.hasReadablePunch()) {
                    add(DayIssue.MARKED_FOR_REVIEW)
                }
            }
            ownIssues.forEach { reportDay(key, it) }

            val entry = WorkStatsCalculator.parseClockTime(receipt.entryTime)
            val exit = WorkStatsCalculator.parseClockTime(receipt.exitTime)
            when {
                occurrence != null ->
                    shifts += PairedShift(key, null, null, listOf(receipt), 0, ownIssues, occurrence)
                entry != null && exit != null -> addSameReceiptShift(receipt, key, entry, exit, ownIssues)
                // Sem data não há como casar com outra batida: o comprovante fica como turno sozinho.
                date == null -> shifts += PairedShift(
                    key,
                    entry?.let { ShiftPunch(receipt, it) },
                    exit?.let { ShiftPunch(receipt, it) },
                    listOf(receipt),
                    0,
                    ownIssues
                )
                else -> classifyDated(receipt, key, date, entry, exit, ownIssues)
            }
        }

        /**
         * Par completo no mesmo comprovante. A duração vem de [WorkStatsCalculator.durationMinutes], que
         * aplica o teto de [WorkStatsCalculator.MAX_SHIFT_MINUTES]; acima disso o dia fica pendente.
         */
        private fun addSameReceiptShift(
            receipt: Receipt,
            key: String,
            entry: LocalTime,
            exit: LocalTime,
            ownIssues: Set<DayIssue>
        ) {
            val worked = WorkStatsCalculator.durationMinutes(receipt.entryTime, receipt.exitTime)
            val issues = if (worked != null) {
                ownIssues
            } else {
                reportDay(key, DayIssue.IMPLAUSIBLE_SHIFT)
                ownIssues + DayIssue.IMPLAUSIBLE_SHIFT
            }
            if (worked != null) addMinutes(key, worked)
            shifts += PairedShift(
                key,
                ShiftPunch(receipt, entry),
                ShiftPunch(receipt, exit),
                listOf(receipt),
                worked ?: 0,
                issues
            )
        }

        private fun classifyDated(
            receipt: Receipt,
            key: String,
            date: LocalDate,
            entry: LocalTime?,
            exit: LocalTime?,
            ownIssues: Set<DayIssue>
        ) {
            val punch = when {
                entry != null -> DatedPunch(receipt, key, date.atTime(entry), PunchKind.ENTRY)
                exit != null -> DatedPunch(receipt, key, date.atTime(exit), PunchKind.EXIT)
                else -> WorkStatsCalculator.parseClockTime(receipt.workTime)
                    ?.let { DatedPunch(receipt, key, date.atTime(it), PunchKind.UNKNOWN) }
            }
            if (punch == null) {
                shifts += PairedShift(key, null, null, listOf(receipt), 0, ownIssues)
            } else {
                dated += punch
            }
        }

        /**
         * Batida sem tipo (o OCR leu só um horário) só vira entrada ou saída quando o dia não deixa
         * dúvida: há exatamente uma batida tipada no dia e a sem tipo está do lado oposto no relógio.
         * Assim "entrada 08:00 + horário solto 18:30" fecha o dia sem obrigar o usuário a editar.
         * Com duas batidas sem tipo, ou sem batida tipada, a batida fica sem par com
         * [DayIssue.UNTYPED_PUNCH].
         *
         * Devolve as batidas tipadas e as sem tipo já resolvidas, prontas para o pareamento.
         */
        fun resolveUntyped(): List<DatedPunch> {
            val typed = dated.filter { it.kind != PunchKind.UNKNOWN }
            val resolved = mutableListOf<DatedPunch>()
            dated.filter { it.kind == PunchKind.UNKNOWN }.groupBy { it.key }.forEach { (key, untypedOfDay) ->
                val unknown = untypedOfDay.singleOrNull()
                val typedPunch = typed.singleOrNull { it.key == key }
                val inferred = when {
                    unknown == null || typedPunch == null -> null
                    typedPunch.kind == PunchKind.ENTRY && unknown.at.isAfter(typedPunch.at) -> PunchKind.EXIT
                    typedPunch.kind == PunchKind.EXIT && unknown.at.isBefore(typedPunch.at) -> PunchKind.ENTRY
                    else -> null
                }
                if (unknown != null && inferred != null) {
                    resolved += unknown.copy(kind = inferred)
                } else {
                    untypedOfDay.forEach(::leaveUntyped)
                }
            }
            return typed + resolved
        }

        private fun leaveUntyped(punch: DatedPunch) {
            reportDay(punch.key, DayIssue.UNTYPED_PUNCH)
            shifts += PairedShift(punch.key, null, null, listOf(punch.receipt), 0, setOf(DayIssue.UNTYPED_PUNCH))
        }

        /** Percorre as batidas em ordem cronológica; na igualdade de horário, a entrada vem primeiro. */
        fun pairChronologically(punches: List<DatedPunch>) {
            val open = ArrayDeque<DatedPunch>()
            punches
                .sortedWith(compareBy<DatedPunch> { it.at }.thenBy { if (it.kind == PunchKind.ENTRY) 0 else 1 })
                .forEach { punch ->
                    when (punch.kind) {
                        PunchKind.ENTRY -> open.addLast(punch)
                        PunchKind.EXIT -> closeWithExit(open, punch)
                        PunchKind.UNKNOWN -> leaveUntyped(punch)
                    }
                }
            // Entrada que sobrou sem saída: a saída pode chegar em outro comprovante depois. Não é pendência.
            open.forEach { shifts += PairedShift(it.key, it.shiftPunch(), null, listOf(it.receipt), 0, emptySet()) }
        }

        private fun closeWithExit(open: ArrayDeque<DatedPunch>, exit: DatedPunch) {
            // Entrada aberta há mais de [WorkStatsCalculator.MAX_SHIFT_MINUTES] é considerada esquecida:
            // não pareia e deixa o dia daquela entrada pendente, em vez de creditar uma jornada irreal.
            var hadStaleEntry = false
            while (open.isNotEmpty() &&
                Duration.between(open.first().at, exit.at).toMinutes() > WorkStatsCalculator.MAX_SHIFT_MINUTES
            ) {
                val stale = open.removeFirst()
                reportDay(stale.key, DayIssue.MISSING_EXIT)
                shifts += PairedShift(
                    stale.key, stale.shiftPunch(), null, listOf(stale.receipt), 0, setOf(DayIssue.MISSING_EXIT)
                )
                hadStaleEntry = true
            }

            val entry = open.removeFirstOrNull()
            if (entry == null) {
                // Saída sozinha não é pendência. Só vira "saída sem entrada" quando havia uma entrada que
                // expirou (>16h), o que indica a batida intermediária esquecida.
                val issues = if (hadStaleEntry) setOf(DayIssue.MISSING_ENTRY) else emptySet()
                if (hadStaleEntry) reportDay(exit.key, DayIssue.MISSING_ENTRY)
                shifts += PairedShift(exit.key, null, exit.shiftPunch(), listOf(exit.receipt), 0, issues)
                return
            }

            val worked = Duration.between(entry.at, exit.at).toMinutes().toInt()
            if (worked > 0) {
                addMinutes(entry.key, worked)
                shifts += PairedShift(
                    entry.key, entry.shiftPunch(), exit.shiftPunch(),
                    listOf(entry.receipt, exit.receipt), worked, emptySet()
                )
            } else {
                // Entrada e saída no mesmo minuto: não é jornada, é erro de digitação/OCR.
                reportDay(entry.key, DayIssue.IMPLAUSIBLE_SHIFT)
                reportDay(exit.key, DayIssue.IMPLAUSIBLE_SHIFT)
                shifts += PairedShift(
                    entry.key, entry.shiftPunch(), exit.shiftPunch(),
                    listOf(entry.receipt, exit.receipt), 0, setOf(DayIssue.IMPLAUSIBLE_SHIFT)
                )
            }
        }

        private fun reportDay(key: String, issue: DayIssue) {
            issuesByDate.getOrPut(key) { mutableSetOf() } += issue
        }

        private fun addMinutes(key: String, minutes: Int) {
            minutesByDate[key] = (minutesByDate[key] ?: 0) + minutes
        }
    }

    private fun DatedPunch.shiftPunch() = ShiftPunch(receipt, at.toLocalTime())

    /** `true` quando o comprovante não tem nenhum horário que o pareamento consiga usar. */
    private fun Receipt.hasReadablePunch(): Boolean =
        WorkStatsCalculator.parseClockTime(entryTime) != null ||
            WorkStatsCalculator.parseClockTime(exitTime) != null ||
            WorkStatsCalculator.parseClockTime(workTime) != null
}
