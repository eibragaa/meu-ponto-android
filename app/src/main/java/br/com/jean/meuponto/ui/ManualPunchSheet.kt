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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import br.com.jean.meuponto.domain.DayOccurrence
import br.com.jean.meuponto.domain.PunchChoice
import br.com.jean.meuponto.domain.WorkStatsCalculator
import br.com.jean.meuponto.domain.formatMinutes
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val dateDisplayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val timeDisplayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Folha de lançamento manual de batida de ponto (sem foto obrigatória).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualPunchSheet(
    initialPunch: PunchChoice?,
    onDismiss: () -> Unit,
    onSave: (workDate: String, entryTime: String?, exitTime: String?, occurrence: DayOccurrence?, note: String) -> Unit
) {
    val todayText = remember { LocalDate.now().format(dateDisplayFormatter) }
    var workDateField by remember {
        mutableStateOf(TextFieldValue(todayText, TextRange(todayText.length)))
    }
    val workDate = workDateField.text

    var choice by remember {
        mutableStateOf(initialPunch ?: PunchChoice.ENTRY)
    }
    var occurrence by remember { mutableStateOf<DayOccurrence?>(null) }

    val nowClock = remember { LocalTime.now().format(timeDisplayFormatter) }
    var entryField by remember {
        val initial = if (choice == PunchChoice.ENTRY || choice == PunchChoice.BOTH) nowClock else ""
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
    var exitField by remember {
        val initial = if (choice == PunchChoice.EXIT) nowClock else ""
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
    var noteField by remember { mutableStateOf("") }

    val entryTime = entryField.text
    val exitTime = exitField.text
    val dateValid = isValidWorkDate(workDate)
    val entryValid = entryTime.isBlank() || WorkStatsCalculator.parseClockTime(entryTime) != null
    val exitValid = exitTime.isBlank() || WorkStatsCalculator.parseClockTime(exitTime) != null

    val timesValid = when (choice) {
        PunchChoice.BOTH -> entryTime.isNotBlank() && exitTime.isNotBlank() && entryValid && exitValid
        PunchChoice.ENTRY -> entryTime.isNotBlank() && entryValid
        PunchChoice.EXIT -> exitTime.isNotBlank() && exitValid
        PunchChoice.NONE -> true
    }

    val occurrenceValid = if (choice == PunchChoice.NONE) {
        occurrence != null
    } else {
        occurrence == null || occurrence in choice.occurrences
    }

    val duration = if (choice == PunchChoice.BOTH) {
        WorkStatsCalculator.durationMinutes(entryTime, exitTime)
    } else {
        null
    }

    val canSave = dateValid && timesValid && occurrenceValid

    fun selectChoice(next: PunchChoice) {
        val currentNow = LocalTime.now().format(timeDisplayFormatter)
        if (next == PunchChoice.ENTRY && entryField.text.isBlank()) {
            val fill = exitField.text.ifBlank { currentNow }
            entryField = TextFieldValue(fill, TextRange(fill.length))
        }
        if (next == PunchChoice.EXIT && exitField.text.isBlank()) {
            val fill = entryField.text.ifBlank { currentNow }
            exitField = TextFieldValue(fill, TextRange(fill.length))
        }
        if (next == PunchChoice.BOTH) {
            if (entryField.text.isBlank()) {
                entryField = TextFieldValue("08:00", TextRange(5))
            }
            if (exitField.text.isBlank()) {
                exitField = TextFieldValue(currentNow, TextRange(currentNow.length))
            }
        }
        choice = next
        occurrence = occurrence?.takeIf { it in next.occurrences }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Lançar ponto manual",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Registre seus horários diretamente no aplicativo sem necessidade de foto.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Campo de Data
            OutlinedTextField(
                value = workDateField,
                onValueChange = { workDateField = maskedFieldValue(it, ::formatDateInput) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Data do ponto") },
                placeholder = { Text("dd/mm/aaaa") },
                trailingIcon = {
                    TextButton(onClick = {
                        val currentToday = LocalDate.now().format(dateDisplayFormatter)
                        workDateField = TextFieldValue(currentToday, TextRange(currentToday.length))
                    }) {
                        Text("Hoje")
                    }
                },
                supportingText = {
                    Text(
                        when {
                            workDate.isBlank() -> "Informe o dia do registro"
                            !dateValid -> "Data inválida (formato dd/mm/aaaa)"
                            else -> "Data confirmada"
                        }
                    )
                },
                isError = workDate.isNotBlank() && !dateValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Seleção de Tipo
            Text(
                "Tipo de registro",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PunchChoice.entries) { option ->
                    FilterChip(
                        selected = choice == option,
                        onClick = { selectChoice(option) },
                        label = { Text(option.label) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Horários
            when (choice) {
                PunchChoice.ENTRY -> {
                    TimeInputWithNow(label = "Horário de Entrada", field = entryField, isValid = entryValid) {
                        entryField = it
                    }
                }
                PunchChoice.EXIT -> {
                    TimeInputWithNow(label = "Horário de Saída", field = exitField, isValid = exitValid) {
                        exitField = it
                    }
                }
                PunchChoice.BOTH -> {
                    TimeInputWithNow(label = "Horário de Entrada", field = entryField, isValid = entryValid) {
                        entryField = it
                    }
                    TimeInputWithNow(label = "Horário de Saída", field = exitField, isValid = exitValid) {
                        exitField = it
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val prev = entryField
                                entryField = exitField
                                exitField = prev
                            },
                            enabled = entryTime.isNotBlank() || exitTime.isNotBlank()
                        ) {
                            Icon(Icons.Outlined.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Inverter")
                        }
                        if (duration != null) {
                            Text(
                                "Total: ${formatMinutes(duration)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                PunchChoice.NONE -> {
                    Text(
                        "Motivo da ausência ou ocorrência",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PunchChoice.NONE.occurrences.toList()) { option ->
                            FilterChip(
                                selected = occurrence == option,
                                onClick = { occurrence = option },
                                label = { Text(occurrenceLabel(option)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Campo de Observação / Anotação
            OutlinedTextField(
                value = noteField,
                onValueChange = { noteField = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Observação ou justificativa (opcional)") },
                placeholder = { Text("Ex.: Home office, esqueceu crachá, etc.") }
            )

            // Botão Salvar
            Button(
                onClick = {
                    if (canSave) {
                        onSave(
                            workDate,
                            entryTime.takeIf { choice == PunchChoice.ENTRY || choice == PunchChoice.BOTH },
                            exitTime.takeIf { choice == PunchChoice.EXIT || choice == PunchChoice.BOTH },
                            occurrence,
                            noteField.ifBlank { "Registro manual" }
                        )
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Confirmar e Salvar Ponto", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TimeInputWithNow(
    label: String,
    field: TextFieldValue,
    isValid: Boolean,
    onValueChange: (TextFieldValue) -> Unit
) {
    OutlinedTextField(
        value = field,
        onValueChange = { onValueChange(maskedFieldValue(it, ::formatTimeInput)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text("hh:mm") },
        trailingIcon = {
            TextButton(onClick = {
                val current = LocalTime.now().format(timeDisplayFormatter)
                onValueChange(TextFieldValue(current, TextRange(current.length)))
            }) {
                Text("Agora")
            }
        },
        supportingText = {
            Text(if (!isValid && field.text.isNotBlank()) "Horário inválido (formato hh:mm)" else "Ex.: 08:00")
        },
        isError = field.text.isNotBlank() && !isValid,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

private fun occurrenceLabel(occurrence: DayOccurrence): String = when (occurrence) {
    DayOccurrence.MEDICAL_LEAVE -> "Atestado médico"
    DayOccurrence.DAY_OFF -> "Folga / DSR"
    DayOccurrence.ABSENCE -> "Falta integral"
    DayOccurrence.FORGOT_PUNCH -> "Esquecimento de batida"
}
