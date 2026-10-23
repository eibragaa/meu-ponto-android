package br.com.jean.meuponto.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateParserTest {

    private val releaseUrl =
        "https://github.com/eibragaa/meu-ponto-android/releases/tag/v0.5.2"

    private val releaseNotesV051 = """
        # Meu Ponto v0.5.1

        ## Correções

        - Turno noturno agora é pareado entre comprovantes e entre dias; a duração é creditada no dia da
          entrada (P16).
        - Editor de horário mantém o cursor e salva apenas o tipo escolhido ("Só entrada" ou "Só saída"),
          evitando horários invertidos e batidas duplicadas (P18/P19).
        - Duração de jornada unificada, com o mesmo teto de 16 horas para comprovante único e para
          comprovantes separados.

        ## Interno

        - Código reorganizado em `domain/`, `data/`, `ui/` e `updater/`.
        - 35 testes unitários cobrindo cálculo de horas, máscaras, formatação e comparação de versão.
        - Padrão de engenharia e documentação do projeto atualizados.

        ## Observações

        - Esta versão ainda não inclui as melhorias de teclado no editor (E1) nem o motivo detalhado do
          "Revisar" (E3); elas ficam para a próxima versão.
    """.trimIndent()

    private fun apk(
        name: String = "Meu-Ponto-0.5.2.apk",
        url: String = "https://example.com/$name",
        size: Long = 1024L,
        digest: String = ""
    ) = ApkAsset(name = name, downloadUrl = url, size = size, digest = digest)

    private fun release(
        tag: String = "v0.5.2",
        name: String = "Meu Ponto 0.5.2",
        body: String = "Correções e melhorias",
        assets: List<ApkAsset> = listOf(apk())
    ) = ReleasePayload(
        tag = tag,
        name = name,
        body = body,
        releaseUrl = releaseUrl,
        assets = assets
    )

    @Test
    fun `release com apk mais nova devolve versao notas e url`() {
        val result = evaluateRelease(release(), currentVersion = "0.5.1")

        assertTrue(result is UpdateCheckResult.Available)
        val info = (result as UpdateCheckResult.Available).info
        assertEquals("0.5.2", info.versionName)
        assertEquals("Meu Ponto 0.5.2", info.releaseName)
        assertEquals("Correções e melhorias", info.releaseNotes)
        assertEquals("https://example.com/Meu-Ponto-0.5.2.apk", info.apkUrl)
        assertEquals("Meu-Ponto-0.5.2.apk", info.apkFileName)
        assertEquals(1024L, info.apkSize)
        assertEquals(releaseUrl, info.releaseUrl)
    }

    @Test
    fun `release propaga o digest publicado do apk`() {
        val digest = "sha256:" + "a".repeat(64)
        val result = evaluateRelease(
            release(assets = listOf(apk(digest = digest))),
            currentVersion = "0.5.1"
        )

        val info = (result as UpdateCheckResult.Available).info
        assertEquals(digest, info.apkDigest)
    }

    @Test
    fun `release sem digest deixa o campo vazio`() {
        val result = evaluateRelease(release(), currentVersion = "0.5.1")

        val info = (result as UpdateCheckResult.Available).info
        assertEquals("", info.apkDigest)
    }

    @Test
    fun `prefere apk com nome do produto entre varios assets`() {
        val assets = listOf(
            apk(name = "app-release.apk", url = "https://example.com/app-release.apk"),
            apk(name = "Meu-Ponto-0.5.2.apk", url = "https://example.com/Meu-Ponto-0.5.2.apk")
        )

        val result = evaluateRelease(release(assets = assets), currentVersion = "0.5.1")

        val info = (result as UpdateCheckResult.Available).info
        assertEquals("Meu-Ponto-0.5.2.apk", info.apkFileName)
    }

    @Test
    fun `sem nome do produto usa o primeiro apk com url`() {
        val assets = listOf(
            ApkAsset(name = "notas.txt", downloadUrl = "https://example.com/notas.txt", size = 10L),
            apk(name = "app-release.apk", url = "https://example.com/app-release.apk"),
            apk(name = "outro.apk", url = "https://example.com/outro.apk")
        )

        val result = evaluateRelease(release(assets = assets), currentVersion = "0.5.1")

        val info = (result as UpdateCheckResult.Available).info
        assertEquals("app-release.apk", info.apkFileName)
    }

    @Test
    fun `release sem apk devolve no apk`() {
        val assets = listOf(
            ApkAsset(name = "notas.txt", downloadUrl = "https://example.com/notas.txt", size = 10L),
            apk(name = "sem-url.apk", url = "")
        )

        val result = evaluateRelease(release(assets = assets), currentVersion = "0.5.1")

        assertEquals(UpdateCheckResult.Failed(UpdateFailure.NO_APK), result)
    }

    @Test
    fun `release sem assets devolve no apk`() {
        val result = evaluateRelease(release(assets = emptyList()), currentVersion = "0.5.1")

        assertEquals(UpdateCheckResult.Failed(UpdateFailure.NO_APK), result)
    }

    @Test
    fun `versao igual ou menor devolve up to date`() {
        assertEquals(
            UpdateCheckResult.UpToDate,
            evaluateRelease(release(tag = "v0.5.1"), currentVersion = "0.5.1")
        )
        assertEquals(
            UpdateCheckResult.UpToDate,
            evaluateRelease(release(tag = "v0.5.0"), currentVersion = "0.5.1")
        )
        assertEquals(
            UpdateCheckResult.UpToDate,
            evaluateRelease(release(tag = "v0.5.1", assets = emptyList()), currentVersion = "0.5.1")
        )
    }

    @Test
    fun `versao maior devolve available mesmo com tag sem v`() {
        val result = evaluateRelease(release(tag = "0.5.2"), currentVersion = "0.5.1")

        assertTrue(result is UpdateCheckResult.Available)
        assertEquals("0.5.2", (result as UpdateCheckResult.Available).info.versionName)
    }

    @Test
    fun `tag vazia devolve resposta invalida`() {
        val result = evaluateRelease(release(tag = "v"), currentVersion = "0.5.1")

        assertEquals(UpdateCheckResult.Failed(UpdateFailure.INVALID_RESPONSE), result)
    }

    @Test
    fun `corpo vazio mantem available sem notas`() {
        val result = evaluateRelease(
            release(body = "", name = ""),
            currentVersion = "0.5.1"
        )

        val info = (result as UpdateCheckResult.Available).info
        assertEquals("", info.releaseNotes)
        assertEquals("Versão 0.5.2", info.releaseName)
    }

    @Test
    fun `notas acima do limite sao truncadas`() {
        val body = "x".repeat(MAX_RELEASE_NOTES_CHARS + 50)

        val result = evaluateRelease(release(body = body), currentVersion = "0.5.1")

        val info = (result as UpdateCheckResult.Available).info
        assertEquals(MAX_RELEASE_NOTES_CHARS, info.releaseNotes.length)
    }

    @Test
    fun `mapeia status http para falha tipada`() {
        listOf(401, 403, 429).forEach { status ->
            assertEquals(UpdateFailure.RATE_LIMITED, failureForHttpStatus(status))
        }
        listOf(500, 502, 503, 599).forEach { status ->
            assertEquals(UpdateFailure.SERVER_ERROR, failureForHttpStatus(status))
        }
        listOf(400, 418, 302).forEach { status ->
            assertEquals(UpdateFailure.SERVER_ERROR, failureForHttpStatus(status))
        }
    }

    @Test
    fun `reconhece 404 como release inexistente`() {
        assertTrue(isReleaseMissingStatus(404))
        assertFalse(isReleaseMissingStatus(200))
        assertFalse(isReleaseMissingStatus(500))
    }

    @Test
    fun `banner da v0 5 1 resume correcoes de forma generica`() {
        val summary = summarizeReleaseForBanner(releaseNotesV051)

        assertEquals("Correções e melhorias", summary.text)
        assertFalse(summary.isFeature)
    }

    @Test
    fun `banner destaca ate duas novidades`() {
        val notes = """
            # Meu Ponto v0.6.0

            ## Novidades

            - Backup automático com senha
            - Tema novo para o editor
            - Item que não deve aparecer no banner

            ## Correções e melhorias

            - Ajustes internos
        """.trimIndent()

        val summary = summarizeReleaseForBanner(notes)

        assertEquals("Backup automático com senha · Tema novo para o editor", summary.text)
        assertTrue(summary.isFeature)
    }

    @Test
    fun `banner usa correcoes genericas quando so ha melhorias`() {
        val notes = """
            ## Melhorias

            - Estabilidade
            - Desempenho
        """.trimIndent()

        val summary = summarizeReleaseForBanner(notes)

        assertEquals("Correções e melhorias", summary.text)
        assertFalse(summary.isFeature)
    }

    @Test
    fun `resume ignora titulo nome do release e cabecalho de secao`() {
        val notes = """
            # Meu Ponto v0.5.1
            Meu Ponto v0.5.1

            ## Correções

            - Item importante
        """.trimIndent()

        assertEquals("Item importante", summarizeReleaseNotes(notes))
    }

    @Test
    fun `resume ignora todos os cabecalhos de secao conhecidos`() {
        val headers = listOf(
            "Correções",
            "Correcoes",
            "Novidades",
            "Interno",
            "Observações",
            "Observacoes"
        )

        headers.forEach { header ->
            assertEquals(
                "cabeçalho: $header",
                "Item relevante",
                summarizeReleaseNotes("$header\n\n- Item relevante")
            )
        }
    }

    @Test
    fun `resume remove marcadores de lista e negrito`() {
        assertEquals("Item importante", summarizeReleaseNotes("- **Item importante**"))
        assertEquals("Aviso", summarizeReleaseNotes("* Aviso"))
        assertEquals("Correções importantes", summarizeReleaseNotes("Correções **importantes**"))
    }

    @Test
    fun `resume trunca com reticencias`() {
        val summary = summarizeReleaseNotes("Correções e melhorias importantes", maxChars = 10)

        assertEquals(10, summary.length)
        assertTrue(summary.endsWith("…"))
    }

    @Test
    fun `resume devolve vazio para notas em branco ou so titulo`() {
        assertEquals("", summarizeReleaseNotes(""))
        assertEquals("", summarizeReleaseNotes("\n \n"))
        assertEquals("", summarizeReleaseNotes("# Meu Ponto v0.5.1\n\nv0.5.1"))
        assertEquals("", summarizeReleaseNotes("texto", maxChars = 0))
    }
}
