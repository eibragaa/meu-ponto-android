package br.com.jean.meuponto

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import br.com.jean.meuponto.BuildConfig
import br.com.jean.meuponto.data.AppDatabase
import br.com.jean.meuponto.data.Receipt
import br.com.jean.meuponto.data.cleanupUpdateAndPdfCaches
import br.com.jean.meuponto.data.copyToPrivateStorage
import br.com.jean.meuponto.data.createBackup
import br.com.jean.meuponto.data.decodePreview
import br.com.jean.meuponto.data.recognizeAndSave
import br.com.jean.meuponto.data.restoreBackup
import br.com.jean.meuponto.data.shareAsPdf
import br.com.jean.meuponto.domain.DayOccurrence
import br.com.jean.meuponto.domain.PayrollSettingsStore
import br.com.jean.meuponto.domain.PeriodStats
import br.com.jean.meuponto.domain.PunchChoice
import br.com.jean.meuponto.domain.WorkStatsCalculator
import br.com.jean.meuponto.domain.dayIssueSummary
import br.com.jean.meuponto.domain.dayOccurrenceSummary
import br.com.jean.meuponto.domain.formatMinutes
import br.com.jean.meuponto.domain.occurrenceLabel
import br.com.jean.meuponto.domain.punchChoiceOf
import br.com.jean.meuponto.domain.receiptReviewHint
import br.com.jean.meuponto.domain.receiptTimeLabel
import br.com.jean.meuponto.ui.Coral
import br.com.jean.meuponto.ui.DaySection
import br.com.jean.meuponto.ui.DeleteReceiptDialog
import br.com.jean.meuponto.ui.HeroNightBottom
import br.com.jean.meuponto.ui.HeroNightMuted
import br.com.jean.meuponto.ui.HeroNightNumber
import br.com.jean.meuponto.ui.HeroNightText
import br.com.jean.meuponto.ui.HeroNightTop
import br.com.jean.meuponto.ui.Ink
import br.com.jean.meuponto.ui.InsightsSheet
import br.com.jean.meuponto.ui.MeuPontoTheme
import br.com.jean.meuponto.ui.Mint
import br.com.jean.meuponto.ui.MonthSection
import br.com.jean.meuponto.ui.PayrollSettingsSheet
import br.com.jean.meuponto.ui.PtBrLocale
import br.com.jean.meuponto.ui.ReceiptViewer
import br.com.jean.meuponto.ui.SwipeToDeleteBox
import br.com.jean.meuponto.ui.TealDark
import br.com.jean.meuponto.ui.UpdateBanner
import br.com.jean.meuponto.ui.WhatsNewSheet
import br.com.jean.meuponto.ui.buildMonthSections
import br.com.jean.meuponto.ui.currentMonthKey
import br.com.jean.meuponto.ui.filters
import br.com.jean.meuponto.ui.formatDateInput
import br.com.jean.meuponto.ui.formatTimeInput
import br.com.jean.meuponto.ui.groupModes
import br.com.jean.meuponto.ui.groupReceipts
import br.com.jean.meuponto.ui.isValidWorkDate
import br.com.jean.meuponto.ui.maskedFieldValue
import br.com.jean.meuponto.updater.AboutSheet
import br.com.jean.meuponto.updater.AppUpdater
import br.com.jean.meuponto.updater.UpdateDialog
import br.com.jean.meuponto.updater.UpdateRuntime
import br.com.jean.meuponto.updater.UpdateUiState
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Tela principal do app.
 *
 * Responsabilidade deste arquivo: **estado da tela + composição visual**. Nenhum I/O pesado
 * acontece aqui: OCR (`recognizeAndSave`), backup/restauração (`createBackup`/`restoreBackup`),
 * PDF (`shareAsPdf`) e limpeza de cache (`cleanupUpdateAndPdfCaches`) ficam em `data/`;
 * agrupamento e formatação em `ui/`; regras de jornada em `domain/`.
 *
 * Ordem do arquivo: raiz e tema -> estado da tela -> lista (cabeçalho, mês, dia, card) ->
 * folhas (adicionar, backup, organizar, editor) -> utilitários de UI.
 */
/** Tempo que o resultado da verificação manual fica visível no rodapé. */
private const val UPDATE_CHECK_MESSAGE_MS = 6000L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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

