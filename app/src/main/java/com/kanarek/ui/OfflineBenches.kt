package com.kanarek.ui

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream

// Same pinned twojstar source, isolated per-Bench pages.
internal object OfflineBenches {
    const val ORIGIN = "https://appassets.androidplatform.net"
    private val kinds = setOf("codebench", "docbench", "streambench")
    private const val BACKEND = "streambench.travny.workers.dev"

    fun url(kind: String) = "$ORIGIN/benches/$kind/portable.html"

    fun isBundled(context: Context, kind: String): Boolean =
        kind in kinds && runCatching {
            context.assets.open("benches/$kind/portable.html").use { it.read() >= 0 }
        }.getOrDefault(false)

    fun destination(bundled: Boolean, kind: String, hosted: String) =
        if (bundled && kind in kinds) url(kind) else hosted

    fun isTrustedPage(uri: Uri, kind: String) =
        kind in kinds && uri.scheme == "https" &&
            uri.host == "appassets.androidplatform.net" &&
            uri.port == -1 && uri.path == "/benches/$kind/portable.html" &&
            uri.query == null

    fun isAllowedStreamBackend(uri: Uri): Boolean =
        uri.scheme == "https" && uri.host == BACKEND && uri.port == -1 &&
            (uri.path == "/api" || uri.path?.startsWith("/api/") == true)

    fun loader(context: Context): WebViewAssetLoader =
        WebViewAssetLoader.Builder().addPathHandler(
            "/benches/",
            WebViewAssetLoader.PathHandler { path ->
                val parts = path.split('/')
                if (parts.size != 2 || parts[0] !in kinds || parts[1] != "portable.html") {
                    return@PathHandler null
                }
                runCatching {
                    WebResourceResponse(
                        "text/html", "UTF-8",
                        context.assets.open("benches/" + parts[0] + "/portable.html"),
                    )
                }.getOrNull()
            },
        ).build()

    fun intercept(loader: WebViewAssetLoader, uri: Uri, kind: String): WebResourceResponse? {
        if (uri.scheme in setOf("data", "blob", "content")) return null
        if (isTrustedPage(uri, kind)) {
            loader.shouldInterceptRequest(uri)?.let { return it }
        }
        // Existing restricted Worker, not a native general-purpose proxy.
        if (kind == "streambench" && isAllowedStreamBackend(uri)) return null
        return WebResourceResponse(
            "text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)),
        ).apply { setStatusCodeAndReasonPhrase(403, "Forbidden") }
    }
}
