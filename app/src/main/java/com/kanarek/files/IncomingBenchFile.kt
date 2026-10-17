package com.kanarek.files

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

enum class BenchFileTarget { CODE, DOC, STREAM }

data class IncomingBenchFile(
    val id: Long,
    val uri: Uri?,
    val filename: String,
    val mime: String,
    val size: Long?,
    val suggested: BenchFileTarget,
    val inlineText: String? = null,
)

data class PreparedBenchFile(
    val id: Long,
    val filename: String,
    val mime: String,
    val bytes: ByteArray,
    val target: BenchFileTarget,
)

object IncomingBenchFiles {
    // The hosted WebView handoff is intentionally small. Larger files can be
    // selected through Bench's own system file picker; bundled assets come later.
    const val MAX_PREVIEW_BYTES = 4 * 1024 * 1024
    private val nextId = AtomicLong(0)

    fun classify(filename: String, mime: String): BenchFileTarget {
        val name = filename.lowercase()
        val media = mime.lowercase().substringBefore(';')
        if (name.endsWith(".m3u") || name.endsWith(".m3u8") ||
            name.endsWith(".xmltv") ||
            media in setOf("application/vnd.apple.mpegurl", "application/x-mpegurl",
                "audio/x-mpegurl", "audio/mpegurl")
        ) return BenchFileTarget.STREAM
        if (name.endsWith(".png") || name.endsWith(".jpg") ||
            name.endsWith(".jpeg") || name.endsWith(".webp") ||
            name.endsWith(".gif") || name.endsWith(".svg") ||
            media.startsWith("image/")
        ) return BenchFileTarget.CODE
        return BenchFileTarget.DOC
    }

    @Suppress("DEPRECATION")
    fun fromIntent(intent: Intent?, resolver: ContentResolver): IncomingBenchFile? {
        if (intent == null || intent.action !in setOf(Intent.ACTION_VIEW, Intent.ACTION_SEND)) return null
        val uri = (if (intent.action == Intent.ACTION_VIEW) intent.data else {
            if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        }) ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
        if (uri == null && intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.takeIf {
                it.isNotBlank() && it.toByteArray(Charsets.UTF_8).size <= MAX_PREVIEW_BYTES
            } ?: return null
            return IncomingBenchFile(
                nextId.incrementAndGet(), null, "shared-text.txt", "text/plain",
                sharedText.toByteArray(Charsets.UTF_8).size.toLong(), BenchFileTarget.DOC, sharedText,
            )
        }
        if (uri == null) return null
        // No file://, network URL, or arbitrary paths. The sender grants access
        // to the user-selected content:// URI only.
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
        var name: String? = null
        var size: Long? = null
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex).takeIf { it >= 0 }
                }
            }
        }
        val safeName = (name ?: uri.lastPathSegment ?: "document")
            .substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[\\u0000-\\u001F\\u007F]"), "")
            .take(150).ifBlank { "document" }
        val mime = runCatching { resolver.getType(uri) }.getOrNull()
            ?: intent.type ?: "application/octet-stream"
        return IncomingBenchFile(nextId.incrementAndGet(), uri, safeName, mime, size, classify(safeName, mime))
    }

    suspend fun prepare(context: Context, source: IncomingBenchFile, target: BenchFileTarget): PreparedBenchFile =
        withContext(Dispatchers.IO) {
            if (source.size != null && source.size > MAX_PREVIEW_BYTES) {
                throw IOException("File exceeds the 4 MB preview limit. Open it using the Bench file picker.")
            }
            val textBytes = source.inlineText?.toByteArray(Charsets.UTF_8)
            if (textBytes != null) {
                return@withContext PreparedBenchFile(
                    source.id, source.filename, source.mime, textBytes, target,
                )
            }
            val output = ByteArrayOutputStream()
            val uri = source.uri ?: throw IOException("Missing document URI.")
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Could not open the selected document.")
            stream.use { input ->
                val chunk = ByteArray(32 * 1024)
                while (true) {
                    val read = input.read(chunk)
                    if (read < 0) break
                    if (output.size() + read > MAX_PREVIEW_BYTES) {
                        throw IOException("File exceeds the 4 MB preview limit. Open it using the Bench file picker.")
                    }
                    output.write(chunk, 0, read)
                }
            }
            PreparedBenchFile(source.id, source.filename, source.mime, output.toByteArray(), target)
        }

    // No URI, file path or JS native-command interface is exposed to the page.
    // All strings are JSON-quoted and the content is encoded as base64.
    fun importJavascript(payload: PreparedBenchFile): String {
        val inputId = when (payload.target) {
            BenchFileTarget.CODE -> "fileInput"
            BenchFileTarget.STREAM -> if (payload.filename.endsWith(".xmltv", true)) "epgFile" else "playlistFile"
            BenchFileTarget.DOC -> when {
                payload.filename.endsWith(".pdf", true) -> "pdf-input"
                payload.filename.endsWith(".docx", true) -> "docx-to-pdf-input"
                else -> "file-input"
            }
        }
        val extensionMime = if (payload.target == BenchFileTarget.CODE) {
            when (payload.filename.substringAfterLast('.', "").lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "svg" -> "image/svg+xml"
                else -> null
            }
        } else null
        val safeMime = (extensionMime ?: payload.mime).takeIf {
            it.length <= 100 && it.matches(Regex("[A-Za-z0-9.+_/-]+"))
        } ?: "application/octet-stream"
        val encoded = Base64.encodeToString(payload.bytes, Base64.NO_WRAP)
        return """
            (function () {
                const input = document.getElementById(${JSONObject.quote(inputId)});
                if (!input || typeof DataTransfer === 'undefined') return false;
                const decoded = atob(${JSONObject.quote(encoded)});
                const bytes = new Uint8Array(decoded.length);
                for (let i = 0; i < decoded.length; i++) bytes[i] = decoded.charCodeAt(i);
                const file = new File([bytes], ${JSONObject.quote(payload.filename)}, {type: ${JSONObject.quote(safeMime)}});
                const transfer = new DataTransfer();
                transfer.items.add(file);
                if (${JSONObject.quote(inputId)} === "fileInput") {
                    document.querySelector('[data-tab="scan"]')?.click();
                } else if (${JSONObject.quote(inputId)} === "pdf-input" ||
                           ${JSONObject.quote(inputId)} === "docx-to-pdf-input") {
                    document.getElementById("pdf-tab")?.click();
                }
                input.files = transfer.files;
                input.dispatchEvent(new Event("change", {bubbles: true}));
                return true;
            })();
        """.trimIndent()
    }
}