/**
 * Dono do estado da tela inicial: lista, filtros, folhas e ações de arquivo.
 *
 * O estado vive aqui (e não em um ViewModel) por decisão histórica; é a pendência ENG-1 do padrão de
 * engenharia. Os valores derivados usam `remember(chaves)` para não recalcular a cada recomposição.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeuPontoApp(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).receiptDao() }
    val snackbarHostState = remember { SnackbarHostState() }

    // Busca e organização da lista: em `rememberSaveable` para sobreviver a rotação e à recriação
    // do processo.
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("all") }
    var selectedGroupMode by rememberSaveable { mutableStateOf("month") }

    // Uma flag por folha (bottom sheet) aberta. São mutuamente exclusivas na prática; unificá-las
    // em um único estado selado está registrado como pendência no padrão de engenharia (ENG-1).
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showBackupSheet by rememberSaveable { mutableStateOf(false) }
    var showOrganizeSheet by rememberSaveable { mutableStateOf(false) }
    var showPayrollSettings by rememberSaveable { mutableStateOf(false) }
    var showInsights by rememberSaveable { mutableStateOf(false) }
    var backupPasswordDialog by rememberSaveable { mutableStateOf<String?>(null) }

    // Registros em edição/exibição/exclusão: guardam o objeto inteiro para a folha não precisar
    // reler o banco enquanto está aberta.
    var receiptBeingEdited by remember { mutableStateOf<Receipt?>(null) }
    var receiptBeingViewed by remember { mutableStateOf<Receipt?>(null) }
    var receiptPendingDelete by remember { mutableStateOf<Receipt?>(null) }
    var payrollSettings by remember { mutableStateOf(PayrollSettingsStore.load(context)) }
    var expandedMonths by remember { mutableStateOf(setOf(currentMonthKey())) }
    var expandedDays by remember { mutableStateOf(emptySet<String>()) }
    var isProcessing by remember { mutableStateOf(false) }
    var isBackupProcessing by remember { mutableStateOf(false) }
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedReceiptIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingBackupPassword by remember { mutableStateOf("") }
    val updaterEnabled = BuildConfig.UPDATER_ENABLED
    val currentVersion = remember { AppUpdater.getInstalledVersionName(context) }
    // Processo único (UpdateRuntime) para o estado do updater sobreviver à rotação; a tela apenas
    // observa e dispara eventos. Sem updater (flavor play), um fluxo vazio mantém o estado neutro.
    val updateCoordinator = remember {
        if (updaterEnabled) UpdateRuntime.get(context, currentVersion) else null
    }
    val emptyUpdateState = remember { MutableStateFlow(UpdateUiState()) }
    val updateState by (updateCoordinator?.state ?: emptyUpdateState).collectAsState()
    var showAboutSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cleanupUpdateAndPdfCaches(context)
        updateCoordinator?.onColdStart()
    }

    // ON_START sem dependência nova: o lifecycle do ComponentActivity avisa quando o app volta ao
    // foreground. O coordenador aplica o throttle de 30 min internamente.
    val lifecycleOwner = context as? ComponentActivity
    DisposableEffect(lifecycleOwner, updateCoordinator) {
        val coordinator = updateCoordinator
        if (lifecycleOwner != null && coordinator != null) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    coordinator.onForeground()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(updateState.isPromptVisible) {
        if (updateState.isPromptVisible) {
            showAboutSheet = false
        }
    }

    val receipts by (if (query.isBlank()) dao.observeAll() else dao.search(query))
        .collectAsState(initial = emptyList())
    val visibleReceipts = remember(receipts, selectedFilter) {
        when (selectedFilter) {
            "today" -> {
                val today = SimpleDateFormat("dd/MM/yyyy", PtBrLocale).format(Date())
                receipts.filter { it.workDate == today }
            }
            "time" -> receipts.filter {
                it.workTime.isNotBlank() || it.entryTime != null || it.exitTime != null
            }
            "review" -> WorkStatsCalculator.receiptsNeedingReview(receipts)
            else -> receipts
        }
    }
    val visibleDayStats = remember(visibleReceipts) {
        WorkStatsCalculator.dayStats(visibleReceipts)
    }
    val periodStats = remember(receipts, payrollSettings) {
        WorkStatsCalculator.periodStats(receipts, payrollSettings)
    }
    val groupedReceipts = remember(visibleReceipts, selectedGroupMode) {
        groupReceipts(visibleReceipts, selectedGroupMode)
    }
    val monthSections = remember(visibleReceipts, visibleDayStats) {
        buildMonthSections(visibleReceipts, visibleDayStats)
    }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun checkUpdatesManually() {
        updateCoordinator?.checkNow()
    }

    // A verificação manual devolve o resultado pelo estado. A resposta aparece no próprio rodapé,
    // onde o usuário tocou: em snackbar ela ficava atrás do FAB e do rodapé, parecendo ignorada.
    var updateCheckMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(updateState.manualMessage) {
        updateState.manualMessage?.let { message ->
            updateCheckMessage = message
            updateCoordinator?.consumeManualMessage()
        }
    }
    LaunchedEffect(updateCheckMessage) {
        if (updateCheckMessage != null) {
            delay(UPDATE_CHECK_MESSAGE_MS)
            updateCheckMessage = null
        }
    }

    /**
     * Importa uma imagem (câmera ou galeria) e grava o comprovante com o que o OCR reconheceu.
     * A cópia para o storage privado roda em IO; a mensagem devolvida vira snackbar.
     */
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

    // A senha é guardada em `pendingBackupPassword` *antes* de abrir o seletor de arquivos: o
    // launcher só devolve a Uri depois que o diálogo já foi fechado, então não dá para lê-la lá.
    val createBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isBackupProcessing = true
                val password = pendingBackupPassword
                pendingBackupPassword = ""
                val message = runCatching { createBackup(context, dao, uri, password) }
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
                val password = pendingBackupPassword
                pendingBackupPassword = ""
                val message = runCatching { restoreBackup(context, dao, uri, password) }
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

    /**
     * Abre o Document Scanner do ML Kit.
     *
     * Ele devolve um `IntentSender` que exige uma `Activity` (não serve `Context` genérico), por
     * isso o cast; quando não há Activity ou Play Services, cai na mensagem para usar a galeria.
     */
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
            if (receipts.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Adicionar") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.navigationBarsPadding().padding(bottom = 8.dp)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Header(isDarkTheme = isDarkTheme, onThemeClick = onToggleTheme, onBackupClick = { showBackupSheet = true }, onAboutClick = { showAboutSheet = true }) }
            updateState.bannerVersion?.let { bannerVersion ->
                item(key = "update-banner") {
                    UpdateBanner(
                        version = bannerVersion,
                        summary = updateState.bannerSummary.orEmpty(),
                        isFeature = updateState.bannerIsFeature,
                        onShowNotes = { updateCoordinator?.showPrompt() },
                        onDismiss = { updateCoordinator?.dismissBanner() }
                    )
                }
            }
            item {
                SummaryCard(
                    total = receipts.size,
                    stats = periodStats,
                    isProcessing = isProcessing,
                    isDarkTheme = isDarkTheme,
                    onConfigure = { showPayrollSettings = true },
                    onInsights = { showInsights = true }
                )
            }
            item { SearchField(query = query, onQueryChange = { query = it }) }
            item {
                OrganizeBar(
                    selectedFilter = selectedFilter,
                    selectedGroupMode = selectedGroupMode,
                    onClick = { showOrganizeSheet = true }
                )
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
            } else if (selectedGroupMode == "month") {
                monthSections.forEach { month ->
                    val monthExpanded = query.isNotBlank() || expandedMonths.contains(month.key)
                    item(key = "month-${month.key}") {
                        MonthHeader(
                            month = month,
                            expanded = monthExpanded,
                            onToggle = {
                                expandedMonths = if (expandedMonths.contains(month.key)) {
                                    expandedMonths - month.key
                                } else {
                                    expandedMonths + month.key
                                }
                            }
                        )
                    }
                    if (monthExpanded) {
                        month.days.forEach { day ->
                            val dayExpanded = query.isNotBlank() || expandedDays.contains(day.key)
                            item(key = "day-${day.key}") {
                                DayHeader(
                                    day = day,
                                    expanded = dayExpanded,
                                    onToggle = {
                                        expandedDays = if (expandedDays.contains(day.key)) {
                                            expandedDays - day.key
                                        } else {
                                            expandedDays + day.key
                                        }
                                    }
                                )
                            }
                            if (dayExpanded) {
                                items(day.receipts, key = { it.id }) { receipt ->
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
                                        onOpen = { receiptBeingViewed = receipt },
                                        onEdit = { receiptBeingEdited = receipt },
                                        onShare = { shareAsPdf(context, listOf(receipt)) },
                                        onRequestDelete = { receiptPendingDelete = receipt }
                                    )
                                }
                            }
                        }
                    }
                }
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
                            onOpen = { receiptBeingViewed = receipt },
                            onEdit = { receiptBeingEdited = receipt },
                            onShare = { shareAsPdf(context, listOf(receipt)) },
                            onRequestDelete = { receiptPendingDelete = receipt }
                        )
                    }
                }
            }
            item {
                TrustFooter(
                    versionName = currentVersion,
                    updaterEnabled = updaterEnabled,
                    isChecking = updateState.isChecking,
                    checkMessage = updateCheckMessage,
                    onCheckUpdate = { checkUpdatesManually() }
                )
            }
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
                showBackupSheet = false
                backupPasswordDialog = "create"
            },
            onRestoreBackup = {
                showBackupSheet = false
                backupPasswordDialog = "restore"
            }
        )
    }
    if (showOrganizeSheet) {
        OrganizeSheet(
            selectedFilter = selectedFilter,
            selectedGroupMode = selectedGroupMode,
            onFilterChange = { selectedFilter = it },
            onGroupChange = { selectedGroupMode = it },
            onDismiss = { showOrganizeSheet = false }
        )
    }
    if (showPayrollSettings) {
        PayrollSettingsSheet(
            settings = payrollSettings,
            onDismiss = { showPayrollSettings = false },
            onSave = { settings ->
                PayrollSettingsStore.save(context, settings)
                payrollSettings = settings
                showPayrollSettings = false
                showMessage("Período de folha atualizado.")
            }
        )
    }
    if (showInsights) {
        InsightsSheet(stats = periodStats, onDismiss = { showInsights = false })
    }
    backupPasswordDialog?.let { action ->
        BackupPasswordDialog(
            action = action,
            onDismiss = { backupPasswordDialog = null },
            onConfirm = { password ->
                backupPasswordDialog = null
                if (action == "create") {
                    pendingBackupPassword = password
                    createBackupLauncher.launch("meu-ponto-backup-${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.zip")
                } else {
                    pendingBackupPassword = password
                    restoreBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                }
            }
        )
    }
    receiptBeingEdited?.let { receipt ->
        EditReceiptSheet(
            receipt = receipt,
            onDismiss = { receiptBeingEdited = null },
            onSave = { workDate, entryTime, exitTime, occurrence ->
                scope.launch {
                    dao.updateReceiptDetails(
                        id = receipt.id,
                        workDate = workDate,
                        workTime = entryTime ?: exitTime ?: "",
                        entryTime = entryTime,
                        exitTime = exitTime,
                        needsReview = false,
                        occurrence = occurrence
                    )
                    receiptBeingEdited = null
                    showMessage("Comprovante atualizado.")
                }
            }
        )
    }

    receiptBeingViewed?.let { receipt ->
        val viewerIndex = visibleReceipts.indexOfFirst { it.id == receipt.id }
        ReceiptViewer(
            receipt = receipt,
            hasPrevious = viewerIndex > 0,
            hasNext = viewerIndex >= 0 && viewerIndex < visibleReceipts.lastIndex,
            onPrevious = {
                if (viewerIndex > 0) receiptBeingViewed = visibleReceipts[viewerIndex - 1]
            },
            onNext = {
                if (viewerIndex >= 0 && viewerIndex < visibleReceipts.lastIndex) {
                    receiptBeingViewed = visibleReceipts[viewerIndex + 1]
                }
            },
            onEdit = {
                receiptBeingViewed = null
                receiptBeingEdited = receipt
            },
            onShare = { shareAsPdf(context, listOf(receipt)) },
            onDelete = { receiptPendingDelete = receipt },
            onClose = { receiptBeingViewed = null }
        )
    }

    receiptPendingDelete?.let { receipt ->
        DeleteReceiptDialog(
            receipt = receipt,
            onDismiss = { receiptPendingDelete = null },
            onConfirm = {
                scope.launch {
                    dao.delete(receipt.id)
                    runCatching { File(receipt.imagePath).delete() }
                    receiptPendingDelete = null
                    if (receiptBeingViewed?.id == receipt.id) receiptBeingViewed = null
                    showMessage("Comprovante excluído.")
                }
            }
        )
    }

    val coordinator = updateCoordinator
    if (coordinator != null && updateState.isPromptVisible) {
        updateState.available?.let { info ->
            UpdateDialog(
                updateInfo = info,
                currentVersion = currentVersion,
                onDismiss = coordinator::dismissPrompt,
                onInstallerReady = coordinator::onInstallerReady
            )
        }
    }

    if (showAboutSheet) {
        AboutSheet(
            currentVersion = currentVersion,
            isCheckingUpdate = updateState.isChecking,
            updaterEnabled = updaterEnabled,
            onCheckUpdate = { checkUpdatesManually() },
            onDismiss = { showAboutSheet = false },
            brandMark = { MeuPontoMark(Modifier.size(56.dp)) }
        )
    }

    updateState.whatsNew?.let { pendingNotes ->
        WhatsNewSheet(
            version = pendingNotes.version,
            notes = pendingNotes.notes,
            onDismiss = { updateCoordinator?.dismissWhatsNew() }
        )
    }
}

