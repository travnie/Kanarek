package com.kanarek.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuoteWidgetTest {
    @Test
    fun `daily selection advances through the bundled order without state`() {
        val quotes =
            listOf(
                QuoteItem("one", "a"),
                QuoteItem("two", "b"),
                QuoteItem("three", "c"),
            )

        assertEquals("one", quoteForDay(quotes, 0)?.quote)
        assertEquals("two", quoteForDay(quotes, 1)?.quote)
        assertEquals("three", quoteForDay(quotes, 2)?.quote)
        assertEquals("one", quoteForDay(quotes, 3)?.quote)
    }

    @Test
    fun `typography grows with widget size and shrinks for long quotes`() {
        val compact = quoteWidgetTypography(widthDp = 180, heightDp = 80, quoteLength = 80)
        val expanded = quoteWidgetTypography(widthDp = 340, heightDp = 220, quoteLength = 80)
        val long = quoteWidgetTypography(widthDp = 340, heightDp = 220, quoteLength = 500)

        assertTrue(expanded.quoteSp > compact.quoteSp)
        assertTrue(long.quoteSp < expanded.quoteSp)
        assertTrue(long.quoteSp >= 13f)
        assertTrue(long.authorSp >= 11f)
    }

    @Test
    fun `quote shrinks to fit, then ellipsizes so the author stays visible`() {
        val short = quoteWidgetTypography(widthDp = 320, heightDp = 180, quoteLength = 40)
        val medium = quoteWidgetTypography(widthDp = 320, heightDp = 180, quoteLength = 150)
        val cramped = quoteWidgetTypography(widthDp = 180, heightDp = 110, quoteLength = 150)

        assertEquals(24f, short.quoteSp)
        assertTrue(medium.quoteSp < short.quoteSp)
        assertEquals(13f, cramped.quoteSp)
        // 110dp tall: only a few lines fit above the author, not the whole quote.
        assertTrue(cramped.maxLines in 1..4)
        // Minimum resize height: one line, not a forced second one that pushes the author out.
        assertEquals(1, quoteWidgetTypography(widthDp = 180, heightDp = 80, quoteLength = 150).maxLines)
        // 60dp resize minimum: no room for quote + author, so the quote gets the space alone.
        val tiny = quoteWidgetTypography(widthDp = 180, heightDp = 60, quoteLength = 150)
        assertEquals(false, tiny.showAuthor)
        assertEquals(1, tiny.maxLines)
        // Blank author: its row isn't reserved, so the quote gets more lines.
        assertTrue(
            quoteWidgetTypography(180, 80, 150, hasAuthor = false).maxLines >
                quoteWidgetTypography(180, 80, 150).maxLines,
        )
        // Larger system font: smaller sp chosen for the same box.
        assertTrue(
            quoteWidgetTypography(320, 180, 150, fontScale = 1.3f).quoteSp < medium.quoteSp,
        )
    }

    @Test
    fun `wikiquote resolver accepts existing pages and rejects missing pages`() {
        val found =
            wikiquotePageUrl(
                "Albert Einstein",
                """{"query":{"pages":{"736":{"pageid":736,"title":"Albert Einstein"}}}}""",
            )
        val missing =
            wikiquotePageUrl(
                "Nobody",
                """{"query":{"pages":{"-1":{"ns":0,"title":"Nobody","missing":""}}}}""",
            )

        assertEquals("https://en.wikiquote.org/wiki/Albert_Einstein", found)
        assertEquals(null, missing)
    }

    @Test
    fun `quote parser drops blank entries`() {
        val parsed =
            parseQuotes(
                """{"quotes":[{"quote":"","author":"Nobody"},{"quote":"Hello","author":"World"}]}""",
            )

        assertEquals(listOf(QuoteItem("Hello", "World")), parsed)
    }
}
