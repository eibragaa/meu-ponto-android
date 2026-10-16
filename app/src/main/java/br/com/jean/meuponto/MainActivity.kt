package br.com.jean.meuponto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import br.com.jean.meuponto.data.AppDatabase
import br.com.jean.meuponto.data.Receipt
import br.com.jean.meuponto.data.ReceiptDao
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF112032)
private val InkSoft = Color(0xFF304052)
private val Teal = Color(0xFF157C70)
private val TealDark = Color(0xFF0B5C53)
private val Mint = Color(0xFFD8F1E2)
private val Coral = Color(0xFFFF8A72)
private val Cream = Color(0xFFF8F7F2)
private val Mist = Color(0xFFE9EFEC)
private val Line = Color(0xFFD6E0DB)
private val Muted = Color(0xFF6B7773)

private data class FilterOption(val key: String, val label: String)

private val filters = listOf(
    FilterOption("all", "Todos"),
    FilterOption("today", "Hoje"),
    FilterOption("time", "Com horário")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MeuPontoTheme { MeuPontoApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeuPontoApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).receiptDao() }
    val snackbarHostState = remember { SnackbarHostState() }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("all") }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showBackupSheet by rememberSaveable { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var cameraFile by remember { mutableStateOf<File?>(null) }

    val receipts by (if (query.isBlank()) dao.observeAll() else dao.search(query))
        .collectAsState(initial = emptyList())
    val visibleReceipts = remember(receipts, selectedFilter) {
        when (selectedFilter) {
            "today" -> {
                val today = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
                receipts.filter { it.workDate == today }
            }
            "time" -> receipts.filter { it.workTime.isNotBlank() }
            else -> receipts
        }
    }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun processFile(file: File) {
        scope.launch {
            isProcessing = true
            try {
                val savedFile = withContext(Dispatchers.IO) { copyFileToPrivateStorage(context, file) }
                showMessage(recognizeAndSave(context, dao, savedFile))
            } catch (error: Exception) {
                showMessage("Não foi possível ler o comprovante. Tente novamente.")
            } finally {
                isProcessing = false
                file.delete()
            }
        }
    }

    val importReceipt = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                try {
                    val savedFile = withContext(Dispatchers.IO) { copyToPrivateStorage(context, uri) }
                    showMessage(recognizeAndSave(context, dao, savedFile))
                } catch (error: Exception) {
                    showMessage("Não foi possível importar este arquivo. Tente novamente.")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val captureReceipt = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = cameraFile
        cameraFile = null
        if (success && file != null) processFile(file) else file?.delete()
    }

    fun launchCamera() {
        try {
            val file = createCameraFile(context)
            cameraFile = file
            captureReceipt.launch(FileProvider.getUriForFile(context, "${context.packageName}.provider", file))
        } catch (error: Exception) {
            cameraFile = null
            showMessage("Não foi possível abrir a câmera. Verifique a permissão e tente novamente.")
        }
    }

    fun startCamera() {
        launchCamera()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Adicionar") },
                containerColor = Ink,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.navigationBarsPadding().padding(bottom = 8.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Header(onBackupClick = { showBackupSheet = true }) }
            item { SummaryCard(total = receipts.size, isProcessing = isProcessing) }
            item { SearchField(query = query, onQueryChange = { query = it }) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { option ->
                        FilterChip(
                            selected = selectedFilter == option.key,
                            onClick = { selectedFilter = option.key },
                            label = { Text(option.label) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Ink,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = InkSoft
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == option.key,
                                borderColor = Line,
                                selectedBorderColor = Ink
                            )
                        )
                    }
                }
            }
            item { SectionHeader(title = if (query.isBlank()) "Seus comprovantes" else "Resultados", count = visibleReceipts.size) }
            if (visibleReceipts.isEmpty()) {
                item { EmptyState(onAdd = { showAddSheet = true }, isSearch = query.isNotBlank()) }
            } else {
                items(visibleReceipts, key = { it.id }) { receipt ->
                    ReceiptCard(receipt = receipt, onShare = { shareAsPdf(context, receipt) })
                }
            }
            item { TrustFooter() }
        }
    }

    if (showAddSheet) {
        AddReceiptSheet(
            onDismiss = { showAddSheet = false },
            onCamera = { showAddSheet = false; startCamera() },
            onGallery = { showAddSheet = false; importReceipt.launch(arrayOf("image/*")) }
        )
    }
    if (showBackupSheet) BackupSheet(onDismiss = { showBackupSheet = false })
}

