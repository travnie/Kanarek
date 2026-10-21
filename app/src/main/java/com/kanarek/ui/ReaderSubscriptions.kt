package com.kanarek.ui

import com.kanarek.data.NewsItem
import java.net.URI

/** Subscription identity is always the saved feed URL, not a name from an article. */
internal data class ReaderSubscription(
    val url: String,
    val title: String,
)

internal fun readerSubscriptions(
    savedFeeds: List<String>,
    itemsByFeed: Map<String, List<NewsItem>>,
): List<ReaderSubscription> =
    savedFeeds
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .map { url ->
            val name = itemsByFeed[url]
                .orEmpty()
                .firstOrNull { it.source.isNotBlank() }
                ?.source
                ?.trim()
                .orEmpty()
            ReaderSubscription(url, name.ifBlank { subscriptionFallbackTitle(url) })
        }

/** Reads by subscription URL, so two feeds with the same publisher title stay distinct. */
internal fun readerSubscriptionItems(
    selectedUrl: String?,
    itemsByFeed: Map<String, List<NewsItem>>,
    allItems: List<NewsItem>,
): List<NewsItem> =
    if (selectedUrl == null) allItems else itemsByFeed[selectedUrl].orEmpty()

internal fun savedArticlesFromSubscription(
    savedArticles: List<NewsItem>,
    subscriptionItems: List<NewsItem>,
): List<NewsItem> {
    val links = subscriptionItems.mapTo(hashSetOf()) { it.link.trim() }
    return savedArticles.filter { it.link.trim() in links }
}

/** Human-readable labels while a subscribed feed has no cached articles. */
private fun subscriptionFallbackTitle(url: String): String {
    val uri = runCatching { URI(url) }.getOrNull() ?: return url
    val host = uri.host?.removePrefix("www.").orEmpty()
    if (host.isBlank()) return url
    val filename = uri.path.orEmpty().trimEnd('/').substringAfterLast('/')
    if (host == "raw.githubusercontent.com" && filename.startsWith("feed_")) {
        return filename.removePrefix("feed_").substringBeforeLast('.')
            .replace('_', ' ').uppercase()
    }
    return host
}
