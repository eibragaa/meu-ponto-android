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
import br.com.jean.meuponto.domain.DayOccurrence
import br.com.jean.meuponto.domain.Journey
import br.com.jean.meuponto.domain.PayrollSettings
import br.com.jean.meuponto.domain.WorkStatsCalculator
import br.com.jean.meuponto.domain.formatMinutes
import br.com.jean.meuponto.domain.journeys
import br.com.jean.meuponto.domain.receiptTimeLabel
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Exportação de comprovantes e relatórios em PDF para compartilhamento e entrega ao RH.
 */

/**
 * Monta um PDF com os canhotos/comprovantes (uma página por comprovante, ou grade de 2 por página)
 * e abre o compartilhamento.
 *
 * Registros manuais sem foto são desenhados como cartões digitais vetoriais próprios, sem falhas.
 */
internal fun shareAsPdf(context: Context, receipts: List<Receipt>) {
    if (receipts.isEmpty()) return
    try {
        val pdf = PdfDocument()
        if (receipts.size == 1) {
            val receipt = receipts.first()
            val bitmap = decodePdfBitmap(receipt.imagePath) ?: createManualReceiptBitmap(receipt)
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
                    val bitmap = decodePdfBitmap(receipt.imagePath) ?: createManualReceiptBitmap(receipt)
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
            "canhotos_meu-ponto_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.pdf"
        }
        val outFile = File(outDir, fileName)
        try {
            outFile.outputStream().use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
        dispatchPdfShare(context, outFile, if (receipts.size == 1) "Compartilhar comprovante" else "Compartilhar canhotos")
    } catch (error: Exception) {
        Toast.makeText(context, "Falha ao gerar PDF de canhotos", Toast.LENGTH_LONG).show()
    }
}

/**
 * Gera e compartilha o Espelho de Ponto Folha Mensal consolidado em tabela padrão A4 (formato CLT).
 *
 * Contém colunas: Data, Dia da Semana, Entrada 1, Saída 1, Entrada 2, Saída 2, Total do Dia e
 * Ocorrência/Saldo, com cabeçalho de apuração e campo formal para assinatura do trabalhador.
 */
internal fun shareTimesheetPdf(
    context: Context,
    receipts: List<Receipt>,
    payrollSettings: PayrollSettings? = null
) {
    if (receipts.isEmpty()) return
    try {
        val pdf = PdfDocument()
        val pageWidth = 1200
        val pageHeight = 1697
        val marginX = 48f
        val marginY = 48f
        val contentWidth = pageWidth - (marginX * 2)

        // Agrupa comprovantes por dia, respeitando o período de apuração quando configurado.
        val periodStartDate = payrollSettings?.periodStart
        val periodEndDate = payrollSettings?.periodEnd
        val scopedReceipts = if (periodStartDate != null && periodEndDate != null) {
            receipts.filter { receipt ->
                val day = WorkStatsCalculator.parseWorkDate(receipt.workDate)
                day == null || (!day.isBefore(periodStartDate) && !day.isAfter(periodEndDate))
            }
        } else {
            receipts
        }
        val journeyMap = WorkStatsCalculator.journeys(scopedReceipts)
        val dayStatsMap = WorkStatsCalculator.dayStats(scopedReceipts)

        // Determina datas do período ordenadas
        val sortedDates = journeyMap.keys
            .filter { it.isNotBlank() && it != "Revisar" }
            .mapNotNull { WorkStatsCalculator.parseWorkDate(it) }
            .sorted()

        val periodStartStr = payrollSettings?.periodStart?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            ?: sortedDates.firstOrNull()?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            ?: "Início"
        val periodEndStr = payrollSettings?.periodEnd?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            ?: sortedDates.lastOrNull()?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            ?: "Fim"

        val totalMinutesWorked = dayStatsMap.values.sumOf { it.minutes }
        val daysWithWork = dayStatsMap.values.count { it.minutes > 0 }

        val rowsPerPage = 28
        val chunks = if (sortedDates.isEmpty()) listOf(emptyList()) else sortedDates.chunked(rowsPerPage)
        val totalPages = chunks.size

        chunks.forEachIndexed { pageIndex, pageDates ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(android.graphics.Color.WHITE)

            var currentY = marginY

            // Cabeçalho da Folha (apenas na página 1 de forma expandida)
            if (pageIndex == 0) {
                currentY = drawTimesheetHeader(
                    canvas = canvas,
                    left = marginX,
                    top = currentY,
                    width = contentWidth,
                    periodStart = periodStartStr,
                    periodEnd = periodEndStr,
                    totalDays = daysWithWork,
                    totalMinutes = totalMinutesWorked,
                    expectedDailyMinutes = payrollSettings?.expectedDailyMinutes
                )
            } else {
                val miniHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 20f
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                }
                canvas.drawText("MEU PONTO • ESPELHO DE PONTO (Continuação)", marginX, currentY + 24f, miniHeaderPaint)
                currentY += 44f
            }

            // Tabela de Batidas
            currentY = drawTimesheetTable(
                canvas = canvas,
                left = marginX,
                top = currentY,
                width = contentWidth,
                dates = pageDates,
                journeyMap = journeyMap,
                dayStatsMap = dayStatsMap,
                expectedDailyMinutes = payrollSettings?.expectedDailyMinutes
            )

            // Rodapé com assinatura e observações na última página
            if (pageIndex == totalPages - 1) {
                drawTimesheetFooter(
                    canvas = canvas,
                    left = marginX,
                    bottom = pageHeight - marginY,
                    width = contentWidth,
                    totalMinutes = totalMinutesWorked
                )
            }

            // Numeração de página
            val pageNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.GRAY
                textSize = 14f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Página ${pageIndex + 1} de $totalPages", pageWidth - marginX, pageHeight - 20f, pageNumPaint)

            pdf.finishPage(page)
        }

        val outDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val fileName = "espelho_ponto_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.pdf"
        val outFile = File(outDir, fileName)
        try {
            outFile.outputStream().use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }

        dispatchPdfShare(context, outFile, "Compartilhar Espelho de Ponto")
    } catch (error: Exception) {
        Toast.makeText(context, "Falha ao gerar espelho de ponto", Toast.LENGTH_LONG).show()
    }
}

