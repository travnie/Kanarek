package com.kanarek.widget

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.kanarek.data.WebLinks
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resolves an author against Wikiquote only when the user taps the widget.
 *
 * The widget stays fully offline and does no background network work. A tap performs one
 * bounded MediaWiki lookup, opens the exact English Wikiquote page when it exists, and
 * otherwise falls back to the source gist, matching Feedseek's daily_quote behavior.
 */
class QuoteRedirectActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val author = intent?.getStringExtra(EXTRA_AUTHOR).orEmpty().trim()
        if (author.isBlank()) {
            openAndFinish(QuoteRepository.GIST_URL)
            return
        }

        Thread {
            val target = resolveWikiquote(author) ?: QuoteRepository.GIST_URL
            runOnUiThread { openAndFinish(target) }
        }.start()
    }

    private fun openAndFinish(url: String) {
        if (WebLinks.isHttpOrHttps(url)) {
            runCatching {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
        finish()
    }

    companion object {
        const val EXTRA_AUTHOR = "com.kanarek.widget.extra.AUTHOR"

        private const val WIKIQUOTE_API = "https://en.wikiquote.org/w/api.php"
        private const val WIKIQUOTE_WIKI = "https://en.wikiquote.org/wiki/"
        private const val NETWORK_TIMEOUT_MS = 5_000

        internal fun resolveWikiquote(author: String): String? {
            val apiUrl =
                Uri.parse(WIKIQUOTE_API)
                    .buildUpon()
                    .appendQueryParameter("action", "query")
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("redirects", "1")
                    .appendQueryParameter("titles", author)
                    .build()
                    .toString()
            val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = NETWORK_TIMEOUT_MS
                readTimeout = NETWORK_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Kanarek-Android/quote-widget")
            }
            return try {
                if (connection.responseCode !in 200..299) return null
                val payload =
                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }
                wikiquotePageUrl(author, payload)
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }
    }
}

internal fun wikiquotePageUrl(
    author: String,
    rawJson: String,
): String? {
    val pages =
        runCatching {
            JSONObject(rawJson)
                .optJSONObject("query")
                ?.optJSONObject("pages")
        }.getOrNull() ?: return null

    val keys = pages.keys()
    while (keys.hasNext()) {
        val page = pages.optJSONObject(keys.next()) ?: continue
        if (page.has("missing") || page.optInt("pageid", -1) <= 0) continue
        val title = page.optString("title", author).trim().ifBlank { author }
        return "https://en.wikiquote.org/wiki/" +
            Uri.encode(title.replace(" ", "_"), "_")
    }
    return null
}
