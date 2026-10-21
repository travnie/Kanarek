package com.kanarek.ui

import com.kanarek.data.NewsItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReaderSubscriptionsTest {
    private fun item(title: String, source: String, link: String) =
        NewsItem(
            title = title, link = link, source = source, summary = "",
            imageUrl = null, publishedAtMillis = null,
        )

    @Test fun subscriptionsComeFromConfiguredUrlsEvenWhenFeedHasNoStories() {
        val subscribed = listOf("https://a.test/rss", "https://b.test/feed")
        val options = readerSubscriptions(subscribed, mapOf(subscribed[0] to listOf(
            item("A1", "Daily A", "https://a.test/a1"),
        )))
        assertEquals(subscribed, options.map { it.url })
        assertEquals("Daily A", options[0].title)
        assertEquals("Daily A", options[0].favoriteSource)
        assertEquals(null, options[1].favoriteSource)
        assertEquals("b.test", options[1].title)
    }

    @Test fun feedsWithSamePublisherTitleStaySeparate() {
        val first = "https://example.com/first"
        val second = "https://example.com/second"
        val a = item("A", "Same publisher", "https://example.com/a")
        val b = item("B", "Same publisher", "https://example.com/b")
        val indexed = mapOf(first to listOf(a), second to listOf(b))
        val options = readerSubscriptions(listOf(first, second), indexed)
        assertEquals(listOf("Same publisher", "Same publisher"), options.map { it.title })
        assertEquals(listOf(first), readerActiveFeeds(listOf(first, second), first))
        assertEquals(listOf(second), readerActiveFeeds(listOf(first, second), second))
        assertEquals(listOf(first, second), readerActiveFeeds(listOf(first, second), null))
        assertEquals(listOf(first, second), readerActiveFeeds(listOf(first, second), "unknown"))
        assertEquals(first, options.first().url)
    }

    @Test fun onlySavedStoriesFromSelectedChannelRemain() {
        val a = item("A", "A", "https://example.com/a")
        val b = item("B", "B", "https://example.com/b")
        val mapping = mapOf(a.link to "feed-a", b.link to "feed-b")
        assertEquals(
            listOf(a),
            savedArticlesFromSubscription(listOf(a, b), "feed-a", mapping, emptyMap()),
        )
        assertEquals(
            listOf(a),
            savedArticlesFromSubscription(listOf(a, b), "feed-a", emptyMap(), mapOf(
                "feed-a" to listOf(a),
            )),
        )
    }

    @Test fun feedseekChannelsHaveDistinctLabelsBeforeFirstFetch() {
        val urls = listOf(
            "https://raw.githubusercontent.com/trvny/feedseek/main/feeds/feed_pap.xml",
            "https://raw.githubusercontent.com/trvny/feedseek/main/feeds/feed_reuters.xml",
        )
        assertEquals(
            listOf("PAP", "REUTERS"),
            readerSubscriptions(urls, emptyMap()).map { it.title },
        )
    }

    @Test fun duplicateFeedUrlsAreNotListedTwice() {
        val options = readerSubscriptions(
            listOf(" https://example.com/rss ", "https://example.com/rss", ""),
            emptyMap(),
        )
        assertEquals(1, options.size)
    }
}
