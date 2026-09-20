package com.kanarek.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedParserTest : FeedParserTestExecutor() {
    private fun parseFeed(xml: String): List<NewsItem> {
        var items = emptyList<NewsItem>()
        runTest { items = FeedParser.parse(xml) }
        return items
    }
    private val rss =
        """
        <?xml version="1.0"?>
        <rss version="2.0"><channel>
          <title>Example News</title>
          <item>
            <title>First &amp; foremost</title>
            <link>https://example.com/a</link>
            <description><![CDATA[<p>Body <b>one</b></p>]]></description>
            <pubDate>Wed, 01 Jan 2020 00:00:00 +0000</pubDate>
            <enclosure url="https://img.example.com/a.jpg" type="image/jpeg"/>
          </item>
          <item>
            <title>Second</title>
            <link>https://example.com/b</link>
            <pubDate>Thu, 02 Jan 2020 00:00:00 +0000</pubDate>
          </item>
        </channel></rss>
        """.trimIndent()

    @Test
    fun parsesRssTitleLinkSummary() {
        val items = parseFeed(rss)
        assertEquals(2, items.size)
        assertEquals("First & foremost", items[0].title)
        assertEquals("https://example.com/a", items[0].link)
        assertEquals("Body one", items[0].summary)
    }

    @Test
    fun derivesSourceFromChannelTitle() {
        assertEquals("Example News", parseFeed(rss)[0].source)
    }

    @Test
    fun derivesSourceFromHostWhenChannelTitleIsMissing() {
        val xml =
            """
            <rss><channel>
              <title></title>
              <item><title>t</title><link>https://www.example.com/story</link></item>
            </channel></rss>
            """.trimIndent()

        assertEquals("example.com", parseFeed(xml)[0].source)
    }

    @Test
    fun parsesRfc822Date() {
        assertEquals(1577836800000L, parseFeed(rss)[0].publishedAtMillis)
    }

    @Test
    fun parsesRfc822DateWithSingleDigitDay() {
        val singleDigitDay =
            """
            <rss><channel><title>S</title>
              <item><title>t</title><link>https://x/1</link><pubDate>Mon, 7 Oct 2024 12:00:00 GMT</pubDate></item>
            </channel></rss>
            """.trimIndent()
        assertEquals(1728302400000L, parseFeed(singleDigitDay)[0].publishedAtMillis)
    }

    @Test
    fun parsesIsoOffsetsAndLocalDate() {
        fun date(value: String): Long? {
            val xml =
                """
                <rss><channel><title>S</title>
                  <item><title>t</title><link>https://x/1</link><pubDate>$value</pubDate></item>
                </channel></rss>
                """.trimIndent()
            return parseFeed(xml)[0].publishedAtMillis
        }

        assertEquals(1728295200000L, date("2024-10-07T12:00:00+02:00"))
        assertEquals(1728295200000L, date("2024-10-07T12:00:00+0200"))
        assertEquals(1728259200000L, date("2024-10-07"))
    }

    @Test
    fun picksEnclosureImage() {
        assertEquals("https://img.example.com/a.jpg", parseFeed(rss)[0].imageUrl)
    }

    @Test
    fun dropsItemsMissingTitleOrLink() {
        val broken =
            """
            <rss><channel><title>X</title>
              <item><link>https://x/1</link></item>
              <item><title>ok</title><link>https://x/2</link></item>
            </channel></rss>
            """.trimIndent()
        val items = parseFeed(broken)
        assertEquals(listOf("https://x/2"), items.map { it.link })
    }

    @Test
    fun parsesAtomEntry() {
        val atom =
            """
            <?xml version="1.0"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>Atom Source</title>
              <entry>
                <title>Hello</title>
                <link rel="alternate" href="https://atom.example/post"/>
                <summary>Short summary</summary>
                <updated>2021-06-15T12:00:00Z</updated>
                <media:content url="https://atom.example/p.png"/>
              </entry>
            </feed>
            """.trimIndent()
        val items = parseFeed(atom)
        assertEquals(1, items.size)
        assertEquals("Hello", items[0].title)
        assertEquals("https://atom.example/post", items[0].link)
        assertEquals("Short summary", items[0].summary)
        assertEquals("https://atom.example/p.png", items[0].imageUrl)
        assertEquals(1623758400000L, items[0].publishedAtMillis)
    }

    @Test
    fun prefersAtomPublishedOverGeneratedUpdated() {
        val atom =
            """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>Atom Source</title>
              <entry>
                <title>Old article</title>
                <link href="https://atom.example/old"/>
                <published>2020-01-02T00:00:00Z</published>
                <updated>2026-07-30T02:33:56Z</updated>
              </entry>
            </feed>
            """.trimIndent()

        assertEquals(1577923200000L, parseFeed(atom)[0].publishedAtMillis)
    }

    @Test
    fun returnsEmptyOnGarbageWithoutThrowing() {
        assertTrue(parseFeed("not xml at all").isEmpty())
        assertTrue(parseFeed("").isEmpty())
    }

    @Test
    fun decodesNumericAndHexEntities() {
        val xml =
            """
            <rss><channel><title>S</title>
              <item><title>A&#65;&#x42;</title><link>https://x/1</link></item>
            </channel></rss>
            """.trimIndent()
        assertEquals("AAB", parseFeed(xml)[0].title)
    }

    @Test
    fun decodesSupplementaryUnicodeNumericEntity() {
        val xml =
            """
            <rss><channel><title>S</title>
              <item><title>Launch &#128640;</title><link>https://x/rocket</link></item>
            </channel></rss>
            """.trimIndent()
        assertEquals("Launch 🚀", parseFeed(xml)[0].title)
    }

    @Test
    fun relativeTimeBucketsInEnglish() {
        val now = 1_000_000_000_000L
        assertEquals("", FeedParser.relativeTime(null, now, "en"))
        assertEquals("just now", FeedParser.relativeTime(now - 30_000, now, "en"))
        assertEquals("1 minute ago", FeedParser.relativeTime(now - 60_000, now, "en"))
        assertEquals("5 minutes ago", FeedParser.relativeTime(now - 5 * 60_000, now, "en"))
        assertEquals("3 hours ago", FeedParser.relativeTime(now - 3 * 3_600_000, now, "en"))
        assertEquals("2 days ago", FeedParser.relativeTime(now - 2 * 86_400_000L, now, "en"))
    }

    @Test
    fun relativeTimeUsesPolishForms() {
        val now = 1_000_000_000_000L
        assertEquals("przed chwilą", FeedParser.relativeTime(now - 30_000, now, "pl"))
        assertEquals("1 minutę temu", FeedParser.relativeTime(now - 60_000, now, "pl"))
        assertEquals("2 minuty temu", FeedParser.relativeTime(now - 2 * 60_000, now, "pl"))
        assertEquals("12 minut temu", FeedParser.relativeTime(now - 12 * 60_000, now, "pl"))
        assertEquals("22 minuty temu", FeedParser.relativeTime(now - 22 * 60_000, now, "pl"))
        assertEquals("1 godzinę temu", FeedParser.relativeTime(now - 3_600_000, now, "pl"))
        assertEquals("5 godzin temu", FeedParser.relativeTime(now - 5 * 3_600_000, now, "pl"))
        assertEquals("2 dni temu", FeedParser.relativeTime(now - 2 * 86_400_000L, now, "pl"))
    }

    @Test
    fun futureDatesAreReportedAsJustNow() {
        val now = 1_000_000_000_000L
        assertEquals("just now", FeedParser.relativeTime(now + 60_000, now, "en"))
    }

    @Test
    fun nullDateWhenUnparseable() {
        val xml =
            """
            <rss><channel><title>S</title>
              <item><title>t</title><link>https://x/1</link><pubDate>not a date</pubDate></item>
            </channel></rss>
            """.trimIndent()
        assertNull(parseFeed(xml)[0].publishedAtMillis)
    }
}
