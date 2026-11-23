package br.com.jean.meuponto.ui

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.jean.meuponto.R
import br.com.jean.meuponto.domain.PeriodStats
import br.com.jean.meuponto.domain.WorkDayStats
import br.com.jean.meuponto.domain.WorkStatsCalculator
import br.com.jean.meuponto.domain.dayIssueSummary
import br.com.jean.meuponto.domain.dayOccurrenceSummary
import br.com.jean.meuponto.domain.formatMinutes
import br.com.jean.meuponto.domain.formatSignedMinutes
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val insightDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-BR"))

/** Cabeçalho da grade do mês, domingo primeiro (calendário brasileiro). */
private val weekdayHeaders = listOf("D", "S", "T", "Q", "Q", "S", "S")

/** Colunas da grade do mês: uma por dia da semana. */
private const val DAYS_PER_WEEK = 7

/** Porcentagem da referência a partir da qual um dia sai de [HeatLevel.LOW] e vai a [HeatLevel.MEDIUM]. */
private const val HEAT_MEDIUM_PERCENT = 50

/** Duração da animação de entrada da folha de Insights, em milissegundos. */
private const val SHEET_ENTER_DURATION_MS = 200

/** Duração de uma volta completa do relógio nos dias incompletos, em milissegundos. */
internal const val CLOCK_ROTATION_DURATION_MS = 2_000

/** Ângulo de uma volta completa do relógio animado, em graus. */
internal const val FULL_ROTATION_DEGREES = 360f

/** Transparência aplicada a dias fora do período sem comprovante registrado. */
private const val MUTED_DAY_ALPHA = 0.35f

/**
 * Nível de preenchimento de um dia na grade do mês ("Mapa do mês").
 *
 * - [NONE]: sem horas (sem registro, dia só com ocorrência ou zerado);
 * - [LOW]: houve horas, mas menos da metade da referência, ou não há referência (ver [dayHeatLevel]);
 * - [MEDIUM]: de metade da referência até antes dela;
 * - [FULL]: na referência ou acima.
 */
enum class HeatLevel { NONE, LOW, MEDIUM, FULL }

/**
 * Converte os minutos trabalhados no dia em um [HeatLevel].
 *
 * [targetMinutes] é a referência diária. A tela passa a meta da escala quando existe; senão passa a
 * média do período ([PeriodStats.averageMinutes]). Regras:
 * - `minutes <= 0` -> [HeatLevel.NONE], com ou sem referência;
 * - `targetMinutes == null` (e também `<= 0`, tratado da mesma forma): não há como graduar. Qualquer dia
 *   com horas fica em [HeatLevel.LOW], mostrando só que houve registro, sem afirmar que a meta foi batida;
 * - com referência positiva: `minutes >= target` -> [HeatLevel.FULL]; `minutes >= 50%` do target ->
 *   [HeatLevel.MEDIUM]; abaixo disso -> [HeatLevel.LOW].
 */
internal fun dayHeatLevel(minutes: Int, targetMinutes: Int?): HeatLevel {
    if (minutes <= 0) return HeatLevel.NONE
    val target = targetMinutes?.takeIf { it > 0 } ?: return HeatLevel.LOW
    return when {
        minutes >= target -> HeatLevel.FULL
        minutes * 100 >= target * HEAT_MEDIUM_PERCENT -> HeatLevel.MEDIUM
        else -> HeatLevel.LOW
    }
}

/**
 * Retorna os meses tocados pelo intervalo inclusivo [start]..[end], em ordem cronológica.
 *
 * A lista é vazia quando o intervalo é inválido. A regra fica aqui, junto da formatação da grade,
 * para que a tela não tenha de inferir quantos calendários precisa exibir.
 */
internal fun monthsInPeriod(start: LocalDate, end: LocalDate): List<YearMonth> {
    if (end.isBefore(start)) return emptyList()
    val lastMonth = YearMonth.from(end)
    val months = buildList {
        var current = YearMonth.from(start)
        while (!current.isAfter(lastMonth)) {
            add(current)
            current = current.plusMonths(1)
        }
    }
    return months
}