private fun drawTimesheetHeader(
    canvas: Canvas,
    left: Float,
    top: Float,
    width: Float,
    periodStart: String,
    periodEnd: String,
    totalDays: Int,
    totalMinutes: Int,
    expectedDailyMinutes: Int?
): Float {
    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(248, 250, 252)
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(203, 213, 225)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(15, 23, 42)
        textSize = 28f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(13, 140, 70)
        textSize = 16f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(100, 116, 139)
        textSize = 15f
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(15, 23, 42)
        textSize = 16f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    val headerHeight = 150f
    val rect = RectF(left, top, left + width, top + headerHeight)
    canvas.drawRoundRect(rect, 16f, 16f, boxPaint)
    canvas.drawRoundRect(rect, 16f, 16f, borderPaint)

    canvas.drawText("MEU PONTO • REGISTRO ELETRÔNICO INDIVIDUAL", left + 24f, top + 34f, brandPaint)
    canvas.drawText("Espelho de Ponto • Folha Mensal", left + 24f, top + 70f, titlePaint)

    val metaInfoY = top + 115f
    canvas.drawText("Período:", left + 24f, metaInfoY, labelPaint)
    canvas.drawText("$periodStart a $periodEnd", left + 85f, metaInfoY, valuePaint)

    canvas.drawText("Dias com ponto:", left + 360f, metaInfoY, labelPaint)
    canvas.drawText("$totalDays dias", left + 475f, metaInfoY, valuePaint)

    canvas.drawText("Total apurado:", left + 620f, metaInfoY, labelPaint)
    canvas.drawText(formatMinutes(totalMinutes), left + 725f, metaInfoY, valuePaint)

    if (expectedDailyMinutes != null && expectedDailyMinutes > 0) {
        val expectedTotal = totalDays * expectedDailyMinutes
        val balance = totalMinutes - expectedTotal
        val sign = if (balance >= 0) "+" else ""
        canvas.drawText("Saldo do período:", left + 860f, metaInfoY, labelPaint)
        val balancePaint = Paint(valuePaint).apply {
            color = if (balance >= 0) android.graphics.Color.rgb(13, 140, 70) else android.graphics.Color.rgb(220, 38, 38)
        }
        canvas.drawText("$sign${formatMinutes(balance)}", left + 985f, metaInfoY, balancePaint)
    }

    return top + headerHeight + 20f
}

