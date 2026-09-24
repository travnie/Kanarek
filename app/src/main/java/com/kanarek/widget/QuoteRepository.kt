package com.kanarek.widget

import android.content.Context
import com.kanarek.R
import org.json.JSONObject
import java.time.LocalDate

internal data class QuoteItem(
    val quote: String,
    val author: String,
)

internal object QuoteRepository {
    const val GIST_URL = "https://gist.github.com/trvny/167d2271e3cf7d21e118aa7d906a7d2c"

    @Volatile
    private var cached: List<QuoteItem>? = null

    fun today(context: Context): QuoteItem? =
        quoteForDay(
            quotes = load(context),
            epochDay = LocalDate.now().toEpochDay(),
        )

    private fun load(context: Context): List<QuoteItem> {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: run {
                val raw =
                    context.resources
                        .openRawResource(R.raw.quotes)
                        .bufferedReader()
                        .use { it.readText() }
                parseQuotes(raw).also { cached = it }
            }
        }
    }
}

internal fun parseQuotes(raw: String): List<QuoteItem> {
    val array = JSONObject(raw).optJSONArray("quotes") ?: return emptyList()
    return buildList(array.length()) {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val quote = item.optString("quote").trim()
            if (quote.isBlank()) continue
            add(
                QuoteItem(
                    quote = quote,
                    author = item.optString("author").trim(),
                ),
            )
        }
    }
}

internal fun quoteForDay(
    quotes: List<QuoteItem>,
    epochDay: Long,
): QuoteItem? =
    if (quotes.isEmpty()) {
        null
    } else {
        quotes[Math.floorMod(epochDay, quotes.size.toLong()).toInt()]
    }

