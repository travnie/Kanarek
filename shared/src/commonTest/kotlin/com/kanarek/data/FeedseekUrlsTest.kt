package com.kanarek.data

import kotlin.test.Test
import kotlin.test.assertEquals

class FeedseekUrlsTest {
    private val pagesPap = "https://trvny.github.io/feedseek/pap.xml"

    @Test fun rawMainSnapshotMapsToShortPagesUrl() {
        assertEquals(
            pagesPap,
            canonicalFeedUrl("https://raw.githubusercontent.com/trvny/feedseek/main/feeds/feed_pap.xml"),
        )
    }

    @Test fun legacyPagesNameMapsToShortName() {
        assertEquals(
            "https://trvny.github.io/feedseek/wykop.json",
            canonicalFeedUrl("https://trvny.github.io/feedseek/feed_wykop.json"),
        )
    }

    @Test fun otherUrlsAreUntouched() {
        listOf(
            "https://antyweb.pl/feed/",
            pagesPap,
            "https://raw.githubusercontent.com/other/repo/main/feeds/feed_x.xml",
            "https://raw.githubusercontent.com/trvny/feedseek/main/feeds/nested/feed_x.xml",
        ).forEach { assertEquals(it, canonicalFeedUrl(it)) }
    }
}
