package br.com.jean.meuponto

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import br.com.jean.meuponto.data.AppDatabase
import br.com.jean.meuponto.data.Receipt
import br.com.jean.meuponto.data.ReceiptDao
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.BufferedOutputStream
import java.io.BufferedInputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

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

private val groupModes = listOf(
    FilterOption("none", "Sem agrupar"),
    FilterOption("day", "Dia"),
    FilterOption("week", "Semana")
)

private data class ReceiptGroup(val title: String, val receipts: List<Receipt>)

private fun groupReceipts(receipts: List<Receipt>, mode: String): List<ReceiptGroup> {
    if (mode == "none") return listOf(ReceiptGroup("", receipts))
    return receipts.groupBy { receipt ->
        when (mode) {
            "day" -> receipt.workDate
            "week" -> weekLabel(receipt.workDate)
            else -> ""
        }
    }.map { (title, items) -> ReceiptGroup(title, items) }
}

private fun weekLabel(workDate: String): String {
    val parsed = runCatching {
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).apply { isLenient = false }.parse(workDate)
    }.getOrNull() ?: return "Data a revisar"
    val start = Calendar.getInstance(Locale("pt", "BR")).apply {
        time = parsed
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }
    val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
    val formatter = SimpleDateFormat("dd/MM", Locale("pt", "BR"))
    return "${formatter.format(start.time)} – ${formatter.format(end.time)}"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeuPontoRoot()
        }
    }
}

