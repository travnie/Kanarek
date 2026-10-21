package com.kanarek.ui

import com.kanarek.data.NewsItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertEquals(listOf(a), readerSubscriptionItems(first, indexed, listOf(a, b)))
        assertEquals(listOf(b), readerSubscriptionItems(second, indexed, listOf(a, b)))
        assertEquals(listOf(a, b), readerSubscriptionItems(null, indexed, listOf(a, b)))
        assertTrue(readerSubscriptionItems("https://unknown.test", indexed, listOf(a, b)).isEmpty())
    }

    @Test fun onlySavedStoriesFromSelectedChannelRemain() {
        val a = item("A", "A", "https://example.com/a")
        val b = item("B", "B", "https://example.com/b")
        assertEquals(listOf(a), savedArticlesFromSubscription(listOf(a, b), listOf(a)))
    }

    @Test fun duplicateFeedUrlsAreNotListedTwice() {
        val options = readerSubscriptions(
            listOf(" https://example.com/rss ", "https://example.com/rss", ""),
            emptyMap(),
        )
        assertEquals(1, options.size)
    }
}
