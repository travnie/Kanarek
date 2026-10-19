package com.kanarek.ui

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Pending exports live in private cache, not Activity-owned byte arrays.
 * The opaque token can survive Activity recreation through rememberSaveable.
 */
internal object BenchExportStore {
    private const val DIRECTORY = "bench-exports"
    private const val STALE_AGE_MS = 24L * 60 * 60 * 1000
    private val tokenPattern = Regex("^bench-export-[0-9a-f-]{36}\\.bin$")

    private fun folder(context: Context): File = File(context.cacheDir, DIRECTORY)

    private fun source(context: Context, token: String): File {
        require(tokenPattern.matches(token)) { "Invalid export token" }
        return File(folder(context), token)
    }

    fun stage(context: Context, export: BenchExport): String {
        require(export.bytes.size <= BenchExportReceiver.MAX_BYTES)
        val directory = folder(context)
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create export cache" }
        val cutoff = System.currentTimeMillis() - STALE_AGE_MS
        directory.listFiles()?.forEach { file ->
            if (tokenPattern.matches(file.name) && file.lastModified() < cutoff) file.delete()
        }
        val token = "bench-export-" + UUID.randomUUID().toString() + ".bin"
        val file = source(context, token)
        try {
            file.outputStream().buffered().use { it.write(export.bytes) }
        } catch (failure: Exception) {
            file.delete()
            throw failure
        }
        return token
    }

    fun writeTo(context: Context, token: String, target: Uri) {
        val file = source(context, token)
        check(file.isFile) { "Pending export was removed" }
        context.contentResolver.openOutputStream(target, "wt")?.use { sink ->
            file.inputStream().buffered().use { it.copyTo(sink) }
        } ?: error("Document provider refused export")
    }

    fun discard(context: Context, token: String) {
        source(context, token).delete()
    }
}