@Composable
private fun MeuPontoRoot() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("meu_ponto_preferences", Context.MODE_PRIVATE) }
    val systemDark = isSystemInDarkTheme()
    var darkTheme by rememberSaveable { mutableStateOf(preferences.getBoolean("dark_theme", systemDark)) }
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MeuPontoTheme(darkTheme = darkTheme) {
        MeuPontoApp(
            isDarkTheme = darkTheme,
            onToggleTheme = {
                darkTheme = !darkTheme
                preferences.edit().putBoolean("dark_theme", darkTheme).apply()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeuPontoApp(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).receiptDao() }
    val snackbarHostState = remember { SnackbarHostState() }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("all") }
    var selectedGroupMode by rememberSaveable { mutableStateOf("none") }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showBackupSheet by rememberSaveable { mutableStateOf(false) }
    var receiptBeingEdited by remember { mutableStateOf<Receipt?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isBackupProcessing by remember { mutableStateOf(false) }
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedReceiptIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

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
    val groupedReceipts = remember(visibleReceipts, selectedGroupMode) {
        groupReceipts(visibleReceipts, selectedGroupMode)
    }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun processUri(uri: Uri) {
        scope.launch {
            isProcessing = true
            try {
                val savedFile = withContext(Dispatchers.IO) { copyToPrivateStorage(context, uri) }
                showMessage(recognizeAndSave(context, dao, savedFile))
            } catch (error: Exception) {
                showMessage("Não foi possível ler o comprovante. Tente novamente.")
            } finally {
                isProcessing = false
            }
        }
    }

    val importReceipt = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) processUri(uri)
    }

    val createBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isBackupProcessing = true
                val message = runCatching { createBackup(context, dao, uri) }
                    .fold(
                        onSuccess = { count -> "Backup criado com $count comprovante${if (count == 1) "" else "s"}." },
                        onFailure = { "Não foi possível criar o backup." }
                    )
                isBackupProcessing = false
                showMessage(message)
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isBackupProcessing = true
                val message = runCatching { restoreBackup(context, dao, uri) }
                    .fold(
                        onSuccess = { count -> "$count comprovante${if (count == 1) " restaurado" else "s restaurados"}." },
                        onFailure = { "Não foi possível restaurar esse backup." }
                    )
                isBackupProcessing = false
                showMessage(message)
            }
        }
    }

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val documentScanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val imageUri = scanResult?.pages?.firstOrNull()?.imageUri
            if (imageUri != null) {
                processUri(imageUri)
            } else {
                showMessage("Não foi possível preparar o comprovante digitalizado.")
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            showMessage("Não foi possível concluir a digitalização.")
        }
    }

    fun startScanner() {
        val activity = context as? Activity
        if (activity == null) {
            showMessage("Não foi possível iniciar o scanner neste dispositivo.")
            return
        }
        documentScanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                showMessage("Scanner indisponível. Você ainda pode importar pela galeria.")
            }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Adicionar") },
                containerColor = MaterialTheme.colorScheme.primary,
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
            item { Header(isDarkTheme = isDarkTheme, onThemeClick = onToggleTheme, onBackupClick = { showBackupSheet = true }) }
            item { SummaryCard(total = receipts.size, isProcessing = isProcessing, isDarkTheme = isDarkTheme) }
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
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == option.key,
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Agrupar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(groupModes) { option ->
                            FilterChip(
                                selected = selectedGroupMode == option.key,
                                onClick = { selectedGroupMode = option.key },
                                label = { Text(option.label) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedGroupMode == option.key,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
            item {
                SectionHeader(
                    title = if (query.isBlank()) "Seus comprovantes" else "Resultados",
                    count = visibleReceipts.size,
                    isSelectionMode = isSelectionMode,
                    selectedCount = selectedReceiptIds.size,
                    onToggleSelection = {
                        isSelectionMode = !isSelectionMode
                        if (isSelectionMode) selectedReceiptIds = emptySet()
                    },
                    onExport = {
                        val selected = visibleReceipts.filter { it.id in selectedReceiptIds }
                        shareAsPdf(context, selected)
                        isSelectionMode = false
                        selectedReceiptIds = emptySet()
                    }
                )
            }
            if (visibleReceipts.isEmpty()) {
                item { EmptyState(onAdd = { showAddSheet = true }, isSearch = query.isNotBlank()) }
            } else {
                groupedReceipts.forEachIndexed { groupIndex, group ->
                    if (selectedGroupMode != "none") {
                        item(key = "group-$groupIndex-${group.title}") {
                            GroupHeader(group.title)
                        }
                    }
                    items(group.receipts, key = { it.id }) { receipt ->
                        ReceiptCard(
                            receipt = receipt,
                            isSelectionMode = isSelectionMode,
                            isSelected = receipt.id in selectedReceiptIds,
                            onToggleSelection = {
                                selectedReceiptIds = if (receipt.id in selectedReceiptIds) {
                                    selectedReceiptIds - receipt.id
                                } else {
                                    selectedReceiptIds + receipt.id
                                }
                            },
                            onEdit = { receiptBeingEdited = receipt },
                            onShare = { shareAsPdf(context, listOf(receipt)) }
                        )
                    }
                }
            }
            item { TrustFooter() }
        }
    }

    if (showAddSheet) {
        AddReceiptSheet(
            onDismiss = { showAddSheet = false },
            onCamera = { showAddSheet = false; startScanner() },
            onGallery = { showAddSheet = false; importReceipt.launch(arrayOf("image/*")) }
        )
    }
    if (showBackupSheet) {
        BackupSheet(
            isProcessing = isBackupProcessing,
            onDismiss = { showBackupSheet = false },
            onCreateBackup = {
                createBackupLauncher.launch("meu-ponto-backup-${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.zip")
            },
            onRestoreBackup = { restoreBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }
        )
    }
    receiptBeingEdited?.let { receipt ->
        EditReceiptSheet(
            receipt = receipt,
            onDismiss = { receiptBeingEdited = null },
            onSave = { workDate, workTime ->
                scope.launch {
                    dao.updateDateAndTime(receipt.id, workDate, workTime)
                    receiptBeingEdited = null
                    showMessage("Comprovante atualizado.")
                }
            }
        )
    }
}