/** Estado visual derivado do resumo do dia; não depende de animação nem de Compose. */
internal enum class DayVisualState { NONE, COMPLETE, INCOMPLETE }

/**
 * Decide se uma célula mostra estado completo, pendência ou nada.
 * Dias só com saída, ocorrência ou sem horas não ganham relógio: falta contexto de jornada.
 */
internal fun dayVisualState(day: WorkDayStats?, inPeriod: Boolean): DayVisualState {
    if (!inPeriod || day == null || day.occurrences.isNotEmpty()) return DayVisualState.NONE
    if (day.issues.isEmpty() && day.exitTime != null) return DayVisualState.COMPLETE
    return if (day.issues.isNotEmpty() || day.minutes > 0) {
        DayVisualState.INCOMPLETE
    } else {
        DayVisualState.NONE
    }
}

/** Progresso inicial/final da animação de entrada; movimento reduzido começa no estado final. */
internal fun initialDayAnimationProgress(reduceMotion: Boolean): Float = if (reduceMotion) 1f else 0f

/** Indica se o ícone de relógio de um dia incompleto deve girar continuamente. */
internal fun shouldAnimateClock(state: DayVisualState, reduceMotion: Boolean): Boolean =
    state == DayVisualState.INCOMPLETE && !reduceMotion

/** Mês inicial do calendário compacto: hoje quando pertence ao período, senão o último mês. */
internal fun initialCompactMonth(start: LocalDate, end: LocalDate, today: LocalDate): YearMonth {
    if (end.isBefore(start)) return YearMonth.from(end)
    return if (!today.isBefore(start) && !today.isAfter(end)) YearMonth.from(today)
    else YearMonth.from(end)
}

/**
 * Meses navegáveis no calendário dos Insights: cobre todos os meses do período ativo e recua até o
 * mês mais antigo com comprovante salvo (ou avança até o mais recente), em ordem cronológica.
 */
internal fun availableCalendarMonths(
    periodStart: LocalDate,
    periodEnd: LocalDate,
    recordedDateKeys: Collection<String>
): List<YearMonth> {
    val periodMonths = monthsInPeriod(periodStart, periodEnd)
    if (periodMonths.isEmpty()) return emptyList()

    val recordedMonths = recordedDateKeys.mapNotNull { key ->
        WorkStatsCalculator.parseWorkDate(key)?.let(YearMonth::from)
    }
    val firstMonth = recordedMonths.minOrNull()?.takeIf { it.isBefore(periodMonths.first()) }
        ?: periodMonths.first()
    val lastMonth = recordedMonths.maxOrNull()?.takeIf { it.isAfter(periodMonths.last()) }
        ?: periodMonths.last()

    return monthsInPeriod(firstMonth.atDay(1), lastMonth.atDay(1))
}

/** Informa se [month] pertence aos meses tocados pelo período de folha atual. */
internal fun isMonthInPeriod(month: YearMonth, periodStart: LocalDate, periodEnd: LocalDate): Boolean {
    if (periodEnd.isBefore(periodStart)) return false
    return !month.isBefore(YearMonth.from(periodStart)) && !month.isAfter(YearMonth.from(periodEnd))
}

/** Avança ou recua um mês dentro de [availableMonths], respeitando início e fim da lista. */
internal fun stepCalendarMonth(
    current: YearMonth,
    availableMonths: List<YearMonth>,
    delta: Int
): YearMonth {
    if (availableMonths.isEmpty()) return current
    val currentIndex = availableMonths.indexOf(current).takeIf { it >= 0 } ?: when {
        current.isBefore(availableMonths.first()) -> 0
        else -> availableMonths.lastIndex
    }
    return availableMonths[(currentIndex + delta).coerceIn(0, availableMonths.lastIndex)]
}

