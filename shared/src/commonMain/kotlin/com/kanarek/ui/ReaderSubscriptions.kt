package com.kanarek.ui

import com.kanarek.data.NewsItem

/** Subscription identity is always the saved feed URL, not a name from an article. */
data class ReaderSubscription(
    val url: String,
    val title: String,
    val favoriteSource: String? = null,
)

fun readerSubscriptions(
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
            ReaderSubscription(
                url = url,
                title = name.ifBlank { subscriptionFallbackTitle(url) },
                favoriteSource = name.takeIf(String::isNotBlank),
            )
        }

/** This exact feed list is passed to the synchronizer; UI and tests share one rule. */
fun readerActiveFeeds(
    savedFeeds: List<String>,
    selectedFeedUrl: String?,
): List<String> =
    selectedFeedUrl?.takeIf(savedFeeds::contains)?.let(::listOf) ?: savedFeeds

/** Persisted channel ownership survives the preview and feed-cache retention limits. */
fun savedArticlesFromSubscription(
    savedArticles: List<NewsItem>,
    selectedFeedUrl: String,
    savedFeedByArticle: Map<String, String>,
    cachedByFeed: Map<String, List<NewsItem>>,
): List<NewsItem> {
    val oldLinks = cachedByFeed[selectedFeedUrl].orEmpty()
        .mapTo(hashSetOf()) { it.link.trim() }
    return savedArticles.filter { article ->
        when (savedFeedByArticle[article.link.trim()]) {
            selectedFeedUrl -> true
            null -> article.link.trim() in oldLinks
            else -> false
        }
    }
}

/** Human-readable labels while a subscribed feed has no cached articles. */
private fun subscriptionFallbackTitle(url: String): String {
    val scheme = url.substringBefore("://")
    if (scheme != "https" && scheme != "http") return url
    val remainder = url.substringAfter("://")
    val host = remainder.substringBefore('/').substringBefore('?')
        .substringBefore(':').removePrefix("www.")
    if (host.isBlank()) return url
    val filename = remainder.substringAfter('/', "").substringBefore('?')
        .trimEnd('/').substringAfterLast('/')
    if (host == "raw.githubusercontent.com" && filename.startsWith("feed_")) {
        return filename.removePrefix("feed_").substringBeforeLast('.')
            .replace('_', ' ').uppercase()
    }
    return host
}
