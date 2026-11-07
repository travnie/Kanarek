package com.kanarek.data

import kotlin.test.Test
import kotlin.test.assertEquals

class FeedseekUrlsTest {
    @Test fun rawMainSnapshotMapsToShortPagesUrl() {
        assertEquals(
            "https://trvny.github.io/feedseek/pap.xml",
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
            "https://trvny.github.io/feedseek/pap.xml",
            "https://raw.githubusercontent.com/other/repo/main/feeds/feed_x.xml",
            "https://raw.githubusercontent.com/trvny/feedseek/main/feeds/nested/feed_x.xml",
        ).forEach { assertEquals(it, canonicalFeedUrl(it)) }
    }

    @Test fun mappingDropsDuplicatesKeepingOrder() {
        assertEquals(
            listOf("https://trvny.github.io/feedseek/pap.xml", "https://a.test/rss"),
            canonicalFeedUrls(listOf(
                "https://raw.githubusercontent.com/trvny/feedseek/main/feeds/feed_pap.xml",
                "https://a.test/rss",
                "https://trvny.github.io/feedseek/pap.xml",
            )),
        )
    }
}
