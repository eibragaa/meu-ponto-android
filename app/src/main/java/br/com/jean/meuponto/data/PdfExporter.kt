package br.com.jean.meuponto.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import br.com.jean.meuponto.domain.WorkStatsCalculator
import br.com.jean.meuponto.domain.receiptTimeLabel
import br.com.jean.meuponto.domain.formatMinutes
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exportação de comprovantes em PDF para compartilhamento.
 */

internal fun shareAsPdf(context: Context, receipts: List<Receipt>) {
    if (receipts.isEmpty()) return
    try {
        val pdf = PdfDocument()
        if (receipts.size == 1) {
            val receipt = receipts.first()
            val bitmap = decodePdfBitmap(receipt.imagePath) ?: error("Imagem indisponível")
            val footerHeight = 96
            val pageInfo = PdfDocument.PageInfo.Builder(
                bitmap.width.coerceAtLeast(1),
                bitmap.height.coerceAtLeast(1) + footerHeight,
                1
            ).create()
            val page = pdf.startPage(pageInfo)
            page.canvas.drawColor(android.graphics.Color.WHITE)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            drawPdfReceiptInfo(
                canvas = page.canvas,
                receipt = receipt,
                left = 24f,
                baseline = bitmap.height + 34f,
                textSize = 24f,
                includeFooter = true
            )
            pdf.finishPage(page)
            bitmap.recycle()
        } else {
            val pageWidth = 1200f
            val pageHeight = 1697f
            val margin = 36f
            val gap = 24f
            val slotHeight = (pageHeight - (margin * 2) - gap) / 2f
            val infoHeight = 48f
            receipts.chunked(2).forEachIndexed { pageIndex, pageReceipts ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageIndex + 1).create()
                val page = pdf.startPage(pageInfo)
                page.canvas.drawColor(android.graphics.Color.WHITE)
                pageReceipts.forEachIndexed { slotIndex, receipt ->
                    val bitmap = decodePdfBitmap(receipt.imagePath) ?: error("Imagem indisponível")
                    val top = margin + (slotIndex * (slotHeight + gap))
                    val target = fitBitmapRect(bitmap, margin, top, pageWidth - margin, top + slotHeight - infoHeight)
                    page.canvas.drawBitmap(bitmap, null, target, null)
                    drawPdfReceiptInfo(
                        canvas = page.canvas,
                        receipt = receipt,
                        left = margin,
                        baseline = top + slotHeight - 18f,
                        textSize = 20f,
                        includeFooter = false
                    )
                    bitmap.recycle()
                }
                pdf.finishPage(page)
            }
        }
        val outDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val fileName = if (receipts.size == 1) {
            "comprovante_${receipts.first().workDate.replace('/', '-')}.pdf"
        } else {
            "meu-ponto_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.pdf"
        }
        val outFile = File(outDir, fileName)
        try {
            outFile.outputStream().use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", outFile)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserTitle = if (receipts.size == 1) "Compartilhar comprovante" else "Compartilhar comprovantes"
        context.startActivity(android.content.Intent.createChooser(intent, chooserTitle))
    } catch (error: Exception) {
        Toast.makeText(context, "Falha ao gerar PDF", Toast.LENGTH_LONG).show()
    }
}

private fun drawPdfReceiptInfo(
    canvas: Canvas,
    receipt: Receipt,
    left: Float,
    baseline: Float,
    textSize: Float,
    includeFooter: Boolean
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.DKGRAY
        this.textSize = textSize
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    val duration = WorkStatsCalculator.durationMinutes(receipt)?.let { " · ${formatMinutes(it)}" }.orEmpty()
    val review = if (receipt.needsReview) " · Revisar" else ""
    canvas.drawText(
        "${receipt.workDate.ifBlank { "Data pendente" }} · ${receiptTimeLabel(receipt)}$duration$review",
        left,
        baseline,
        paint
    )
    if (includeFooter) {
        paint.textSize = textSize * .78f
        paint.color = android.graphics.Color.GRAY
        canvas.drawText("Meu Ponto · arquivo local", left, baseline + textSize + 10f, paint)
    }
}

private fun fitBitmapRect(bitmap: Bitmap, left: Float, top: Float, right: Float, bottom: Float): RectF {
    val scale = minOf((right - left) / bitmap.width, (bottom - top) / bitmap.height)
    val width = bitmap.width * scale
    val height = bitmap.height * scale
    val offsetX = left + ((right - left) - width) / 2f
    val offsetY = top + ((bottom - top) - height) / 2f
    return RectF(offsetX, offsetY, offsetX + width, offsetY + height)
}

private fun decodePdfBitmap(path: String): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > 2200 || bounds.outHeight / sample > 2200) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}
