package br.com.jean.meuponto.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.jean.meuponto.R
import br.com.jean.meuponto.domain.PunchHabit
import br.com.jean.meuponto.domain.REMINDER_LEAD_OPTIONS
import br.com.jean.meuponto.domain.ReminderSettings
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

private val habitTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Folha de configuração do lembrete local de ponto.
 *
 * Não realiza I/O: apenas exibe o hábito aprendido ([habit]) e devolve a escolha confirmada
 * via [onSave].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSheet(
    settings: ReminderSettings,
    habit: PunchHabit,
    onDismiss: () -> Unit,
    onSave: (ReminderSettings) -> Unit
) {
    var enabled by remember(settings) { mutableStateOf(settings.enabled) }
    var leadMinutes by remember(settings) { mutableIntStateOf(settings.leadMinutes) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.reminder_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "O lembrete aprende seu horário típico de entrada e dias de trabalho a partir dos comprovantes salvos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HabitSummaryCard(habit)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.reminder_sheet_enable),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            LeadOptionsSection(
                leadMinutes = leadMinutes,
                enabled = enabled,
                onSelect = { leadMinutes = it }
            )

            ReminderSheetActions(
                onDismiss = onDismiss,
                onSave = { onSave(ReminderSettings(enabled = enabled, leadMinutes = leadMinutes)) }
            )
        }
    }
}

@Composable
private fun HabitSummaryCard(habit: PunchHabit) {
    val entryTime = habit.entryTime
    val hasHabit = entryTime != null && habit.workdays.isNotEmpty()
    val daysLabel = habit.workdays
        .sortedBy { it.value }
        .joinToString(", ", transform = ::shortDayLabel)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (hasHabit) {
                        "Entrada típica: ${entryTime?.format(habitTimeFormatter)}"
                    } else {
                        "Histórico insuficiente para agendar"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (hasHabit) {
                        "Dias aprendidos: $daysLabel"
                    } else {
                        "Registre pelo menos 2 dias no mesmo dia da semana com horário de entrada."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LeadOptionsSection(
    leadMinutes: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.reminder_sheet_lead_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(REMINDER_LEAD_OPTIONS) { option ->
                val selected = leadMinutes == option
                FilterChip(
                    selected = selected,
                    enabled = enabled,
                    onClick = { onSelect(option) },
                    label = { Text(stringResource(R.string.reminder_sheet_lead_minutes, option)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun ReminderSheetActions(
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.reminder_sheet_cancel))
        }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onSave, shape = RoundedCornerShape(14.dp)) {
            Text(stringResource(R.string.reminder_sheet_save))
        }
    }
}

private fun shortDayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Seg"
    DayOfWeek.TUESDAY -> "Ter"
    DayOfWeek.WEDNESDAY -> "Qua"
    DayOfWeek.THURSDAY -> "Qui"
    DayOfWeek.FRIDAY -> "Sex"
    DayOfWeek.SATURDAY -> "Sáb"
    DayOfWeek.SUNDAY -> "Dom"
}