@Composable
private fun Header(
    isDarkTheme: Boolean,
    onThemeClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAboutClick: () -> Unit
) {
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
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = onBackupClick, modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.surface, CircleShape)) {
            Icon(Icons.Outlined.CloudOff, contentDescription = "Status do backup", tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = onAboutClick, modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.surface, CircleShape)) {
            Icon(Icons.Outlined.Info, contentDescription = "Sobre e Atualizações", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SummaryCard(
    total: Int,
    stats: PeriodStats,
    isProcessing: Boolean,
    isDarkTheme: Boolean,
    onConfigure: () -> Unit,
    onInsights: () -> Unit
) {
    val heroGradient = if (isDarkTheme) listOf(HeroNightTop, HeroNightBottom) else listOf(Ink, TealDark)
    val heroText = if (isDarkTheme) HeroNightText else Color.White
    val heroMuted = if (isDarkTheme) HeroNightMuted else Color.White.copy(alpha = .70f)
    val heroNumber = if (isDarkTheme) HeroNightNumber else Mint
    val totalDays = ChronoUnit.DAYS.between(stats.periodStart, stats.periodEnd).toInt() + 1
    val elapsedDays = (ChronoUnit.DAYS.between(stats.periodStart, LocalDate.now()).toInt() + 1)
        .coerceIn(0, totalDays.coerceAtLeast(0))
    val progress = if (totalDays > 0) elapsedDays.toFloat() / totalDays else 0f
    val shortDate = DateTimeFormatter.ofPattern("dd/MM")
    val periodLabel = "${stats.periodStart.format(shortDate)} a ${stats.periodEnd.format(shortDate)}"
    val closingLabel = when {
        LocalDate.now().isAfter(stats.periodEnd) -> "Período encerrado"
        stats.daysRemaining == 0 -> "Fecha hoje"
        else -> "Faltam ${stats.daysRemaining} dias para o fechamento"
    }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(heroGradient)).padding(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isProcessing) "Organizando seu comprovante" else if (total == 0) "Seu arquivo começa aqui" else "Resumo do período",
                            color = heroText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (isProcessing) "Lendo data e horário com OCR local…" else periodLabel,
                            color = heroMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Coral, modifier = Modifier.size(28.dp))
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        formatMinutes(stats.totalMinutes),
                        color = heroNumber,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 44.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("horas\ntrabalhadas", color = heroMuted, style = MaterialTheme.typography.labelLarge)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = heroNumber, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(closingLabel, color = heroText, style = MaterialTheme.typography.labelLarge)
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(8.dp)),
                    color = heroNumber,
                    trackColor = heroMuted.copy(alpha = .25f)
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${stats.daysWithRecords} dias com registro",
                        color = heroMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "· ${stats.pendingReceipts} pendente${if (stats.pendingReceipts == 1) "" else "s"}",
                        color = if (stats.pendingReceipts > 0) Coral else heroMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onConfigure,
                        colors = ButtonDefaults.textButtonColors(contentColor = heroText)
                    ) { Text("Configurar") }
                    TextButton(
                        onClick = onInsights,
                        colors = ButtonDefaults.textButtonColors(contentColor = heroNumber)
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Insights")
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
private fun OrganizeBar(selectedFilter: String, selectedGroupMode: String, onClick: () -> Unit) {
    val filterLabel = filters.firstOrNull { it.key == selectedFilter }?.label ?: "Todos"
    val groupLabel = groupModes.firstOrNull { it.key == selectedGroupMode }?.label ?: "Sem agrupar"
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Visão do arquivo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("$filterLabel · $groupLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text("Organizar")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrganizeSheet(
    selectedFilter: String,
    selectedGroupMode: String,
    onFilterChange: (String) -> Unit,
    onGroupChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.imePadding().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Organizar arquivo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Escolha uma visão sem poluir a tela principal.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Mostrar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { option ->
                    FilterChip(
                        selected = selectedFilter == option.key,
                        onClick = { onFilterChange(option.key) },
                        label = { Text(option.label) },
                        shape = RoundedCornerShape(12.dp),
                        colors = organizeChipColors(),
                        border = organizeChipBorder(selectedFilter == option.key)
                    )
                }
            }
            Text("Agrupar por", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groupModes) { option ->
                    FilterChip(
                        selected = selectedGroupMode == option.key,
                        onClick = { onGroupChange(option.key) },
                        label = { Text(option.label) },
                        shape = RoundedCornerShape(12.dp),
                        colors = organizeChipColors(),
                        border = organizeChipBorder(selectedGroupMode == option.key)
                    )
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text("Aplicar visão")
            }
        }
    }
}

