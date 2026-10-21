package com.kanarek.data

enum class ArticleListFilter { ALL, UNREAD, SAVED }

data class ArticleState(
    val readIds: Set<String> = emptySet(),
    val savedArticles: List<NewsItem> = emptyList(),
    val savedFeedByArticle: Map<String, String> = emptyMap(),
    val hiddenIds: Set<String> = emptySet(),
    val offlineArticles: Map<String, OfflineArticleContent> = emptyMap(),
) {
    val savedIds: Set<String> = savedArticles.mapTo(linkedSetOf()) { ArticleStates.id(it) }
    val offlineArticleBytes: Long = offlineArticles.values.sumOf(::offlineArticleStorageBytes)

    fun isRead(item: NewsItem): Boolean = ArticleStates.id(item) in readIds

    fun isSaved(item: NewsItem): Boolean = ArticleStates.id(item) in savedIds

    fun offlineArticle(item: NewsItem): OfflineArticleContent? = offlineArticles[ArticleStates.id(item)]
}

/** Plain reader text persisted with a saved-article snapshot. It is rendered as text, never HTML. */
data class OfflineArticleContent(
    val title: String,
    val author: String?,
    val imageUrl: String?,
    val content: String,
    val wordCount: Int,
    val storedAtMillis: Long,
) {
    fun asCleanArticle(): CleanArticle =
        CleanArticle(
            title = title,
            author = author,
            imageUrl = imageUrl,
            content = content,
            wordCount = wordCount,
        )
}

object ArticleStates {
    fun id(item: NewsItem): String = item.link.trim()

    fun visible(
        feedItems: List<NewsItem>,
        state: ArticleState,
        filter: ArticleListFilter,
        query: String = "",
        sources: Set<String> = emptySet(),
    ): List<NewsItem> {
        val candidates =
            when (filter) {
                ArticleListFilter.SAVED -> state.savedArticles.sortedByDescending { it.publishedAtMillis ?: 0L }
                ArticleListFilter.ALL,
                ArticleListFilter.UNREAD,
                -> feedItems
            }
        val normalizedQuery = query.trim()
        val normalizedSources =
            sources
                .map(::sourceKey)
                .filterTo(linkedSetOf(), String::isNotEmpty)

        return candidates
            .distinctBy(::id)
            .filterNot { id(it) in state.hiddenIds }
            .filter { filter != ArticleListFilter.UNREAD || id(it) !in state.readIds }
            .filter { normalizedSources.isEmpty() || sourceKey(it.source) in normalizedSources }
            .filter { item ->
                normalizedQuery.isEmpty() ||
                    sequenceOf(item.title, item.source, item.summary)
                        .any { it.contains(normalizedQuery, ignoreCase = true) }
            }
    }

    private fun sourceKey(source: String): String = source.trim().lowercase()
}

/** Persisted byte cost of the Base64-url-without-padding offline fields in a v2 saved record. */
fun offlineArticleStorageBytes(offline: OfflineArticleContent): Long =
    (
        offline.storedAtMillis.toString().length +
            base64UrlLength(offline.title) +
            base64UrlLength(offline.author.orEmpty()) +
            base64UrlLength(offline.imageUrl.orEmpty()) +
            base64UrlLength(offline.content) +
            offline.wordCount.toString().length
    ).toLong()

private fun base64UrlLength(value: String): Int {
    val bytes = value.encodeToByteArray().size
    return (bytes * 4 + 2) / 3
}