private fun drawTimesheetTable(
    canvas: Canvas,
    left: Float,
    top: Float,
    width: Float,
    dates: List<LocalDate>,
    journeyMap: Map<String, List<Journey>>,
    dayStatsMap: Map<String, br.com.jean.meuponto.domain.WorkDayStats>,
    expectedDailyMinutes: Int?
): Float {
    val colWidths = floatArrayOf(130f, 90f, 115f, 115f, 115f, 115f, 130f, width - (130f + 90f + 460f + 130f))
    val rowHeight = 36f

    val headerBgPaint = Paint().apply { color = android.graphics.Color.rgb(241, 245, 249) }
    val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(51, 65, 85)
        textSize = 15f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    val gridPaint = Paint().apply {
        color = android.graphics.Color.rgb(226, 232, 240)
        strokeWidth = 1.2f
    }
    val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(30, 41, 59)
        textSize = 15f
    }
    val cellBoldTextPaint = Paint(cellTextPaint).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    // Linha do cabeçalho
    canvas.drawRect(left, top, left + width, top + rowHeight, headerBgPaint)
    canvas.drawLine(left, top + rowHeight, left + width, top + rowHeight, gridPaint)

    val headers = arrayOf("Data", "Semana", "Entr. 1", "Saída 1", "Entr. 2", "Saída 2", "Total", "Observações")
    var colX = left
    headers.forEachIndexed { i, title ->
        canvas.drawText(title, colX + 10f, top + 24f, headerTextPaint)
        colX += colWidths[i]
    }

    var currentY = top + rowHeight

    val dayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val dowFormatter = DateTimeFormatter.ofPattern("EEE", Locale("pt", "BR"))

    dates.forEachIndexed { rowIndex, date ->
        val dateStr = date.format(dayFormatter)
        val dowStr = date.format(dowFormatter).replace(".", "").replaceFirstChar { it.uppercase() }
        val journeys = journeyMap[dateStr].orEmpty()
        val stats = dayStatsMap[dateStr]

        // Coleta até 4 batidas nos turnos do dia
        val punches = mutableListOf<String>()
        journeys.forEach { j ->
            j.entryTime?.let { punches.add(it) }
            j.exitTime?.let { punches.add(it) }
        }

        val e1 = punches.getOrNull(0) ?: "—"
        val s1 = punches.getOrNull(1) ?: "—"
        val e2 = punches.getOrNull(2) ?: "—"
        val s2 = punches.getOrNull(3) ?: "—"
        val totalMinutes = stats?.minutes ?: 0
        val totalStr = if (totalMinutes > 0) formatMinutes(totalMinutes) else "—"

        val occurrences = stats?.occurrences.orEmpty()
        val obs = when {
            occurrences.contains(DayOccurrence.MEDICAL_LEAVE) -> "Atestado Médico"
            occurrences.contains(DayOccurrence.DAY_OFF) -> "Folga / DSR"
            occurrences.contains(DayOccurrence.ABSENCE) -> "Falta"
            occurrences.contains(DayOccurrence.FORGOT_PUNCH) -> "Esquecimento de batida"
            stats?.issues?.isNotEmpty() == true -> "Pendente de revisão"
            totalMinutes == 0 -> "Sem expediente"
            else -> "Normal"
        }

        // Fundo zebrado sutil
        if (rowIndex % 2 == 1) {
            val zebraPaint = Paint().apply { color = android.graphics.Color.rgb(248, 250, 252) }
            canvas.drawRect(left, currentY, left + width, currentY + rowHeight, zebraPaint)
        }
        canvas.drawLine(left, currentY + rowHeight, left + width, currentY + rowHeight, gridPaint)

        var cellX = left
        val values = arrayOf(dateStr, dowStr, e1, s1, e2, s2, totalStr, obs)
        values.forEachIndexed { colIdx, text ->
            val paint = if (colIdx == 0 || colIdx == 6) cellBoldTextPaint else cellTextPaint
            canvas.drawText(text, cellX + 10f, currentY + 23f, paint)
            cellX += colWidths[colIdx]
        }

        currentY += rowHeight
    }

    return currentY + 20f
}