@Composable
private fun Header(isDarkTheme: Boolean, onThemeClick: () -> Unit, onBackupClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MeuPontoMark(Modifier.size(46.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Meu Ponto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Clareza para sua jornada", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onThemeClick, modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.surface, CircleShape)) {
            Icon(if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, contentDescription = if (isDarkTheme) "Ativar tema claro" else "Ativar tema escuro", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onBackupClick, modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.surface, CircleShape)) {
            Icon(Icons.Outlined.CloudOff, contentDescription = "Status do backup", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SummaryCard(total: Int, isProcessing: Boolean, isDarkTheme: Boolean) {
    val heroGradient = if (isDarkTheme) {
        listOf(Color(0xFF14242C), Color(0xFF0D4C47))
    } else {
        listOf(Ink, TealDark)
    }
    val heroText = if (isDarkTheme) Color(0xFFF1F8F5) else Color.White
    val heroMuted = if (isDarkTheme) Color(0xFFB8CCC7) else Color.White.copy(alpha = .70f)
    val heroNumber = if (isDarkTheme) Color(0xFF8BE3D2) else Mint
    val heroBadge = if (isDarkTheme) Color(0xFF103A38) else TealDark.copy(alpha = .75f)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(heroGradient)).padding(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isProcessing) "Organizando seu comprovante" else if (total == 0) "Seu arquivo começa aqui" else "Seu arquivo está em dia",
                            color = heroText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (isProcessing) "Lendo data e horário com OCR local…" else "Tudo que importa, em um só lugar.",
                            color = heroMuted, style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Coral, modifier = Modifier.size(28.dp))
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(total.toString(), color = heroNumber, fontSize = 48.sp, fontWeight = FontWeight.Bold, lineHeight = 48.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("comprovante${if (total == 1) "" else "s"}\nsalvo${if (total == 1) "" else "s"}", color = heroMuted, style = MaterialTheme.typography.labelLarge)
                }
                Surface(color = heroBadge, shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = Mint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Backup exportável • pronto", color = heroText, style = MaterialTheme.typography.labelMedium)
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
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Outlined.Close, contentDescription = "Limpar busca") } },
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onToggleSelection: () -> Unit,
    onExport: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.width(8.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
            Text(count.toString(), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.weight(1f))
        if (isSelectionMode) {
            TextButton(onClick = onExport, enabled = selectedCount > 0) {
                Text(if (selectedCount > 0) "PDF ($selectedCount)" else "PDF")
            }
            TextButton(onClick = onToggleSelection) { Text("Cancelar") }
        } else {
            TextButton(onClick = onToggleSelection) { Text("Selecionar") }
        }
    }
}

@Composable
private fun GroupHeader(title: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
            Text(
                title,
                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = .35f)))
    }
}