@Composable
private fun organizeChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
private fun organizeChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = MaterialTheme.colorScheme.outline,
    selectedBorderColor = MaterialTheme.colorScheme.primary
)

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
        // Contagem discreta: um chip aqui competia com o botão "Organizar" e parecia ação.
        Text(
            "· $count",
            Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
private fun MonthHeader(
    month: MonthSection,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    month.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${month.receiptCount} comprovante${if (month.receiptCount == 1) "" else "s"} · ${formatMinutes(month.minutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Recolher mês" else "Expandir mês",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DayHeader(
    day: DaySection,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                day.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "${day.receiptCount} comprovante${if (day.receiptCount == 1) "" else "s"} · ${formatMinutes(day.minutes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (day.pending) {
                Text(
                    dayIssueSummary(day.issues),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (day.pending) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 1.dp)
            ) {
                Text(
                    "Revisar",
                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(Modifier.width(8.dp))
        } else if (day.occurrences.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 1.dp)
            ) {
                Text(
                    dayOccurrenceSummary(day.occurrences),
                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Recolher dia" else "Expandir dia",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 1.dp).size(20.dp)
        )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReceiptCard(
    receipt: Receipt,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val duration = WorkStatsCalculator.durationMinutes(receipt)
    SwipeToDeleteBox(
        enabled = !isSelectionMode,
        onDeleteRequest = onRequestDelete
    ) {
        Card(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (isSelectionMode) onToggleSelection() else onOpen() },
                    onLongClick = { if (!isSelectionMode) onToggleSelection() }
                ),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ReceiptThumbnail(receipt.imagePath)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        receipt.workDate.ifBlank { "Data pendente" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            receiptTimeLabel(receipt),
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                receipt.needsReview -> MaterialTheme.colorScheme.error
                                receipt.entryTime == null && receipt.exitTime == null && receipt.workTime.isBlank() ->
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.primary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (duration != null) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    formatMinutes(duration),
                                    Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        if (receipt.needsReview) receiptReviewHint(receipt) else File(receipt.imagePath).name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (receipt.needsReview) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isSelectionMode) {
                    androidx.compose.material3.Checkbox(checked = isSelected, onCheckedChange = { onToggleSelection() })
                } else {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar data e horário", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Outlined.Share, contentDescription = "Compartilhar PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                }
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
private fun TrustFooter(
    versionName: String,
    updaterEnabled: Boolean,
    isChecking: Boolean,
    checkMessage: String?,
    onCheckUpdate: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Seus arquivos ficam privados neste aparelho", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (updaterEnabled && isChecking) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Verificando atualizações…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Text(
                text = if (updaterEnabled) "Meu Ponto v$versionName • Toque para verificar atualizações" else "Meu Ponto v$versionName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                modifier = if (updaterEnabled) {
                    Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onCheckUpdate).padding(horizontal = 6.dp, vertical = 2.dp)
                } else {
                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                }
            )
        }
        // Resultado da verificação manual no mesmo lugar do toque, para não ficar atrás do FAB.
        checkMessage?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
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
            Text("Crie um backup completo com imagens e dados reconhecidos. Você poderá protegê-lo com uma senha e escolher o Google Drive ou outro local seguro.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("O arquivo é criado no aparelho e só vai para o local que você escolher. A senha não é armazenada pelo aplicativo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Entendi", color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupPasswordDialog(action: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    val isCreate = action == "create"
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.imePadding().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (isCreate) "Proteger backup" else "Abrir backup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(
                if (isCreate) "Use uma senha que você consiga lembrar. Sem ela, o backup protegido não poderá ser restaurado."
                else "Digite a senha usada ao criar o backup. Backups antigos sem senha podem ser restaurados deixando este campo vazio.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Senha ${if (isCreate) "(opcional)" else ""}") },
                visualTransformation = PasswordVisualTransformation()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onConfirm(password) }, shape = RoundedCornerShape(14.dp)) {
                    Text(if (isCreate) "Continuar" else "Restaurar")
                }
            }
        }
    }
}

