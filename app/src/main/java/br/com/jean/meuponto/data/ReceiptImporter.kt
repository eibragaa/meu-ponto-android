package br.com.jean.meuponto.data


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import br.com.jean.meuponto.domain.WorkStatsCalculator
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Importação de comprovante: cópia para armazenamento privado e OCR local (ML Kit).
 */

/**
 * Copia a imagem escolhida para `filesDir/receipts` com nome próprio.
 *
 * O app nunca guarda a `Uri` original porque permissões de SAF expiram; o nome é baseado em
 * `currentTimeMillis` e não no nome de origem (que pode vir com path traversal).
 */
internal fun copyToPrivateStorage(context: Context, uri: Uri): File {
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    val file = File(dir, "comprovante_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri).use { input -> file.outputStream().use { output -> input?.copyTo(output) ?: error("Arquivo vazio") } }
    return file
}

/**
 * Roda o OCR local (ML Kit, sem rede) sobre a imagem e grava o comprovante.
 *
 * Heurística de extração, deliberadamente simples:
 * - data: primeira ocorrência de `dd/MM/yyyy` (ou `dd.MM.yyyy`/`dd-MM-yyyy`) válida;
 * - horários: todas as ocorrências de `HH:mm` válidas, em ordem de leitura. Dois ou mais viram
 *   entrada e saída, nessa ordem (ver `WorkStatsCalculator.ocrPunches`); um só fica sem tipo, porque
 *   o OCR não sabe se é entrada ou saída.
 *
 * O que não for reconhecido, ou o que não tiver tipo, fica com `needsReview = true` para o usuário
 * corrigir na UI. A mensagem devolvida é o texto do snackbar — por isso descreve o que faltou, não
 * "erro".
 */
internal suspend fun recognizeAndSave(context: Context, dao: ReceiptDao, file: File): String {
    val text = withContext(Dispatchers.IO) {
        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            recognizer.process(image).await().text
        } finally {
            recognizer.close()
        }
    }
    val date = WorkStatsCalculator.normalizeWorkDate(text)
    val times = Regex("\\b([01]?\\d|2[0-3]):[0-5]\\d\\b")
        .findAll(text)
        .mapNotNull { WorkStatsCalculator.normalizeClockTime(it.value) }
        .toList()
    val punches = WorkStatsCalculator.ocrPunches(times)
    // Um horário só não diz se é entrada ou saída: fica para o usuário escolher na edição.
    val needsReview = date == null || times.size < 2

    dao.insert(
        Receipt(
            capturedAt = System.currentTimeMillis(),
            workDate = date ?: "",
            workTime = punches.workTime,
            imagePath = file.absolutePath,
            recognizedText = text,
            entryTime = punches.entry,
            exitTime = punches.exit,
            needsReview = needsReview
        )
    )

    return when {
        date == null && times.isEmpty() -> "Comprovante salvo — revise data e horário."
        date == null -> "Comprovante salvo — revise a data."
        times.isEmpty() -> "Comprovante salvo — revise o horário."
        times.size == 1 -> "Comprovante salvo — diga se o horário é entrada ou saída."
        else -> "Comprovante salvo — confira a data $date."
    }
}

/** Miniatura para a lista: reduz por 4 na decodificação, o suficiente para 56dp sem estourar memória. */
internal fun decodePreview(path: String): Bitmap? {
    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
    return BitmapFactory.decodeFile(path, options)
}