@Composable
private fun ReceiptCard(
    receipt: Receipt,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().clickable(enabled = isSelectionMode, onClick = onToggleSelection),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ReceiptThumbnail(receipt.imagePath)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(receipt.workDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(if (receipt.workTime.isBlank()) "Horário pendente de revisão" else "Horário identificado • ${receipt.workTime}", style = MaterialTheme.typography.bodySmall, color = if (receipt.workTime.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(File(receipt.imagePath).name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isSelectionMode) {
                androidx.compose.material3.Checkbox(checked = isSelected, onCheckedChange = { onToggleSelection() })
            } else {
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Editar data e horário", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, contentDescription = "Compartilhar PDF", tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun ReceiptThumbnail(path: String) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = path) { value = withContext(Dispatchers.IO) { decodePreview(path) } }
    Box(Modifier.size(68.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else MeuPontoMark(Modifier.size(34.dp), compact = true)
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit, isSearch: Boolean) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MeuPontoMark(Modifier.size(58.dp))
            Text(if (isSearch) "Nada encontrado" else "Seu arquivo está vazio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(if (isSearch) "Tente outra data, horário ou palavra-chave." else "Guarde seu primeiro comprovante e encontre tudo quando precisar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (!isSearch) {
                Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(14.dp)) {
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
        Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Seus arquivos ficam privados neste aparelho", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReceiptSheet(onDismiss: () -> Unit, onCamera: () -> Unit, onGallery: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Adicionar comprovante", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Escolha como deseja guardar este momento.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            SourceOption(Icons.Outlined.CameraAlt, "Usar câmera", "Fotografe agora e deixe o OCR organizar", onCamera)
            SourceOption(Icons.Outlined.FolderOpen, "Escolher da galeria", "Importe uma imagem já salva", onGallery)
        }
    }
}

@Composable
private fun SourceOption(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupSheet(
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Backup no Google Drive", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Proteção local e exportação segura", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Crie um backup completo com imagens e dados reconhecidos. No seletor do Android, escolha o Google Drive ou outro local seguro; depois use Restaurar para trazer tudo de volta.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onCreateBackup,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isProcessing) "Processando…" else "Criar backup")
            }
            TextButton(onClick = onRestoreBackup, enabled = !isProcessing, modifier = Modifier.align(Alignment.End)) {
                Text("Restaurar backup", color = MaterialTheme.colorScheme.primary)
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("O arquivo é criado no aparelho e só vai para o local que você escolher. Nenhuma credencial do Google fica no aplicativo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Entendi", color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditReceiptSheet(
    receipt: Receipt,
    onDismiss: () -> Unit,
    onSave: (workDate: String, workTime: String) -> Unit
) {
    var workDate by remember(receipt.id) { mutableStateOf(receipt.workDate) }
    var workTime by remember(receipt.id) { mutableStateOf(receipt.workTime) }
    val dateValid = Regex("\\d{2}/\\d{2}/\\d{4}").matches(workDate.trim())
    val timeValid = workTime.isBlank() || Regex("([01]?\\d|2[0-3]):[0-5]\\d").matches(workTime.trim())

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Revisar comprovante", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Confirme o que o OCR identificou ou corrija os campos antes de arquivar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = workDate,
                onValueChange = { workDate = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Data") },
                placeholder = { Text("dd/mm/aaaa") },
                isError = workDate.isNotBlank() && !dateValid,
                supportingText = { if (workDate.isNotBlank() && !dateValid) Text("Use o formato dd/mm/aaaa") }
            )
            OutlinedTextField(
                value = workTime,
                onValueChange = { workTime = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Horário (opcional)") },
                placeholder = { Text("hh:mm") },
                isError = !timeValid,
                supportingText = {
                    if (!timeValid) Text("Use o formato hh:mm") else Text("Se não houver horário no comprovante, deixe em branco.")
                }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(workDate.trim(), workTime.trim()) },
                    enabled = dateValid && timeValid,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Confirmar e salvar")
                }
            }
        }
    }
}

@Composable
private fun MeuPontoMark(modifier: Modifier, compact: Boolean = false) {
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier.clip(RoundedCornerShape(if (compact) 11.dp else 15.dp)).background(Coral), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(if (compact) 7.dp else 10.dp)) {
            val paper = androidx.compose.ui.geometry.Rect(2f, 2f, size.width - 2f, size.height - 2f)
            drawRoundRect(Color.White, topLeft = paper.topLeft, size = paper.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
            drawLine(primary, androidx.compose.ui.geometry.Offset(size.width * .28f, size.height * .34f), androidx.compose.ui.geometry.Offset(size.width * .72f, size.height * .34f), strokeWidth = 2.5f)
            drawLine(primary, androidx.compose.ui.geometry.Offset(size.width * .28f, size.height * .53f), androidx.compose.ui.geometry.Offset(size.width * .62f, size.height * .53f), strokeWidth = 2.5f)
            drawCircle(primary, radius = if (compact) 3.dp.toPx() else 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .68f, size.height * .73f))
        }
    }
}

private fun copyToPrivateStorage(context: Context, uri: Uri): File {
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    val file = File(dir, "comprovante_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri).use { input -> file.outputStream().use { output -> input?.copyTo(output) ?: error("Arquivo vazio") } }
    return file
}

private suspend fun createBackup(context: Context, dao: ReceiptDao, destination: Uri): Int = withContext(Dispatchers.IO) {
    val receipts = dao.getAll()
    val manifest = JSONArray()
    context.contentResolver.openOutputStream(destination)?.use { output ->
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            receipts.forEach { receipt ->
                val source = File(receipt.imagePath)
                if (!source.isFile) return@forEach
                val imageEntry = "images/${receipt.id}.jpg"
                zip.putNextEntry(ZipEntry(imageEntry))
                source.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
                manifest.put(
                    JSONObject()
                        .put("id", receipt.id)
                        .put("capturedAt", receipt.capturedAt)
                        .put("workDate", receipt.workDate)
                        .put("workTime", receipt.workTime)
                        .put("recognizedText", receipt.recognizedText)
                        .put("imageEntry", imageEntry)
                )
            }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    } ?: error("Destino de backup indisponível")
    manifest.length()
}

private suspend fun restoreBackup(context: Context, dao: ReceiptDao, sourceUri: Uri): Int = withContext(Dispatchers.IO) {
    val restoreDir = File(context.cacheDir, "restore_${System.currentTimeMillis()}").apply { mkdirs() }
    try {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val target = File(restoreDir, entry.name)
                    val rootPath = restoreDir.canonicalPath + File.separator
                    if (!target.canonicalPath.startsWith(rootPath)) error("Backup inválido")
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { output -> zip.copyTo(output) }
                    }
                    zip.closeEntry()
                }
            }
        } ?: error("Arquivo de backup indisponível")

        val manifestFile = File(restoreDir, "manifest.json")
        if (!manifestFile.isFile) error("Manifesto do backup ausente")
        val items = JSONArray(manifestFile.readText(Charsets.UTF_8))
        val destinationDir = File(context.filesDir, "receipts").apply { mkdirs() }
        val restored = buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val imageSource = File(restoreDir, item.getString("imageEntry"))
                if (!imageSource.isFile) continue
                val imageDestination = File(destinationDir, "restored_${item.getLong("id")}_${System.currentTimeMillis()}_$index.jpg")
                imageSource.copyTo(imageDestination, overwrite = true)
                add(
                    Receipt(
                        id = item.getLong("id"),
                        capturedAt = item.getLong("capturedAt"),
                        workDate = item.getString("workDate"),
                        workTime = item.getString("workTime"),
                        imagePath = imageDestination.absolutePath,
                        recognizedText = item.optString("recognizedText")
                    )
                )
            }
        }
        dao.insertAll(restored)
        restored.size
    } finally {
        restoreDir.deleteRecursively()
    }
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

