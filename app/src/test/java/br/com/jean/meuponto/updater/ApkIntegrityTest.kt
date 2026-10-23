package br.com.jean.meuponto.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ApkIntegrityTest {

    private val abcSha256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    private val emptySha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

    @Test
    fun `normaliza digest com e sem prefixo e ignora maiusculas`() {
        assertEquals(abcSha256, normalizeSha256("sha256:$abcSha256"))
        assertEquals(abcSha256, normalizeSha256("SHA256:${abcSha256.uppercase()}"))
        assertEquals(abcSha256, normalizeSha256("  ${abcSha256.uppercase()}  "))
    }

    @Test
    fun `rejeita digest vazio malformado ou fora do tamanho`() {
        assertNull(normalizeSha256(null))
        assertNull(normalizeSha256(""))
        assertNull(normalizeSha256("sha256:"))
        assertNull(normalizeSha256("a".repeat(SHA256_HEX_LENGTH - 1)))
        assertNull(normalizeSha256("g".repeat(SHA256_HEX_LENGTH)))
    }

    @Test
    fun `compara digest aceitando prefixo e caixa diferentes`() {
        assertTrue(sha256Matches("sha256:${abcSha256.uppercase()}", abcSha256))
        assertTrue(sha256Matches(abcSha256.uppercase(), "sha256:$abcSha256"))
    }

    @Test
    fun `nao aceita digest diferente ausente ou invalido`() {
        assertFalse(sha256Matches("sha256:${"b".repeat(64)}", abcSha256))
        assertFalse(sha256Matches(null, abcSha256))
        assertFalse(sha256Matches("", abcSha256))
        assertFalse(sha256Matches("sha256:xyz", abcSha256))
    }

    @Test
    fun `calcula sha 256 de arquivo temporario`() {
        val file = File.createTempFile("meu-ponto-sha256", ".bin")
        try {
            file.writeText("abc")
            assertEquals(abcSha256, sha256OfFile(file))

            file.writeText("")
            assertEquals(emptySha256, sha256OfFile(file))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `calcula sha 256 do certificado a partir dos bytes`() {
        assertEquals(abcSha256, certificateSha256("abc".toByteArray()))
    }

    @Test
    fun `considera assinaturas iguais quando os conjuntos coincidem`() {
        assertTrue(signaturesMatch(setOf(abcSha256), setOf(abcSha256)))
        assertTrue(signaturesMatch(setOf(abcSha256.uppercase()), setOf(abcSha256)))
    }

    @Test
    fun `considera assinaturas diferentes quando os conjuntos divergem`() {
        assertFalse(signaturesMatch(setOf(abcSha256), setOf("b".repeat(64))))
        assertFalse(signaturesMatch(setOf(abcSha256, "b".repeat(64)), setOf(abcSha256)))
    }

    @Test
    fun `conjunto vazio nunca conta como assinatura compativel`() {
        assertFalse(signaturesMatch(emptySet(), emptySet()))
        assertFalse(signaturesMatch(setOf(abcSha256), emptySet()))
        assertFalse(signaturesMatch(emptySet(), setOf(abcSha256)))
    }
}
