package br.com.jean.meuponto.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.jean.meuponto.data.Receipt
import br.com.jean.meuponto.domain.WorkStatsCalculator
import br.com.jean.meuponto.domain.formatMinutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReceiptViewer(
    receipt: Receipt,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "Fechar")
                    }
                    IconButton(onClick = onPrevious, enabled = hasPrevious) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Anterior")
                    }
                    IconButton(onClick = onNext, enabled = hasNext) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Próximo")
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Excluir comprovante")
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0B1116))
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = receipt.id, key2 = receipt.imagePath) {
                        value = withContext(Dispatchers.IO) { decodeViewerBitmap(receipt.imagePath) }
                    }
                    val image = bitmap
                    if (image != null) {
                        var scale by remember(receipt.id) { mutableFloatStateOf(1f) }
                        var offset by remember(receipt.id) { mutableStateOf(Offset.Zero) }
                        Image(
                            bitmap = image.asImageBitmap(),
                            contentDescription = "Comprovante",
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(receipt.id) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 6f)
                                        if (scale > 1f) {
                                            offset += pan
                                        } else {
                                            offset = Offset.Zero
                                        }
                                    }
                                }
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = .7f),
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Imagem indisponível",
                                color = Color.White.copy(alpha = .7f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        receipt.workDate.ifBlank { "Data pendente" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        timeSummary(receipt),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (receipt.needsReview) {
                        Text(
                            "Revisar data ou horário reconhecido pelo OCR.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (receipt.recognizedText.isNotBlank()) {
                        Text(
                            receipt.recognizedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Editar")
                        }
                        OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Compartilhar")
                        }
                    }
                }
            }
        }
    }
}

private fun timeSummary(receipt: Receipt): String {
    val duration = WorkStatsCalculator.durationMinutes(receipt)
    return when {
        receipt.entryTime != null && receipt.exitTime != null ->
            "Entrada ${receipt.entryTime} · Saída ${receipt.exitTime}" +
                (duration?.let { " · ${formatMinutes(it)}" } ?: "")
        receipt.entryTime != null -> "Entrada ${receipt.entryTime} · saída pendente"
        receipt.exitTime != null -> "Saída ${receipt.exitTime} · entrada pendente"
        receipt.workTime.isNotBlank() -> "Horário identificado: ${receipt.workTime}"
        else -> "Horário pendente"
    }
}

private fun decodeViewerBitmap(path: String): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > 2200 || bounds.outHeight / sample > 2200) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}