private fun shareAsPdf(context: Context, receipts: List<Receipt>) {
    if (receipts.isEmpty()) return
    try {
        val pdf = PdfDocument()
        if (receipts.size == 1) {
            val receipt = receipts.first()
            val bitmap = decodePdfBitmap(receipt.imagePath) ?: error("Imagem indisponível")
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width.coerceAtLeast(1), bitmap.height.coerceAtLeast(1), 1).create()
            val page = pdf.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdf.finishPage(page)
            bitmap.recycle()
        } else {
            val pageWidth = 1200f
            val pageHeight = 1697f
            val margin = 36f
            val gap = 24f
            val slotHeight = (pageHeight - (margin * 2) - gap) / 2f
            receipts.chunked(2).forEachIndexed { pageIndex, pageReceipts ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageIndex + 1).create()
                val page = pdf.startPage(pageInfo)
                page.canvas.drawColor(android.graphics.Color.WHITE)
                pageReceipts.forEachIndexed { slotIndex, receipt ->
                    val bitmap = decodePdfBitmap(receipt.imagePath) ?: error("Imagem indisponível")
                    val top = margin + (slotIndex * (slotHeight + gap))
                    val target = fitBitmapRect(bitmap, margin, top, pageWidth - margin, top + slotHeight)
                    page.canvas.drawBitmap(bitmap, null, target, null)
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
private fun MeuPontoTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) {
            androidx.compose.material3.darkColorScheme(
                primary = Color(0xFF74D9C6),
                onPrimary = Color(0xFF00382F),
                primaryContainer = Color(0xFF075448),
                onPrimaryContainer = Color(0xFF9AF5E2),
                secondary = Color(0xFFFFA38F),
                onSecondary = Color(0xFF4A160D),
                tertiary = Color(0xFF9BDCCB),
                background = Color(0xFF0C141A),
                onBackground = Color(0xFFE8F0ED),
                surface = Color(0xFF121E25),
                onSurface = Color(0xFFE8F0ED),
                surfaceVariant = Color(0xFF203038),
                onSurfaceVariant = Color(0xFFB6C7C2),
                outline = Color(0xFF42565D)
            )
        } else {
            androidx.compose.material3.lightColorScheme(
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
            )
        },
        typography = AppTypography,
        shapes = androidx.compose.material3.Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(26.dp)),
        content = content
    )
}
