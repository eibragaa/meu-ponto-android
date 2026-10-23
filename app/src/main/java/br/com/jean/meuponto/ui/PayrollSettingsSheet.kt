package br.com.jean.meuponto.ui

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import br.com.jean.meuponto.domain.PayrollSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Configuração do período de folha e da jornada esperada.
 *
 * Só entrega o resultado ao confirmar ([onSave]); cancelar descarta tudo. Regras de validação que a
 * folha aplica: data final não pode ser anterior à inicial, e a jornada esperada é opcional (sem ela
 * o app não calcula saldo). Dias marcados em `daysOff` não contam como falta no período.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayrollSettingsSheet(
    settings: PayrollSettings,
    onDismiss: () -> Unit,
    onSave: (PayrollSettings) -> Unit
) {
    val context = LocalContext.current
    var start by remember(settings) { mutableStateOf(settings.periodStart) }
    var end by remember(settings) { mutableStateOf(settings.periodEnd) }
    var daysOff by remember(settings) { mutableStateOf(settings.daysOff) }
    var expectedEnabled by remember(settings) { mutableStateOf(settings.expectedDailyMinutes != null) }
    var expectedHours by remember(settings) {
        mutableStateOf(
            settings.expectedDailyMinutes
                ?.let { minutes -> (minutes / 60.0).formatHours() }
                ?: "8"
        )
    }

    val valid = start?.let { startDate -> end?.let { endDate -> !endDate.isBefore(startDate) } } ?: false
    val expectedMinutes = if (expectedEnabled) {
        expectedHours.replace(',', '.').toDoubleOrNull()?.times(60)?.roundToInt()?.takeIf { it > 0 }
    } else {
        null
    }
    val expectedValid = !expectedEnabled || expectedMinutes != null

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Configurar folha",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Escolha o período que será usado no resumo e nos insights.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DateRow(
                label = "Início do período",
                date = start,
                onClick = {
                    openDatePicker(context, start) { start = it }
                }
            )
            DateRow(
                label = "Fechamento da folha",
                date = end,
                onClick = {
                    openDatePicker(context, end) { end = it }
                }
            )
            if (!valid) {
                Text(
                    "A data de fechamento deve ser igual ou posterior ao início.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider()

            Text(
                "Dias livres",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Opcional. Dias marcados aqui não entram como falta no período.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (daysOff.isEmpty()) {
                Text(
                    "Nenhum dia livre adicionado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(daysOff.sorted()) { date ->
                        FilterChip(
                            selected = true,
                            onClick = { daysOff = daysOff - date },
                            label = { Text(date.format(displayDateFormatter)) }
                        )
                    }
                }
                Text(
                    "Toque em uma data para remover.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = {
                    openDatePicker(context, LocalDate.now()) { date ->
                        daysOff = daysOff + date
                    }
                }
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Adicionar dia livre")
            }

            HorizontalDivider()

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Jornada esperada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Opcional. Usada para calcular saldo e faltas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = expectedEnabled,
                    onCheckedChange = { expectedEnabled = it }
                )
            }
            if (expectedEnabled) {
                OutlinedTextField(
                    value = expectedHours,
                    onValueChange = { expectedHours = it.filter { char -> char.isDigit() || char == ',' || char == '.' }.take(5) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Horas por dia") },
                    placeholder = { Text("8") },
                    supportingText = {
                        Text(if (expectedValid) "Ex.: 8 ou 7,5" else "Informe um número maior que zero.")
                    },
                    isError = !expectedValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Spacer(Modifier.height(2.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(
                            PayrollSettings(
                                periodStart = start,
                                periodEnd = end,
                                expectedDailyMinutes = expectedMinutes,
                                daysOff = daysOff
                            )
                        )
                    },
                    enabled = valid && expectedValid,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Salvar período")
                }
            }
        }
    }
}

@Composable
private fun DateRow(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(
                date?.format(displayDateFormatter) ?: "Escolher data",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private val displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun openDatePicker(context: Context, initial: LocalDate?, onDate: (LocalDate) -> Unit) {
    val base = initial ?: LocalDate.now()
    DatePickerDialog(
        context,
        { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day)) },
        base.year,
        base.monthValue - 1,
        base.dayOfMonth
    ).show()
}

private fun Double.formatHours(): String {
    val rounded = (this * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString().replace('.', ',')
}
