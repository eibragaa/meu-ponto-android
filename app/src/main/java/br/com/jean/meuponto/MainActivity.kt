package br.com.jean.meuponto

import android.content.ContentValues
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import br.com.jean.meuponto.data.AppDatabase
import br.com.jean.meuponto.data.Receipt
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MeuPontoApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeuPontoApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).receiptDao() }
    var query by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Seus comprovantes ficam neste aparelho até você configurar o backup.") }
    val receipts by (if (query.isBlank()) dao.observeAll() else dao.search(query)).collectAsState(initial = emptyList())

    val importReceipt = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    message = "Lendo comprovante…"
                    val savedFile = withContext(Dispatchers.IO) { copyToPrivateStorage(context, uri) }
                    val text = withContext(Dispatchers.IO) {
                        val image = InputImage.fromFilePath(context, Uri.fromFile(savedFile))
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        try { recognizer.process(image).await().text } finally { recognizer.close() }
                    }
                    val dateMatch = Regex("\\b(\\d{2})[./-](\\d{2})[./-](\\d{4})\\b").find(text)
                    val timeMatch = Regex("\\b([01]?\\d|2[0-3]):[0-5]\\d\\b").find(text)
                    val date = dateMatch?.value?.replace('.', '/')?.replace('-', '/') ?: SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
                    val time = timeMatch?.value ?: ""
                    dao.insert(Receipt(capturedAt = System.currentTimeMillis(), workDate = date, workTime = time, imagePath = savedFile.absolutePath, recognizedText = text))
                    message = if (dateMatch != null) "Comprovante salvo. Confira a data reconhecida: $date" else "Comprovante salvo. A data não foi identificada; pesquise pelo texto e confira manualmente."
                } catch (e: Exception) {
                    message = "Não foi possível importar este arquivo: ${e.localizedMessage ?: "erro desconhecido"}"
                }
            }
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF176B53))) {
        Scaffold(topBar = {
            TopAppBar(title = {
                Column {
                    Text("Meu Ponto", fontWeight = FontWeight.Bold)
                    Text("Arquivo pessoal de comprovantes", style = MaterialTheme.typography.labelSmall)
                }
            })
        }, floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { importReceipt.launch(arrayOf("image/*")) }, icon = { Icon(Icons.Default.Description, null) }, text = { Text("Adicionar comprovante") })
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(Modifier.weight(1f)) { Column(Modifier.padding(14.dp)) { Text("Comprovantes", style = MaterialTheme.typography.labelMedium); Text(receipts.size.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) } }
                    Card(Modifier.weight(1f)) { Column(Modifier.padding(14.dp)) { Text("Backup", style = MaterialTheme.typography.labelMedium); Text("Não configurado", style = MaterialTheme.typography.titleMedium) } }
                }
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Buscar data, horário ou texto…") }, leadingIcon = { Icon(Icons.Default.Search, null) })
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                if (receipts.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Description, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text("Nenhum comprovante encontrado", fontWeight = FontWeight.SemiBold)
                            Text("Adicione uma foto ou imagem do comprovante.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                        items(receipts, key = { it.id }) { receipt ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(receipt.workDate, fontWeight = FontWeight.SemiBold)
                                        Text(if (receipt.workTime.isBlank()) "Horário não identificado" else "Horário: ${receipt.workTime}", style = MaterialTheme.typography.bodySmall)
                                        Text(File(receipt.imagePath).name, style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(onClick = { shareAsPdf(context, receipt) }) { Icon(Icons.Default.Share, contentDescription = "Compartilhar PDF") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copyToPrivateStorage(context: android.content.Context, uri: Uri): File {
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    val file = File(dir, "comprovante_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri).use { input -> file.outputStream().use { output -> input?.copyTo(output) ?: error("Arquivo vazio") } }
    return file
}

private fun shareAsPdf(context: android.content.Context, receipt: Receipt) {
    try {
        val bitmap = BitmapFactory.decodeFile(receipt.imagePath) ?: error("Imagem indisponível")
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width.coerceAtLeast(1), bitmap.height.coerceAtLeast(1), 1).create()
        val page = pdf.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdf.finishPage(page)
        val outDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val outFile = File(outDir, "comprovante_${receipt.workDate.replace('/', '-')}.pdf")
        outFile.outputStream().use { pdf.writeTo(it) }
        pdf.close()
        val contentValues = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, outFile.name) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", outFile)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar comprovante"))
    } catch (e: Exception) {
        Toast.makeText(context, "Falha ao gerar PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