/**
 * Folha de edição de um comprovante.
 *
 * A escolha em [PunchChoice] define **o que este comprovante representa** e é a única fonte de verdade
 * sobre o que será salvo:
 * - entrada e saída no mesmo registro (com "Inverter" se o OCR leu a ordem ao contrário);
 * - só entrada ou só saída: a outra batida virá em outro comprovante, às vezes no dia seguinte. Se ela
 *   nunca foi registrada, marque "Esquecimento": o dia fica resolvido, sem horas;
 * - sem horário: só com falta, folga ou atestado.
 *
 * Comprovante que ainda não tem tipo (ex.: o OCR leu um só horário) começa sem escolha, e o botão de
 * salvar fica desligado até o usuário decidir.
 *
 * Ao confirmar, só o horário do tipo escolhido é gravado. Era o bug P19: em "Só entrada", o campo de
 * saída (vazio ou herdado) também era salvo, gerando horas e pendências erradas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditReceiptSheet(
    receipt: Receipt,
    onDismiss: () -> Unit,
    onSave: (workDate: String, entryTime: String?, exitTime: String?, occurrence: DayOccurrence?) -> Unit
) {
    var workDateField by remember(receipt.id) {
        mutableStateOf(TextFieldValue(receipt.workDate, TextRange(receipt.workDate.length)))
    }
    val workDate = workDateField.text
    var choice by remember(receipt.id) { mutableStateOf(punchChoiceOf(receipt)) }
    var occurrence by remember(receipt.id) { mutableStateOf(receipt.occurrence) }
    // Entrada e saída têm campos separados. Nos modos de uma batida só, o campo mostrado é o da batida
    // escolhida, e o horário acompanha a troca entre entrada e saída (ver `pick`).
    val initialEntry = receipt.entryTime ?: if (receipt.exitTime == null) {
        receipt.workTime.takeIf { it.isNotBlank() }.orEmpty()
    } else {
        ""
    }
    var entryField by remember(receipt.id) {
        mutableStateOf(TextFieldValue(initialEntry, TextRange(initialEntry.length)))
    }
    val initialExit = receipt.exitTime ?: ""
    var exitField by remember(receipt.id) {
        mutableStateOf(TextFieldValue(initialExit, TextRange(initialExit.length)))
    }
    val entryTime = entryField.text
    val exitTime = exitField.text

    val selectedChoice = choice
    val selectedOccurrence = occurrence
    val dateValid = isValidWorkDate(workDate)
    val entryValid = entryTime.isBlank() || WorkStatsCalculator.parseClockTime(entryTime) != null
    val exitValid = exitTime.isBlank() || WorkStatsCalculator.parseClockTime(exitTime) != null
    val timesValid = when (selectedChoice) {
        PunchChoice.BOTH -> entryTime.isNotBlank() && exitTime.isNotBlank() && entryValid && exitValid
        PunchChoice.ENTRY -> entryTime.isNotBlank() && entryValid
        PunchChoice.EXIT -> exitTime.isNotBlank() && exitValid
        PunchChoice.NONE -> true
        null -> false
    }
    // Sem horário, a ocorrência é obrigatória (falta, folga ou atestado). Com uma batida só, é opcional
    // (esquecimento). Com entrada e saída, não existe ocorrência.
    val occurrenceValid = selectedChoice?.let { choice ->
        selectedOccurrence?.let { occ -> choice.occurrences.contains(occ) }
            ?: (choice != PunchChoice.NONE)
    } ?: false
    val canSave = dateValid && timesValid && occurrenceValid
    val duration = if (selectedChoice == PunchChoice.BOTH) {
        WorkStatsCalculator.durationMinutes(entryTime, exitTime)
    } else {
        null
    }

    fun pick(next: PunchChoice) {
        // Entre "Só entrada" e "Só saída", o horário digitado acompanha a troca.
        if (next == PunchChoice.ENTRY && entryField.text.isBlank()) {
            entryField = exitField.copy(selection = TextRange(exitField.text.length))
        }
        if (next == PunchChoice.EXIT && exitField.text.isBlank()) {
            exitField = entryField.copy(selection = TextRange(entryField.text.length))
        }
        choice = next
        // Só mantém a ocorrência que combina com a nova escolha.
        occurrence = occurrence?.takeIf { it in next.occurrences }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Revisar comprovante",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Escolha como este comprovante registra o ponto. Só é salvo o que estiver escolhido aqui.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = workDateField,
                onValueChange = { workDateField = maskedFieldValue(it, ::formatDateInput) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Data") },
                placeholder = { Text("dd/mm/aaaa") },
                supportingText = {
                    Text(
                        when {
                            workDate.isBlank() -> "Ex.: 22/10/2026"
                            !dateValid -> "Use o formato dd/mm/aaaa"
                            else -> "Data reconhecida"
                        }
                    )
                },
                isError = workDate.isNotBlank() && !dateValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Text(
                "Registro de ponto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PunchChoice.entries) { option ->
                    FilterChip(
                        selected = selectedChoice == option,
                        onClick = { pick(option) },
                        label = { Text(option.label) },
                        shape = RoundedCornerShape(12.dp),
                        colors = organizeChipColors(),
                        border = organizeChipBorder(selectedChoice == option)
                    )
                }
            }
            if (selectedChoice == null) {
                Text(
                    "Escolha uma opção acima para poder salvar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            when (selectedChoice) {
                PunchChoice.ENTRY -> PunchTimeField("Entrada", entryField, entryValid) { entryField = it }
                PunchChoice.EXIT -> PunchTimeField("Saída", exitField, exitValid) { exitField = it }
                PunchChoice.BOTH -> {
                    PunchTimeField("Entrada", entryField, entryValid) { entryField = it }
                    PunchTimeField("Saída", exitField, exitValid) { exitField = it }
                    TextButton(
                        onClick = {
                            val previousEntry = entryField
                            entryField = exitField
                            exitField = previousEntry
                        },
                        enabled = entryTime.isNotBlank() || exitTime.isNotBlank()
                    ) {
                        Icon(Icons.Outlined.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Inverter entrada e saída")
                    }
                    val invalidShift = duration == null && entryTime.isNotBlank() && exitTime.isNotBlank()
                    Text(
                        when {
                            duration != null -> "Total calculado: ${formatMinutes(duration)}"
                            invalidShift -> "Jornada inválida: confira os horários (entre 1 min e 16h)."
                            else -> "Informe entrada e saída para calcular as horas."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            duration != null -> MaterialTheme.colorScheme.primary
                            invalidShift -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
                PunchChoice.NONE -> {
                    Text(
                        "Motivo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PunchChoice.NONE.occurrences.toList()) { option ->
                            FilterChip(
                                selected = selectedOccurrence == option,
                                onClick = { occurrence = option },
                                label = { Text(occurrenceLabel(option)) },
                                shape = RoundedCornerShape(12.dp),
                                colors = organizeChipColors(),
                                border = organizeChipBorder(selectedOccurrence == option)
                            )
                        }
                    }
                    Text(
                        if (selectedOccurrence == null) {
                            "Sem horário, escolha o motivo para salvar."
                        } else {
                            "O dia fica resolvido, sem horas."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                null -> Unit
            }

            if (selectedChoice == PunchChoice.ENTRY || selectedChoice == PunchChoice.EXIT) {
                Text(
                    "Outra batida",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(DayOccurrence.FORGOT_PUNCH)) { option ->
                        FilterChip(
                            selected = selectedOccurrence == option,
                            onClick = {
                                occurrence = if (selectedOccurrence == option) null else option
                            },
                            label = { Text(occurrenceLabel(option)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = organizeChipColors(),
                            border = organizeChipBorder(selectedOccurrence == option)
                        )
                    }
                }
                Text(
                    "Use quando a outra batida nunca foi registrada. O dia fica resolvido, sem horas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val chosen = selectedChoice
                        if (chosen != null) {
                            // Só persiste o horário do tipo escolhido (ver KDoc de EditReceiptSheet).
                            val keepEntry = chosen == PunchChoice.BOTH || chosen == PunchChoice.ENTRY
                            val keepExit = chosen == PunchChoice.BOTH || chosen == PunchChoice.EXIT
                            onSave(
                                workDate.trim(),
                                entryTime.trim().takeIf { keepEntry && it.isNotBlank() },
                                exitTime.trim().takeIf { keepExit && it.isNotBlank() },
                                selectedOccurrence
                            )
                        }
                    },
                    enabled = canSave,
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

/** Campo de horário `hh:mm` da folha de edição, com máscara e erro quando o horário não é válido. */
@Composable
private fun PunchTimeField(
    label: String,
    field: TextFieldValue,
    isValid: Boolean,
    onValueChange: (TextFieldValue) -> Unit
) {
    OutlinedTextField(
        value = field,
        onValueChange = { onValueChange(maskedFieldValue(it, ::formatTimeInput)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text("hh:mm") },
        supportingText = { Text(if (field.text.isBlank()) "Ex.: 10:20" else "Horário em hh:mm") },
        isError = field.text.isNotBlank() && !isValid,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
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


