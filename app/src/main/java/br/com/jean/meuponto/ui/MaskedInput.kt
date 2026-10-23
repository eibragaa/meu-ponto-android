package br.com.jean.meuponto.ui


import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import br.com.jean.meuponto.domain.WorkStatsCalculator

/**
 * Máscaras de entrada e validação de data/hora dos campos do editor.
 */

/** Data digitada é aceita só se for uma data real (29/02, mês 13 e afins são recusados). */
internal fun isValidWorkDate(value: String): Boolean =
    WorkStatsCalculator.parseWorkDate(value) != null

/**
 * Máscara de data: mantém só dígitos, limita a 8 e insere as barras (`dd/MM/yyyy`).
 * Funciona com entrada parcial (`"2"` -> `"2"`, `"2010"` -> `"20/10"`).
 */
internal fun formatDateInput(value: String): String {
    val digits = value.filter { it.isDigit() }.take(8)
    val parts = listOf(digits.take(2), digits.drop(2).take(2), digits.drop(4).take(4))
        .filter { it.isNotEmpty() }
    return parts.joinToString("/")
}

/** Máscara de hora: mantém só dígitos, limita a 4 e insere os dois-pontos (`HH:mm`). */
internal fun formatTimeInput(value: String): String {
    val digits = value.filter { it.isDigit() }.take(4)
    return if (digits.length > 2) "${digits.take(2)}:${digits.drop(2)}" else digits
}

/**
 * Aplica a máscara e devolve o cursor **no fim** do texto.
 *
 * Isto existe por causa do bug P18: com `String` puro, o `TextField` reposicionava o cursor no
 * início a cada caractere e o usuário digitava `2243` esperando `22:43` e recebia `22:34`.
 * Como a máscara sempre reescreve o fim da string, o cursor no fim é o comportamento correto.
 */
internal fun maskedFieldValue(input: TextFieldValue, format: (String) -> String): TextFieldValue {
    val formatted = format(input.text)
    return TextFieldValue(formatted, TextRange(formatted.length))
}
