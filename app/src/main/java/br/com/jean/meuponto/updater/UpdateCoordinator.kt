package br.com.jean.meuponto.updater

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado observável pela tela do updater.
 *
 * [available] é a release encontrada e continua preenchida depois de fechar o modal, para o banner
 * poder reabri-lo. [bannerVersion]/[bannerSummary] ficam vazios quando o usuário dispensou o banner
 * daquela versão. [whatsNew] contém as notas salvas antes de instalar e é consumido na abertura.
 */
internal data class UpdateUiState(
    val available: AppUpdateInfo? = null,
    val bannerVersion: String? = null,
    val bannerSummary: String? = null,
    val bannerIsFeature: Boolean = false,
    val isPromptVisible: Boolean = false,
    val isChecking: Boolean = false,
    val manualMessage: String? = null,
    val whatsNew: PendingNotes? = null
)

/**
 * Dono do fluxo de atualização durante a vida do processo.
 *
 * O modal é mostrado a cada verificação bem-sucedida que encontra versão nova; fechá-lo ("Mais
 * tarde") mantém o banner. Dispensar o banner vale só para aquela versão e não impede o modal de
 * aparecer na próxima abertura. Uma verificação por vez evita requisições duplicadas.
 *
 * As dependências (preferências, versão, consulta, relógio e escopo) são injetadas para o fluxo ser
 * testável sem Android.
 */
internal class UpdateCoordinator(
    private val preferences: UpdatePreferences,
    private val currentVersion: String,
    private val fetch: suspend (String) -> UpdateCheckResult,
    private val clock: () -> Long,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var checkJob: Job? = null
    private var coldStartDone = false

    /**
     * Primeira execução do processo: consome as notas pendentes da versão instalada e verifica.
     *
     * Chamadas seguintes são ignoradas, inclusive depois de uma rotação de tela.
     */
    fun onColdStart() {
        if (coldStartDone) return
        coldStartDone = true

        val pendingNotes = preferences.consumePendingNotes(currentVersion)
        if (pendingNotes != null) {
            _state.update { it.copy(whatsNew = pendingNotes) }
        }
        startCheck(CheckTrigger.ColdStart)
    }

    /** Volta ao foreground; antes da primeira abertura apenas encaminha para [onColdStart]. */
    fun onForeground() {
        if (!coldStartDone) {
            onColdStart()
            return
        }
        if (!shouldCheck(CheckTrigger.Foreground, clock(), preferences.lastCheckAt())) return
        startCheck(CheckTrigger.Foreground)
    }

    /** Verificação manual: ignora o intervalo e sempre mostra o resultado ao usuário. */
    fun checkNow() {
        startCheck(CheckTrigger.Manual)
    }

    /** Abre o modal da release já encontrada (ação "Ver novidades" do banner). */
    fun showPrompt() {
        if (_state.value.available == null) return
        _state.update { it.copy(isPromptVisible = true) }
    }

    fun dismissPrompt() {
        _state.update { it.copy(isPromptVisible = false) }
    }

    /**
     * Dispensa o banner da versão atual, mas mantém [UpdateUiState.available] e o modal disponível.
     *
     * A dispensa é gravada nas preferências; a release só some do banner até surgir outra versão.
     */
    fun dismissBanner() {
        val current = _state.value
        val version = current.bannerVersion ?: current.available?.versionName
        if (!version.isNullOrBlank()) {
            preferences.dismissVersion(version)
        }
        _state.update { it.copy(bannerVersion = null, bannerSummary = null, bannerIsFeature = false) }
    }

    /**
     * Guarda as notas da versão antes de abrir o instalador; a próxima abertura consome e mostra
     * como "O que há de novo".
     */
    fun onInstallerReady(info: AppUpdateInfo) {
        preferences.savePendingNotes(PendingNotes(version = info.versionName, notes = info.releaseNotes))
    }

    fun dismissWhatsNew() {
        _state.update { it.copy(whatsNew = null) }
    }

    fun consumeManualMessage() {
        _state.update { it.copy(manualMessage = null) }
    }

    private fun startCheck(trigger: CheckTrigger) {
        if (checkJob?.isActive == true) return
        val manual = trigger == CheckTrigger.Manual

        // Registra a tentativa mesmo quando falha, para o throttle do foreground valer.
        preferences.recordCheck(clock())
        _state.update { it.copy(isChecking = true) }

        checkJob = scope.launch {
            try {
                applyResult(fetch(currentVersion), manual)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A consulta tipada já trata os casos esperados; isto evita derrubar o app em uma
                // falha inesperada e mantém a mensagem acionável na verificação manual.
                if (manual) {
                    _state.update {
                        it.copy(manualMessage = "Não foi possível verificar atualizações. Tente de novo.")
                    }
                }
            } finally {
                _state.update { it.copy(isChecking = false) }
            }
        }
    }

    private fun applyResult(result: UpdateCheckResult, manual: Boolean) {
        when (result) {
            is UpdateCheckResult.Available -> applyAvailable(result.info)
            is UpdateCheckResult.Failed -> {
                if (manual) {
                    _state.update { it.copy(manualMessage = updateFailureMessage(result.reason)) }
                }
                // Falha automática é silenciosa e não apaga um banner já visível.
            }
            UpdateCheckResult.UpToDate -> {
                _state.update { current ->
                    current.copy(
                        available = null,
                        bannerVersion = null,
                        bannerSummary = null,
                        bannerIsFeature = false,
                        isPromptVisible = false,
                        manualMessage = if (manual) {
                            "Você já está na versão mais recente (v$currentVersion)."
                        } else {
                            current.manualMessage
                        }
                    )
                }
            }
        }
    }

    private fun applyAvailable(info: AppUpdateInfo) {
        val showBanner = shouldShowBanner(info.versionName, preferences.dismissedVersion())
        val summary = summarizeReleaseForBanner(info.releaseNotes)
        _state.update { current ->
            current.copy(
                available = info,
                bannerVersion = if (showBanner) info.versionName else null,
                bannerSummary = if (showBanner) summary.text else null,
                bannerIsFeature = showBanner && summary.isFeature,
                isPromptVisible = true,
                manualMessage = null
            )
        }
    }
}

/**
 * Instância única do updater no processo.
 *
 * PENDÊNCIA ENG-1: solução temporária até o estado de tela ir para um `ViewModel`; hoje ela evita
 * recriar o coordenador a cada rotação. Guarda apenas o `applicationContext` através das
 * preferências, então não retém Activity.
 */
internal object UpdateRuntime {
    @Volatile
    private var instance: UpdateCoordinator? = null

    fun get(context: Context, currentVersion: String): UpdateCoordinator {
        return instance ?: synchronized(this) {
            instance ?: UpdateCoordinator(
                preferences = UpdatePreferences(SharedPreferencesUpdateStore(context.applicationContext)),
                currentVersion = currentVersion,
                fetch = AppUpdater::checkForUpdates,
                clock = System::currentTimeMillis,
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            ).also { instance = it }
        }
    }
}
