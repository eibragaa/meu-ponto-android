package br.com.jean.meuponto.ui

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.jean.meuponto.domain.PeriodStats
import br.com.jean.meuponto.domain.WorkDayStats
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

/** Mês inicial do calendário compacto: hoje quando pertence ao período, senão o último mês. */
internal fun initialCompactMonth(start: LocalDate, end: LocalDate, today: LocalDate): YearMonth {
    if (end.isBefore(start)) return YearMonth.from(end)
    return if (!today.isBefore(start) && !today.isAfter(end)) YearMonth.from(today)
    else YearMonth.from(end)
}

/** Avança ou recua um mês, mantendo a seleção dentro dos meses tocados pelo período. */
internal fun stepCompactMonth(
    current: YearMonth,
    start: LocalDate,
    end: LocalDate,
    delta: Int
): YearMonth {
    val months = monthsInPeriod(start, end)
    if (months.isEmpty()) return current
    val currentIndex = months.indexOf(current).takeIf { it >= 0 } ?: when {
        current.isBefore(months.first()) -> 0
        else -> months.lastIndex
    }
    return months[(currentIndex + delta).coerceIn(0, months.lastIndex)]
}

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
    onSelectDay: (String) -> Unit = {},
    expectedDailyMinutes: Int? = null,
    onDismiss: () -> Unit
) {
    val reduceMotion = remember { !ValueAnimator.areAnimatorsEnabled() }
    val sheetEnter = if (reduceMotion) {
        EnterTransition.None
    } else {
        fadeIn(animationSpec = tween(durationMillis = 200)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 12 }
            )
    }
    var compactMonthKey by rememberSaveable(stats.periodStart, stats.periodEnd) {
        mutableStateOf(initialCompactMonth(stats.periodStart, stats.periodEnd, LocalDate.now()).toString())
    }
    val compactMonth = YearMonth.parse(compactMonthKey)

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

            if (monthsInPeriod(stats.periodStart, stats.periodEnd).isNotEmpty()) {
                CompactMonthCalendar(
                    month = compactMonth,
                    stats = stats,
                    expectedDailyMinutes = expectedDailyMinutes,
                    periodStart = stats.periodStart,
                    periodEnd = stats.periodEnd,
                    onSelectDay = onSelectDay,
                    onMonthChange = { compactMonthKey = it.toString() }
                )
            }

            MonthHeatCard(
                stats = stats,
                expectedDailyMinutes = expectedDailyMinutes,
                onSelectDay = onSelectDay
            )

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

/** Calendário mensal compacto que usa o mesmo contêiner rolável da folha. */
@Composable
private fun CompactMonthCalendar(
    month: YearMonth,
    stats: PeriodStats,
    expectedDailyMinutes: Int?,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    onSelectDay: (String) -> Unit,
    onMonthChange: (YearMonth) -> Unit
) {
    val months = monthsInPeriod(periodStart, periodEnd)
    val index = months.indexOf(month).coerceAtLeast(0)
    val target = expectedDailyMinutes ?: stats.averageMinutes
    val weeks = monthWeeks(month)
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
                    onClick = { onMonthChange(months[index - 1]) },
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
                    onClick = { onMonthChange(months[index + 1]) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Próximo mês")
                }
            }
            if (months.size > 1) {
                Text(
                    text = "Meses do período: " + months.joinToString(" · ") {
                        it.format(monthTitleFormatter).replaceFirstChar { first -> first.uppercaseChar() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Meses disponíveis no período: " +
                                months.joinToString(" e ") { it.format(monthTitleFormatter) }
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
                            Spacer(Modifier.weight(1f).height(36.dp))
                        } else {
                            CompactHeatDayCell(
                                date = date,
                                day = stats.days[insightDateFormatter.format(date)],
                                inPeriod = !date.isBefore(periodStart) && !date.isAfter(periodEnd),
                                target = target,
                                modifier = Modifier.weight(1f),
                                onSelectDay = onSelectDay
                            )
                        }
                    }
                }
            }
            Text(
                "Toque em um dia para ver os comprovantes.",
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
    modifier: Modifier,
    onSelectDay: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val state = dayVisualState(day, inPeriod)
    val minutes = day?.minutes ?: 0
    val level = if (inPeriod) dayHeatLevel(minutes, target) else HeatLevel.NONE
    val fill = when (level) {
        HeatLevel.FULL -> colors.primary
        HeatLevel.MEDIUM -> colors.primary.copy(alpha = 0.55f)
        HeatLevel.LOW -> colors.primary.copy(alpha = 0.22f)
        HeatLevel.NONE -> if (inPeriod) colors.surface else Color.Transparent
    }
    val dateText = insightDateFormatter.format(date)
    val description = buildList {
        add(dateText)
        when (state) {
            DayVisualState.COMPLETE -> add("saída ${day?.exitTime}")
            DayVisualState.INCOMPLETE -> add("pendência${day?.issues?.let { ": ${dayIssueSummary(it)}" } ?: ""}")
            DayVisualState.NONE -> add(if (inPeriod) "sem registro" else "fora do período")
        }
        if (day?.exitCrossesMidnight == true) add("dia seguinte")
    }.joinToString(", ")

    Box(
        modifier = modifier
            .height(36.dp)
            .clickable(enabled = inPeriod) { onSelectDay(dateText) }
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
                color = if (level == HeatLevel.FULL) colors.onPrimary else colors.onSurface
            )
        }
        if (state == DayVisualState.INCOMPLETE) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.error)
            )
        }
    }
}

