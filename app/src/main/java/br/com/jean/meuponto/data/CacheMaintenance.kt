package br.com.jean.meuponto.data


import android.content.Context
import java.io.File

/**
 * Limpeza de caches temporários gerados pelo app (PDFs, APKs de update, temporários de backup).
 */

internal fun cleanupUpdateAndPdfCaches(context: Context) {
    runCatching {
        File(context.cacheDir, "shared").deleteRecursively()
        File(context.cacheDir, "updates").deleteRecursively()
        context.cacheDir.listFiles()
            ?.filter { it.name.startsWith("meu-ponto-backup-") || it.name.startsWith("meu-ponto-restore-") }
            ?.forEach { it.deleteRecursively() }
        context.cacheDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("restore_") }
            ?.forEach { it.deleteRecursively() }
    }
}