private fun drawTimesheetFooter(
    canvas: Canvas,
    left: Float,
    bottom: Float,
    width: Float,
    totalMinutes: Int
) {
    val signatureY = bottom - 70f
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(100, 116, 139)
        strokeWidth = 1.5f
    }
    val sigLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(71, 85, 105)
        textSize = 15f
        textAlign = Paint.Align.CENTER
    }
    val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(148, 163, 184)
        textSize = 13f
        textAlign = Paint.Align.CENTER
    }

    val sigWidth = 420f
    val sigLeft = left + (width - sigWidth) / 2f
    val sigCenterX = left + width / 2f

    canvas.drawLine(sigLeft, signatureY, sigLeft + sigWidth, signatureY, linePaint)
    canvas.drawText("Assinatura do Trabalhador / Empregado", sigCenterX, signatureY + 22f, sigLabelPaint)
    canvas.drawText("Reconheço a exatidão das datas e horários registrados neste espelho de ponto.", sigCenterX, signatureY + 42f, notePaint)
}

private fun createManualReceiptBitmap(receipt: Receipt): Bitmap {
    val width = 720
    val height = 440
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(248, 250, 252)
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = android.graphics.Color.rgb(203, 213, 225)
    }
    val rect = RectF(16f, 16f, width - 16f, height - 16f)
    canvas.drawRoundRect(rect, 20f, 20f, cardBgPaint)
    canvas.drawRoundRect(rect, 20f, 20f, borderPaint)

    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(13, 140, 70)
        textSize = 18f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(15, 23, 42)
        textSize = 28f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(51, 65, 85)
        textSize = 22f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    val labelBoldPaint = Paint(textPaint).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    canvas.drawText("MEU PONTO • REGISTRO MANUAL", 44f, 68f, badgePaint)
    canvas.drawText("Comprovante Digital de Ponto", 44f, 115f, titlePaint)

    canvas.drawText("Data de registro:", 44f, 180f, textPaint)
    canvas.drawText(receipt.workDate.ifBlank { "Hoje" }, 250f, 180f, labelBoldPaint)

    canvas.drawText("Horário de ponto:", 44f, 230f, textPaint)
    canvas.drawText(receiptTimeLabel(receipt), 250f, 230f, labelBoldPaint)

    val duration = WorkStatsCalculator.durationMinutes(receipt)
    if (duration != null) {
        canvas.drawText("Tempo apurado:", 44f, 280f, textPaint)
        canvas.drawText(formatMinutes(duration), 250f, 280f, labelBoldPaint)
    }

    val note = receipt.recognizedText.ifBlank { "Registro inserido sem captura fotográfica." }
    canvas.drawText(note.take(55), 44f, 350f, textPaint)

    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(148, 163, 184)
        textSize = 15f
    }
    canvas.drawText("Autenticação e integridade mantidas localmente pelo app Meu Ponto.", 44f, 395f, footerPaint)

    return bitmap
}

private fun dispatchPdfShare(context: Context, file: File, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, title))
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
    if (path.isBlank()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > 2200 || bounds.outHeight / sample > 2200) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}