@Composable
private fun Header(onBackupClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MeuPontoMark(Modifier.size(46.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Meu Ponto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
            Text("Clareza para sua jornada", style = MaterialTheme.typography.labelMedium, color = Muted)
        }
        IconButton(onClick = onBackupClick, modifier = Modifier.size(42.dp).background(Color.White, CircleShape)) {
            Icon(Icons.Outlined.CloudOff, contentDescription = "Status do backup", tint = Teal)
        }
    }
}

@Composable
private fun SummaryCard(total: Int, isProcessing: Boolean) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Ink, TealDark))).padding(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isProcessing) "Organizando seu comprovante" else if (total == 0) "Seu arquivo começa aqui" else "Seu arquivo está em dia",
                            color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (isProcessing) "Lendo data e horário com OCR local…" else "Tudo que importa, em um só lugar.",
                            color = Color.White.copy(alpha = .70f), style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Coral, modifier = Modifier.size(28.dp))
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(total.toString(), color = Mint, fontSize = 48.sp, fontWeight = FontWeight.Bold, lineHeight = 48.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("comprovante${if (total == 1) "" else "s"}\nsalvo${if (total == 1) "" else "s"}", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.labelLarge)
                }
                Surface(color = TealDark.copy(alpha = .75f), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = Mint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Backup local • Drive em breve", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Coral.copy(alpha = .14f), radius = 94.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width - 8.dp.toPx(), 8.dp.toPx()))
                drawCircle(Mint.copy(alpha = .10f), radius = 42.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width - 42.dp.toPx(), size.height - 8.dp.toPx()))
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Buscar por data, horário ou texto") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Teal) },
        trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Outlined.Close, contentDescription = "Limpar busca") } },
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Teal
        )
    )
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.width(8.dp))
        Surface(color = Mist, shape = RoundedCornerShape(8.dp)) {
            Text(count.toString(), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = TealDark)
        }
    }
}

@Composable
private fun ReceiptCard(receipt: Receipt, onShare: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ReceiptThumbnail(receipt.imagePath)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(receipt.workDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                Text(if (receipt.workTime.isBlank()) "Horário pendente de revisão" else "Horário identificado • ${receipt.workTime}", style = MaterialTheme.typography.bodySmall, color = if (receipt.workTime.isBlank()) Muted else TealDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(File(receipt.imagePath).name, style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, contentDescription = "Compartilhar PDF", tint = Teal) }
        }
    }
}

@Composable
private fun ReceiptThumbnail(path: String) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = path) { value = withContext(Dispatchers.IO) { decodePreview(path) } }
    Box(Modifier.size(68.dp).clip(RoundedCornerShape(16.dp)).background(Mint), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else MeuPontoMark(Modifier.size(34.dp), compact = true)
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit, isSearch: Boolean) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MeuPontoMark(Modifier.size(58.dp))
            Text(if (isSearch) "Nada encontrado" else "Seu arquivo está vazio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
            Text(if (isSearch) "Tente outra data, horário ou palavra-chave." else "Guarde seu primeiro comprovante e encontre tudo quando precisar.", style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (!isSearch) {
                Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Teal), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Adicionar comprovante")
                }
            }
        }
    }
}

@Composable
private fun TrustFooter() {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = Teal, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Seus arquivos ficam privados neste aparelho", style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReceiptSheet(onDismiss: () -> Unit, onCamera: () -> Unit, onGallery: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cream) {
        Column(Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Adicionar comprovante", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
            Text("Escolha como deseja guardar este momento.", style = MaterialTheme.typography.bodyMedium, color = Muted)
            Spacer(Modifier.height(4.dp))
            SourceOption(Icons.Outlined.CameraAlt, "Usar câmera", "Fotografe agora e deixe o OCR organizar", onCamera)
            SourceOption(Icons.Outlined.FolderOpen, "Escolher da galeria", "Importe uma imagem já salva", onGallery)
        }
    }
}

