package br.com.jean.meuponto.domain


import br.com.jean.meuponto.data.Receipt

/**
 * Rótulos de exibição derivados de um comprovante (usados por UI e exportação de PDF).
 */

internal fun receiptTimeLabel(receipt: Receipt): String = when {
    !receipt.entryTime.isNullOrBlank() && !receipt.exitTime.isNullOrBlank() ->
        "Entrada ${receipt.entryTime} · Saída ${receipt.exitTime}"
    !receipt.entryTime.isNullOrBlank() -> "Entrada ${receipt.entryTime} · saída pendente"
    !receipt.exitTime.isNullOrBlank() -> "Saída ${receipt.exitTime} · entrada pendente"
    receipt.workTime.isNotBlank() -> "Horário ${receipt.workTime}"
    else -> "Horário pendente"
}
