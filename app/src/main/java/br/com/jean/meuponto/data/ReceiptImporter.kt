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

internal fun copyToPrivateStorage(context: Context, uri: Uri): File {
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    val file = File(dir, "comprovante_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri).use { input -> file.outputStream().use { output -> input?.copyTo(output) ?: error("Arquivo vazio") } }
    return file
}

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
    val entry = times.getOrNull(0)
    val exit = times.getOrNull(1)
    val needsReview = date == null || times.isEmpty()

    dao.insert(
        Receipt(
            capturedAt = System.currentTimeMillis(),
            workDate = date ?: "",
            workTime = entry ?: exit ?: "",
            imagePath = file.absolutePath,
            recognizedText = text,
            entryTime = entry,
            exitTime = exit,
            needsReview = needsReview
        )
    )

    return when {
        date == null && times.isEmpty() -> "Comprovante salvo — revise data e horário."
        date == null -> "Comprovante salvo — revise a data."
        times.isEmpty() -> "Comprovante salvo — revise o horário."
        else -> "Comprovante salvo — confira a data $date."
    }
}

internal fun decodePreview(path: String): Bitmap? {
    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
    return BitmapFactory.decodeFile(path, options)
}