@Composable
private fun SourceOption(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = Color.White) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Mint), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Teal) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = Muted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cream) {
        Column(Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Mint), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = Teal) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Backup no Google Drive", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Próximo passo do produto", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
            Text("Seus comprovantes estão seguros neste aparelho. A sincronização com o Drive ainda está em desenvolvimento e será opcional, com login seguro e restauração por conta.", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
            Surface(color = Mist, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = Teal, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Não colocamos credenciais do Google no aplicativo nem no repositório.", style = MaterialTheme.typography.bodySmall, color = InkSoft)
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Entendi", color = Teal) }
        }
    }
}

@Composable
private fun MeuPontoMark(modifier: Modifier, compact: Boolean = false) {
    Box(modifier.clip(RoundedCornerShape(if (compact) 11.dp else 15.dp)).background(Coral), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(if (compact) 7.dp else 10.dp)) {
            val paper = androidx.compose.ui.geometry.Rect(2f, 2f, size.width - 2f, size.height - 2f)
            drawRoundRect(Color.White, topLeft = paper.topLeft, size = paper.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
            drawLine(Teal, androidx.compose.ui.geometry.Offset(size.width * .28f, size.height * .34f), androidx.compose.ui.geometry.Offset(size.width * .72f, size.height * .34f), strokeWidth = 2.5f)
            drawLine(Teal, androidx.compose.ui.geometry.Offset(size.width * .28f, size.height * .53f), androidx.compose.ui.geometry.Offset(size.width * .62f, size.height * .53f), strokeWidth = 2.5f)
            drawCircle(Teal, radius = if (compact) 3.dp.toPx() else 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .68f, size.height * .73f))
        }
    }
}

private fun copyToPrivateStorage(context: Context, uri: Uri): File {
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    val file = File(dir, "comprovante_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri).use { input -> file.outputStream().use { output -> input?.copyTo(output) ?: error("Arquivo vazio") } }
    return file
}

private fun copyFileToPrivateStorage(context: Context, source: File): File {
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    val file = File(dir, "comprovante_${System.currentTimeMillis()}.jpg")
    source.inputStream().use { input -> file.outputStream().use { output -> input.copyTo(output) } }
    return file
}

private fun createCameraFile(context: Context): File {
    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
    return File(directory, "captura_${System.currentTimeMillis()}.jpg")
}

private suspend fun recognizeAndSave(context: Context, dao: ReceiptDao, file: File): String {
    val text = withContext(Dispatchers.IO) {
        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try { recognizer.process(image).await().text } finally { recognizer.close() }
    }
    val dateMatch = Regex("\\b(\\d{2})[./-](\\d{2})[./-](\\d{4})\\b").find(text)
    val timeMatch = Regex("\\b([01]?\\d|2[0-3]):[0-5]\\d\\b").find(text)
    val date = dateMatch?.value?.replace('.', '/')?.replace('-', '/') ?: SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
    val time = timeMatch?.value ?: ""
    dao.insert(Receipt(capturedAt = System.currentTimeMillis(), workDate = date, workTime = time, imagePath = file.absolutePath, recognizedText = text))
    return if (dateMatch != null) "Comprovante salvo • confira a data $date" else "Comprovante salvo • revise a data reconhecida"
}

private fun decodePreview(path: String): Bitmap? {
    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
    return BitmapFactory.decodeFile(path, options)
}

private fun shareAsPdf(context: Context, receipt: Receipt) {
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
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", outFile)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar comprovante"))
    } catch (error: Exception) {
        Toast.makeText(context, "Falha ao gerar PDF", Toast.LENGTH_LONG).show()
    }
}

private val AppTypography = androidx.compose.material3.Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

@Composable
private fun MeuPontoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Teal,
            onPrimary = Color.White,
            primaryContainer = Mint,
            onPrimaryContainer = TealDark,
            secondary = Coral,
            onSecondary = Ink,
            background = Cream,
            onBackground = Ink,
            surface = Color.White,
            onSurface = Ink,
            surfaceVariant = Mist,
            onSurfaceVariant = InkSoft,
            outline = Line
        ),
        typography = AppTypography,
        shapes = androidx.compose.material3.Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(26.dp)),
        content = content
    )
}
