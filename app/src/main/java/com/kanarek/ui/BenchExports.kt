package com.kanarek.ui

import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream

internal data class BenchExport(val name: String, val mime: String, val bytes: ByteArray)

/** Bounded one-transfer protocol; only call after origin and main-frame checks. */
internal class BenchExportReceiver {
    companion object {
        const val MAX_BYTES = 32 * 1024 * 1024
        private const val MAX_CHUNK = 32 * 1024
    }

    private var stream: ByteArrayOutputStream? = null
    private var expectedSize = 0
    private var filename = ""
    private var mime = ""

    fun reset() {
        stream = null
        expectedSize = 0
        filename = ""
        mime = ""
    }

    fun accept(message: String): BenchExport? {
        require(message.length <= 90_000) { "Oversized export message" }
        val event = JSONObject(message)
        require(event.optInt("v") == 1)
        return when (event.optString("type")) {
            "start" -> {
                require(stream == null)
                val size = event.optInt("size", -1)
                require(size in 0..MAX_BYTES)
                filename = event.optString("name", "export.bin")
                    .substringAfterLast('/').substringAfterLast('\\')
                    .replace(Regex("[\u0000-\u001F\u007F]"), "")
                    .take(120).ifBlank { "export.bin" }
                mime = event.optString("mime").substringBefore(';').take(100)
                    .takeIf { it.matches(Regex("[A-Za-z0-9.+_/-]+")) }
                    ?: "application/octet-stream"
                expectedSize = size
                stream = ByteArrayOutputStream(minOf(size, 1024 * 1024))
                null
            }
            "chunk" -> {
                val sink = checkNotNull(stream)
                val encoded = event.getString("data")
                require(encoded.length <= 45_000)
                val decoded = Base64.decode(encoded, Base64.NO_WRAP)
                require(decoded.size in 1..MAX_CHUNK)
                require(sink.size() + decoded.size <= expectedSize)
                sink.write(decoded)
                null
            }
            "finish" -> {
                val sink = checkNotNull(stream)
                require(sink.size() == expectedSize)
                BenchExport(filename, mime, sink.toByteArray()).also { reset() }
            }
            else -> throw IllegalArgumentException("Unsupported export event")
        }
    }
}

/** Intercepts generated Blob downloads, including detached programmatic links. */
internal val benchExportJavaScript = """
(() => {
  if (!window.KanarekExport || window.__kanarekExportInstalled) return;
  window.__kanarekExportInstalled = true;
  const send = data => window.KanarekExport.postMessage(JSON.stringify({v: 1, ...data}));
  let busy = false;
  const eligible = a => a instanceof HTMLAnchorElement && a.hasAttribute('download') &&
    (a.href.startsWith('blob:') || a.href.startsWith('data:'));
  async function transfer(a) {
    if (busy) { send({type: 'error'}); return; }
    busy = true;
    try {
      const response = await fetch(a.href);
      if (!response.ok) throw new Error('Unable to read export');
      const blob = await response.blob();
      if (blob.size > 33554432) throw new Error('Export too large');
      send({type: 'start', name: a.download || 'export.bin', mime: blob.type, size: blob.size});
      for (let offset = 0; offset < blob.size; offset += 32768) {
        const bytes = new Uint8Array(await blob.slice(offset, offset + 32768).arrayBuffer());
        send({type: 'chunk', data: btoa(String.fromCharCode(...bytes))});
      }
      send({type: 'finish'});
    } catch (_) {
      send({type: 'error'});
    } finally {
      busy = false;
    }
  }
  const originalClick = HTMLAnchorElement.prototype.click;
  HTMLAnchorElement.prototype.click = function() {
    if (eligible(this)) { void transfer(this); return; }
    return originalClick.call(this);
  };
  document.addEventListener('click', event => {
    const anchor = event.target?.closest?.('a[download]');
    if (!eligible(anchor)) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    void transfer(anchor);
  }, true);
})();
""".trimIndent()
