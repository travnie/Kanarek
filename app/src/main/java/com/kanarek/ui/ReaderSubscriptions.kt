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
            val fallback = runCatching {
                URI(url).host?.removePrefix("www.")
            }.getOrNull().orEmpty()
            ReaderSubscription(url, name.ifBlank { fallback.ifBlank { url } })
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
