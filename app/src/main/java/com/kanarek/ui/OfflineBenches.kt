package com.kanarek.ui

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream

// One typed source of truth for the asset paths and their per-Bench origins.
internal enum class BenchKind(val slug: String) {
    CODE("codebench"), DOC("docbench"), STREAM("streambench"),
}

internal object OfflineBenches {
    private const val BACKEND = "streambench.travny.workers.dev"

    fun origin(kind: BenchKind) = "https://${kind.slug}.appassets.androidplatform.net"
    fun url(kind: BenchKind) = origin(kind) + "/bench/portable.html"

    fun isBundled(context: Context, kind: BenchKind): Boolean =
        runCatching {
            context.assets.open("benches/${kind.slug}/portable.html").use { it.read() >= 0 }
        }.getOrDefault(false)

    fun destination(bundled: Boolean, kind: BenchKind, hosted: String) =
        if (bundled) url(kind) else hosted

    fun isTrustedPage(uri: Uri, kind: BenchKind) =
        uri.scheme == "https" &&
            uri.host == "${kind.slug}.appassets.androidplatform.net" &&
            uri.port == -1 && uri.path == "/bench/portable.html" &&
            uri.query == null

    fun isAllowedStreamBackend(uri: Uri): Boolean =
        uri.scheme == "https" && uri.host == BACKEND && uri.port == -1 &&
            (uri.path == "/api" || uri.path?.startsWith("/api/") == true)

    fun loader(context: Context, kind: BenchKind): WebViewAssetLoader {
        return WebViewAssetLoader.Builder()
            .setDomain("${kind.slug}.appassets.androidplatform.net")
            .addPathHandler("/bench/", WebViewAssetLoader.PathHandler { path ->
                if (path != "portable.html") return@PathHandler null
                runCatching {
                    WebResourceResponse(
                        "text/html", "UTF-8",
                        context.assets.open("benches/${kind.slug}/portable.html"),
                    )
                }.getOrNull()
            }).build()
    }

    fun intercept(loader: WebViewAssetLoader, uri: Uri, kind: BenchKind): WebResourceResponse? {
        if (uri.scheme in setOf("data", "blob", "content")) return null
        if (isTrustedPage(uri, kind)) {
            loader.shouldInterceptRequest(uri)?.let { return it }
        }
        // Existing restricted Worker, not a native general-purpose proxy.
        if (kind == BenchKind.STREAM && isAllowedStreamBackend(uri)) return null
        return WebResourceResponse(
            "text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)),
        ).apply { setStatusCodeAndReasonPhrase(403, "Forbidden") }
    }
}