/**
 * Cartões com uma grade para cada mês tocado pelo período: título, cabeçalho da semana e um dia por célula.
 *
 * A referência de cor é [expectedDailyMinutes] quando informada; senão, a média do período. Se a média
 * for zero (nenhuma hora no período), [dayHeatLevel] recebe a referência zerada e cada dia com horas cai em LOW.
 */
@Composable
private fun MonthHeatCard(stats: PeriodStats, expectedDailyMinutes: Int?, onSelectDay: (String) -> Unit) {
    val target = expectedDailyMinutes ?: stats.averageMinutes
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        monthsInPeriod(stats.periodStart, stats.periodEnd).forEach { month ->
            MonthHeatGrid(
                month = month,
                stats = stats,
                target = target,
                expectedDailyMinutes = expectedDailyMinutes,
                onSelectDay = onSelectDay
            )
        }
    }
}

@Composable
private fun MonthHeatGrid(
    month: YearMonth,
    stats: PeriodStats,
    target: Int?,
    expectedDailyMinutes: Int?,
    onSelectDay: (String) -> Unit
) {
    val reduceMotion = remember { !ValueAnimator.areAnimatorsEnabled() }
    val animationProgress = remember(month) { Animatable(initialDayAnimationProgress(reduceMotion)) }
    LaunchedEffect(month, reduceMotion) {
        if (reduceMotion) {
            animationProgress.snapTo(1f)
        } else if (animationProgress.value < 1f) {
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
    }
    val weeks = monthWeeks(month)
    val legend = "Preenchimento: horas do dia contra " +
        (expectedDailyMinutes?.let { "a jornada de ${formatMinutes(it)}" } ?: "a média do período (${formatMinutes(stats.averageMinutes)})") +
        ". Ponto vermelho: dia com pendência. Ponto cinza: ocorrência."

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                month.format(monthTitleFormatter).replaceFirstChar { it.uppercaseChar() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                weekdayHeaders.forEach { header ->
                    Text(
                        header,
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            weeks.forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            HeatDayCell(
                                date = date,
                                day = stats.days[insightDateFormatter.format(date)],
                                inPeriod = !date.isBefore(stats.periodStart) && !date.isAfter(stats.periodEnd),
                                target = target,
                                animationProgress = animationProgress.value,
                                modifier = Modifier.weight(1f),
                                onSelectDay = onSelectDay
                            )
                        }
                    }
                }
            }
            Text(
                legend,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Uma célula da grade. Preenchimento pelo [HeatLevel] do dia; ponto de erro para pendência e ponto
 * neutro para ocorrência. Dias fora do período ficam esmaecidos e não respondem ao toque.
 */
@Composable
private fun HeatDayCell(
    date: LocalDate,
    day: WorkDayStats?,
    inPeriod: Boolean,
    target: Int?,
    animationProgress: Float,
    modifier: Modifier,
    onSelectDay: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val minutes = day?.minutes ?: 0
    val level = if (inPeriod) dayHeatLevel(minutes, target) else HeatLevel.NONE
    val fill = when (level) {
        HeatLevel.FULL -> colors.primary
        HeatLevel.MEDIUM -> colors.primary.copy(alpha = 0.55f)
        HeatLevel.LOW -> colors.primary.copy(alpha = 0.22f)
        HeatLevel.NONE -> if (inPeriod) colors.surfaceVariant else Color.Transparent
    }
    val textColor = when {
        level == HeatLevel.FULL -> colors.onPrimary
        inPeriod -> colors.onSurface
        else -> colors.onSurface.copy(alpha = 0.35f)
    }
    val dateText = insightDateFormatter.format(date)
    val state = dayVisualState(day, inPeriod)
    val isIncomplete = state == DayVisualState.INCOMPLETE
    val description = buildList {
        add(dateText)
        add(
            when {
                !inPeriod -> "fora do período"
                day == null -> "sem registro"
                else -> formatMinutes(minutes)
            }
        )
        if (day != null && day.issues.isNotEmpty()) add("pendência: ${dayIssueSummary(day.issues)}")
        if (day != null && day.occurrences.isNotEmpty()) add(dayOccurrenceSummary(day.occurrences))
        if (state == DayVisualState.COMPLETE) {
            add("saída ${day?.exitTime.orEmpty()}")
            if (day?.exitCrossesMidnight == true) add("dia seguinte")
        } else if (state == DayVisualState.INCOMPLETE && day?.issues?.isEmpty() == true) {
            add("pendência: saída não registrada")
        }
    }.joinToString(", ")

    val showClock = state != DayVisualState.NONE && animationProgress < 1f
    val rotation = animationProgress * 360f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(fill)
            .clickable(enabled = inPeriod) { onSelectDay(dateText) }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (showClock) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = colors.onSurface.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
                    .size(10.dp)
                    .rotate(rotation)
            )
        }
        if (state == DayVisualState.COMPLETE && animationProgress >= 1f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor
                )
                Text(
                    day?.exitTime.orEmpty(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = textColor
                )
            }
        } else {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
        }
        if (day != null && day.issues.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.error)
            )
        }
        if (day != null && day.occurrences.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(colors.onSurfaceVariant)
            )
        }
        if (isIncomplete && animationProgress >= 1f) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(10.dp)
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
