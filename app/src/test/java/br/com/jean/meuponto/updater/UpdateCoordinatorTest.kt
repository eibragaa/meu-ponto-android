package br.com.jean.meuponto.updater

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateCoordinatorTest {

    private class InMemoryUpdateStore : UpdateStore {
        private val strings = mutableMapOf<String, String>()
        private val longs = mutableMapOf<String, Long>()

        override fun getString(key: String): String? =
            strings[key]

        override fun putString(key: String, value: String?) {
            if (value == null) {
                strings.remove(key)
            } else {
                strings[key] = value
            }
        }

        override fun getLong(key: String, defaultValue: Long): Long =
            longs[key] ?: defaultValue

        override fun putLong(key: String, value: Long) {
            longs[key] = value
        }
    }

    private val currentVersion = "0.5.1"
    private val update = AppUpdateInfo(
        versionName = "0.5.2",
        releaseName = "Meu Ponto 0.5.2",
        releaseNotes = "Correções e melhorias",
        apkUrl = "https://example.com/Meu-Ponto-0.5.2.apk",
        apkFileName = "Meu-Ponto-0.5.2.apk",
        apkSize = 1024L
    )

    private lateinit var store: InMemoryUpdateStore
    private lateinit var preferences: UpdatePreferences
    private lateinit var coordinator: UpdateCoordinator
    private var now = 1_000_000L
    private var fetchCalls = 0
    private var nextResult: UpdateCheckResult = UpdateCheckResult.UpToDate

    @Before
    fun setUp() {
        store = InMemoryUpdateStore()
        preferences = UpdatePreferences(store)
        coordinator = UpdateCoordinator(
            preferences = preferences,
            currentVersion = currentVersion,
            fetch = {
                fetchCalls++
                nextResult
            },
            clock = { now },
            scope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `abertura fria verifica uma vez por processo e registra a tentativa`() {
        coordinator.onColdStart()
        coordinator.onColdStart()

        assertEquals(1, fetchCalls)
        assertEquals(now, preferences.lastCheckAt())
    }

    @Test
    fun `atualizacao disponivel abre o modal e preenche o banner`() {
        nextResult = UpdateCheckResult.Available(update)

        coordinator.onColdStart()

        val state = coordinator.state.value
        assertEquals(update, state.available)
        assertTrue(state.isPromptVisible)
        assertEquals(update.versionName, state.bannerVersion)
        assertEquals("Correções e melhorias", state.bannerSummary)
        assertFalse(state.isChecking)
    }

    @Test
    fun `fechar o modal mantem o banner e a proxima verificacao reabre`() {
        nextResult = UpdateCheckResult.Available(update)
        coordinator.onColdStart()

        coordinator.dismissPrompt()
        assertFalse(coordinator.state.value.isPromptVisible)
        assertEquals(update.versionName, coordinator.state.value.bannerVersion)

        coordinator.checkNow()
        assertTrue(coordinator.state.value.isPromptVisible)
        assertEquals(2, fetchCalls)
    }

    @Test
    fun `dispensar o banner esconde so aquela versao e nao impede o modal`() {
        nextResult = UpdateCheckResult.Available(update)
        coordinator.onColdStart()

        coordinator.dismissBanner()

        assertNull(coordinator.state.value.bannerVersion)
        assertNull(coordinator.state.value.bannerSummary)
        assertEquals(update.versionName, preferences.dismissedVersion())

        coordinator.checkNow()
        assertNull(coordinator.state.value.bannerVersion)
        assertTrue(coordinator.state.value.isPromptVisible)
    }

    @Test
    fun `falha manual preenche mensagem e registra a tentativa`() {
        nextResult = UpdateCheckResult.Failed(UpdateFailure.OFFLINE)

        coordinator.checkNow()

        assertEquals(
            updateFailureMessage(UpdateFailure.OFFLINE),
            coordinator.state.value.manualMessage
        )
        assertEquals(now, preferences.lastCheckAt())
        assertFalse(coordinator.state.value.isChecking)
    }

    @Test
    fun `falha automatica e silenciosa e nao apaga o banner existente`() {
        nextResult = UpdateCheckResult.Available(update)
        coordinator.onColdStart()
        assertEquals(update.versionName, coordinator.state.value.bannerVersion)

        nextResult = UpdateCheckResult.Failed(UpdateFailure.SERVER_ERROR)
        now += AUTO_CHECK_INTERVAL_MS
        coordinator.onForeground()

        assertNull(coordinator.state.value.manualMessage)
        assertEquals(update.versionName, coordinator.state.value.bannerVersion)
    }

    @Test
    fun `verificacao manual sem novidade informa a versao e limpa o banner`() {
        nextResult = UpdateCheckResult.Available(update)
        coordinator.onColdStart()

        nextResult = UpdateCheckResult.UpToDate
        coordinator.checkNow()

        val state = coordinator.state.value
        assertNull(state.available)
        assertNull(state.bannerVersion)
        assertFalse(state.isPromptVisible)
        assertEquals(
            "Você já está na versão mais recente (v$currentVersion).",
            state.manualMessage
        )
    }

    @Test
    fun `preparar o instalador guarda as notas da versao`() {
        coordinator.onInstallerReady(update)

        assertEquals(
            PendingNotes(version = update.versionName, notes = update.releaseNotes),
            preferences.consumePendingNotes(update.versionName)
        )
    }

    @Test
    fun `notas pendentes da versao instalada viram o que ha de novo na abertura`() {
        val pending = PendingNotes(version = currentVersion, notes = "Notas da versão")
        preferences.savePendingNotes(pending)

        coordinator.onColdStart()

        assertEquals(pending, coordinator.state.value.whatsNew)

        coordinator.dismissWhatsNew()
        assertNull(coordinator.state.value.whatsNew)
    }

    @Test
    fun `verificacao em andamento nao dispara requisicao duplicada`() = runBlocking {
        val deferred = CompletableDeferred<UpdateCheckResult>()
        var calls = 0
        val blockingCoordinator = UpdateCoordinator(
            preferences = preferences,
            currentVersion = currentVersion,
            fetch = {
                calls++
                deferred.await()
            },
            clock = { now },
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        blockingCoordinator.checkNow()
        blockingCoordinator.checkNow()

        assertEquals(1, calls)

        deferred.complete(UpdateCheckResult.Available(update))
        assertTrue(blockingCoordinator.state.value.isPromptVisible)
    }
}