/** Avança ou recua um mês, mantendo a seleção dentro dos meses tocados pelo período. */
internal fun stepCompactMonth(
    current: YearMonth,
    start: LocalDate,
    end: LocalDate,
    delta: Int
): YearMonth = stepCalendarMonth(current, monthsInPeriod(start, end), delta)

/** Semanas de um mês com domingo na primeira coluna; células vazias são `null`. */
internal fun monthWeeks(month: YearMonth): List<List<LocalDate?>> {
    // DayOfWeek.value vai de 1 (segunda) a 7 (domingo); módulo 7 deixa o domingo na coluna 0.
    val leadingBlanks = month.atDay(1).dayOfWeek.value % DAYS_PER_WEEK
    val monthDays: List<LocalDate?> = List(leadingBlanks) { null } +
        (1..month.lengthOfMonth()).map { month.atDay(it) }
    val trailingBlanks = (DAYS_PER_WEEK - monthDays.size % DAYS_PER_WEEK) % DAYS_PER_WEEK
    return (monthDays + List(trailingBlanks) { null }).chunked(DAYS_PER_WEEK)
}

/**
 * Resumo do período de folha: grade do mês, total, média, pendências, projeção e comparação com o período anterior.
 *
 * Recebe [PeriodStats] já calculado — a tela não recalcula nada e não acessa o banco. Quando o
 * período é inválido, `stats.days` vem vazio e a folha mostra só o aviso, sem números enganosos.
 *
 * [onSelectDay] recebe a data `dd/MM/yyyy` do dia tocado na grade. [expectedDailyMinutes] é a meta
 * diária usada na escala de cor da grade; quando `null`, a grade usa [PeriodStats.averageMinutes] como
 * referência. Os dois parâmetros têm valor padrão, então quem não os passa (ex.: `MainActivity`) não muda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsSheet(
    stats: PeriodStats,
    allDays: Map<String, WorkDayStats> = stats.days,
    onSelectDay: (String) -> Unit = {},
    expectedDailyMinutes: Int? = null,
    onOpenReminder: (() -> Unit)? = null,
    onExportTimesheet: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val reduceMotion = remember { !ValueAnimator.areAnimatorsEnabled() }
    val sheetEnter = if (reduceMotion) {
        EnterTransition.None
    } else {
        fadeIn(animationSpec = tween(durationMillis = SHEET_ENTER_DURATION_MS)) +
            slideInVertically(
                animationSpec = tween(durationMillis = SHEET_ENTER_DURATION_MS, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 12 }
            )
    }
    var compactMonthKey by rememberSaveable(stats.periodStart, stats.periodEnd) {
        mutableStateOf(initialCompactMonth(stats.periodStart, stats.periodEnd, LocalDate.now()).toString())
    }
    val compactMonth = YearMonth.parse(compactMonthKey)
    val calendarMonths = remember(stats.periodStart, stats.periodEnd, allDays) {
        availableCalendarMonths(stats.periodStart, stats.periodEnd, allDays.keys)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        AnimatedVisibility(
            visible = true,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            enter = sheetEnter
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Insights do período",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "${stats.periodStart.format(insightDateFormatter)} a ${stats.periodEnd.format(insightDateFormatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (calendarMonths.isNotEmpty()) {
                CompactMonthCalendar(
                    month = compactMonth,
                    months = calendarMonths,
                    stats = stats,
                    allDays = allDays,
                    expectedDailyMinutes = expectedDailyMinutes,
                    periodStart = stats.periodStart,
                    periodEnd = stats.periodEnd,
                    reduceMotion = reduceMotion,
                    onSelectDay = onSelectDay,
                    onMonthChange = { compactMonthKey = it.toString() }
                )
            }

            InsightCard(
                icon = Icons.Outlined.Schedule,
                title = "Horas trabalhadas",
                value = formatMinutes(stats.totalMinutes),
                detail = "Média de ${formatMinutes(stats.averageMinutes)} por dia com registro"
            )
            InsightCard(
                icon = Icons.Outlined.CalendarMonth,
                title = "Fechamento",
                value = when {
                    LocalDate.now().isAfter(stats.periodEnd) -> "Período encerrado"
                    stats.daysRemaining == 0 -> "Fecha hoje"
                    else -> "Faltam ${stats.daysRemaining} dias"
                },
                detail = "Projeção até o fim: ${formatMinutes(stats.projectedMinutes)}"
            )
            InsightCard(
                icon = Icons.Outlined.CheckCircle,
                title = "Dias com registro",
                value = stats.daysWithRecords.toString(),
                detail = "${stats.completeDays} completos · ${stats.incompleteDays} incompletos"
            )
            InsightCard(
                icon = Icons.Outlined.ErrorOutline,
                title = "Pendências",
                value = stats.pendingReceipts.toString(),
                detail = "${stats.missingWorkdays} dias úteis sem registro no período"
            )

            val previous = stats.previousTotalMinutes
            if (previous != null) {
                val difference = stats.totalMinutes - previous
                InsightCard(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Comparação com período anterior",
                    value = formatSignedMinutes(difference),
                    detail = "Anterior: ${formatMinutes(previous)} · Atual: ${formatMinutes(stats.totalMinutes)}"
                )
            }

            stats.expectedBalanceMinutes?.let { balance ->
                InsightCard(
                    icon = Icons.Outlined.Schedule,
                    title = "Saldo contra jornada esperada",
                    value = formatSignedMinutes(balance),
                    detail = if (balance >= 0) "Você está acima da jornada esperada." else "Horas a compensar no período."
                )
            }

            if (onExportTimesheet != null) {
                Button(
                    onClick = onExportTimesheet,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Espelho de Ponto (PDF)")
                }
                Text(
                    "Tabela A4 com data, batidas, total do dia e campo de assinatura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onOpenReminder != null) {
                OutlinedButton(
                    onClick = onOpenReminder,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.insights_reminder_action))
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Os insights são calculados apenas com os comprovantes salvos neste aparelho. " +
                        "Registros incompletos não entram na soma de horas.",
                    Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/** Calendário mensal compacto único com navegação por meses retroativos e relógio animado nos dias incompletos. */
