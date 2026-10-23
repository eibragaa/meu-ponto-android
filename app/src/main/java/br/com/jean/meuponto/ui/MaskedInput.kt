package br.com.jean.meuponto.ui


import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import br.com.jean.meuponto.domain.WorkStatsCalculator

/**
 * Máscaras de entrada e validação de data/hora dos campos do editor.
 */

internal fun isValidWorkDate(value: String): Boolean =
    WorkStatsCalculator.parseWorkDate(value) != null

internal fun formatDateInput(value: String): String {
    val digits = value.filter { it.isDigit() }.take(8)
    val parts = listOf(digits.take(2), digits.drop(2).take(2), digits.drop(4).take(4))
        .filter { it.isNotEmpty() }
    return parts.joinToString("/")
}

internal fun formatTimeInput(value: String): String {
    val digits = value.filter { it.isDigit() }.take(4)
    return if (digits.length > 2) "${digits.take(2)}:${digits.drop(2)}" else digits
}

internal fun maskedFieldValue(input: TextFieldValue, format: (String) -> String): TextFieldValue {
    val formatted = format(input.text)
    return TextFieldValue(formatted, TextRange(formatted.length))
}
