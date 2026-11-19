package br.com.jean.meuponto.data


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import br.com.jean.meuponto.domain.PunchChoice
import br.com.jean.meuponto.domain.WorkStatsCalculator
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.UUID
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
    val file = File(dir, "comprovante_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
    context.contentResolver.openInputStream(uri).use { input -> file.outputStream().use { output -> input?.copyTo(output) ?: error("Arquivo vazio") } }
    return file
}

/**
 * Roda o OCR local (ML Kit, sem rede) sobre a imagem e grava o comprovante.
 *
 * A extração e consolidação moram em [WorkStatsCalculator.resolveOcrReceipt]:
 * - data: primeira ocorrência de `dd/MM/yyyy` válida; se não houver data mas houver horário válido,
 *   assume a data de hoje para não travar o fluxo de cadastro em 3 cliques;
 * - horários: `HH:mm` em ordem de leitura (com tipo imediato quando [preferredPunch] foi escolhido).
 *
 * O que não for reconhecido, ou o que não tiver tipo, fica com `needsReview = true` para o usuário
 * corrigir na UI. A mensagem devolvida é o texto do snackbar — por isso descreve o que faltou, não
 * "erro".
 */
internal suspend fun recognizeAndSave(
    context: Context,
    dao: ReceiptDao,
    file: File,
    preferredPunch: PunchChoice? = null
): String {
    val text = withContext(Dispatchers.IO) {
        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            recognizer.process(image).await().text
        } finally {
            recognizer.close()
        }
    }
    val resolved = WorkStatsCalculator.resolveOcrReceipt(text, preferredPunch)

    dao.insert(
        Receipt(
            capturedAt = System.currentTimeMillis(),
            workDate = resolved.workDate,
            workTime = resolved.workTime,
            imagePath = file.absolutePath,
            recognizedText = text,
            entryTime = resolved.entryTime,
            exitTime = resolved.exitTime,
            needsReview = resolved.needsReview
        )
    )

    return when {
        resolved.workDate.isBlank() && resolved.workTime.isBlank() ->
            "Comprovante salvo — revise data e horário."
        resolved.workTime.isBlank() ->
            "Comprovante salvo — revise o horário."
        resolved.workDate.isBlank() ->
            "Comprovante salvo — revise a data."
        resolved.needsReview && resolved.usedTodayFallback ->
            "Comprovante salvo com a data de hoje — diga se o horário é entrada ou saída."
        resolved.needsReview ->
            "Comprovante salvo — diga se o horário é entrada ou saída."
        resolved.usedTodayFallback ->
            "Comprovante salvo com a data de hoje (${resolved.workDate})."
        else ->
            "Comprovante salvo — confira a data ${resolved.workDate}."
    }
}

/** Miniatura para a lista: reduz por 4 na decodificação, o suficiente para 56dp sem estourar memória. */
internal fun decodePreview(path: String): Bitmap? {
    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
    return BitmapFactory.decodeFile(path, options)
}