@Composable
private fun CompactMonthCalendar(
    month: YearMonth,
    months: List<YearMonth>,
    stats: PeriodStats,
    allDays: Map<String, WorkDayStats>,
    expectedDailyMinutes: Int?,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    reduceMotion: Boolean,
    onSelectDay: (String) -> Unit,
    onMonthChange: (YearMonth) -> Unit
) {
    val periodMonths = remember(periodStart, periodEnd) { monthsInPeriod(periodStart, periodEnd) }
    val index = months.indexOf(month).takeIf { it >= 0 } ?: months.lastIndex.coerceAtLeast(0)
    val inActivePeriodMonth = isMonthInPeriod(month, periodStart, periodEnd)
    val target = expectedDailyMinutes ?: stats.averageMinutes
    val weeks = remember(month) { monthWeeks(month) }
    val hasIncompleteInMonth = remember(weeks, allDays, periodStart, periodEnd) {
        weeks.flatten().filterNotNull().any { date ->
            val day = allDays[insightDateFormatter.format(date)]
            val inPeriod = !date.isBefore(periodStart) && !date.isAfter(periodEnd)
            dayVisualState(day, inPeriod || day != null) == DayVisualState.INCOMPLETE
        }
    }
    val clockRotation = if (hasIncompleteInMonth && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "incompleteClock")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = FULL_ROTATION_DEGREES,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = CLOCK_ROTATION_DURATION_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "incompleteClockRotation"
        )
        angle
    } else {
        0f
    }
    val colors = MaterialTheme.colorScheme

    Surface(
        color = colors.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    enabled = index > 0,
                    onClick = { onMonthChange(stepCalendarMonth(month, months, -1)) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Mês anterior")
                }
                Text(
                    month.format(monthTitleFormatter).replaceFirstChar { it.uppercaseChar() },
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant
                )
                IconButton(
                    enabled = index < months.lastIndex,
                    onClick = { onMonthChange(stepCalendarMonth(month, months, 1)) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Próximo mês")
                }
            }
            if (months.size > 1) {
                val subtitle = when {
                    !inActivePeriodMonth ->
                        "Histórico (fora do período atual) · ${index + 1} de ${months.size}"
                    months.size == periodMonths.size ->
                        "Meses do período: " + periodMonths.joinToString(" · ") {
                            it.format(monthTitleFormatter).replaceFirstChar { first -> first.uppercaseChar() }
                        }
                    else ->
                        "Período atual · ${index + 1} de ${months.size} meses disponíveis"
                }
                Text(
                    text = subtitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Navegação de meses: $subtitle"
                        },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                weekdayHeaders.forEach { header ->
                    Text(
                        header,
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            weeks.forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(Modifier.weight(1f).height(38.dp))
                        } else {
                            CompactHeatDayCell(
                                date = date,
                                day = allDays[insightDateFormatter.format(date)],
                                inPeriod = !date.isBefore(periodStart) && !date.isAfter(periodEnd),
                                target = target,
                                reduceMotion = reduceMotion,
                                clockRotation = clockRotation,
                                modifier = Modifier.weight(1f),
                                onSelectDay = onSelectDay
                            )
                        }
                    }
                }
            }
            Text(
                "Toque em um dia para ver os comprovantes. Relógio: dia incompleto · Ponto cinza: ocorrência.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactHeatDayCell(
    date: LocalDate,
    day: WorkDayStats?,
    inPeriod: Boolean,
    target: Int?,
    reduceMotion: Boolean,
    clockRotation: Float,
    modifier: Modifier,
    onSelectDay: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val hasRecord = day != null
    val isInteractive = inPeriod || hasRecord
    val state = dayVisualState(day, isInteractive)
    val minutes = day?.minutes ?: 0
    val level = if (isInteractive) dayHeatLevel(minutes, target) else HeatLevel.NONE
    val fill = when (level) {
        HeatLevel.FULL -> colors.primary
        HeatLevel.MEDIUM -> colors.primary.copy(alpha = 0.55f)
        HeatLevel.LOW -> colors.primary.copy(alpha = 0.22f)
        HeatLevel.NONE -> if (inPeriod) colors.surface else Color.Transparent
    }
    val textColor = when {
        level == HeatLevel.FULL -> colors.onPrimary
        isInteractive -> colors.onSurface
        else -> colors.onSurface.copy(alpha = MUTED_DAY_ALPHA)
    }
    val dateText = insightDateFormatter.format(date)
    val description = buildList {
        add(dateText)
        when (state) {
            DayVisualState.COMPLETE -> add("saída ${day?.exitTime}")
            DayVisualState.INCOMPLETE -> add(
                "pendência${day?.issues?.takeIf { it.isNotEmpty() }?.let { ": ${dayIssueSummary(it)}" } ?: ""}"
            )
            DayVisualState.NONE -> add(
                when {
                    day != null && day.occurrences.isNotEmpty() -> dayOccurrenceSummary(day.occurrences)
                    inPeriod -> "sem registro"
                    else -> "fora do período"
                }
            )
        }
        if (!inPeriod && hasRecord) add("mês anterior")
        if (day?.exitCrossesMidnight == true) add("dia seguinte")
    }.joinToString(", ")

    Box(
        modifier = modifier
            .height(38.dp)
            .clickable(enabled = isInteractive) { onSelectDay(dateText) }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(fill),
            contentAlignment = Alignment.Center
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
        if (state == DayVisualState.INCOMPLETE) {
            val iconRotation = if (shouldAnimateClock(state, reduceMotion)) clockRotation else 0f
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = colors.onSurface.copy(alpha = 0.78f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
                    .size(11.dp)
                    .rotate(iconRotation)
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.error)
            )
        }
        if (day != null && day.occurrences.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(colors.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun InsightCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    detail: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
