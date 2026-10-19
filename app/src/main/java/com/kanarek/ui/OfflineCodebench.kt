package com.kanarek.ui

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream

// Codebench's single-file build is staged from a pinned twojstar commit.
// Missing asset (e.g. a plain F-Droid build): retain the hosted preview.
internal object OfflineCodebench {
    const val URL = "https://appassets.androidplatform.net/bench/portable.html"
    private const val ASSET = "benches/codebench/portable.html"

    fun isBundled(context: Context): Boolean =
        runCatching { context.assets.open(ASSET).use { it.read() >= 0 } }.getOrDefault(false)

    fun destination(bundled: Boolean, hostedUrl: String): String =
        if (bundled) URL else hostedUrl

    fun isTrustedPage(uri: Uri): Boolean =
        uri.scheme == "https" &&
            uri.host == "appassets.androidplatform.net" &&
            uri.port == -1 &&
            uri.path == "/bench/portable.html" &&
            uri.query == null

    fun loader(context: Context): WebViewAssetLoader =
        WebViewAssetLoader.Builder()
            .addPathHandler("/bench/", WebViewAssetLoader.PathHandler { path ->
                if (path.trimStart('/') != "portable.html") return@PathHandler null
                runCatching {
                    WebResourceResponse("text/html", "UTF-8", context.assets.open(ASSET))
                }.getOrNull()
            })
            .build()

    // Offline page gets only its embedded bundle. No remote fetch fallback.
    fun intercept(loader: WebViewAssetLoader, uri: Uri): WebResourceResponse? {
        if (uri.scheme in setOf("data", "blob", "content")) return null
        if (isTrustedPage(uri)) {
            loader.shouldInterceptRequest(uri)?.let { return it }
        }
        return WebResourceResponse(
            "text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)),
        ).apply { setStatusCodeAndReasonPhrase(403, "Forbidden") }
    }
}
