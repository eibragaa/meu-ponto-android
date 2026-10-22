package br.com.jean.meuponto.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.jean.meuponto.domain.PeriodStats
import br.com.jean.meuponto.domain.formatMinutes
import br.com.jean.meuponto.domain.formatSignedMinutes
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val insightDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsSheet(stats: PeriodStats, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
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
