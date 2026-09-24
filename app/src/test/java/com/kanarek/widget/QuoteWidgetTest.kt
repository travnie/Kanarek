package com.kanarek.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `quote parser drops blank entries`() {
        val parsed =
            parseQuotes(
                """{"quotes":[{"quote":"","author":"Nobody"},{"quote":"Hello","author":"World"}]}""",
            )

        assertEquals(listOf(QuoteItem("Hello", "World")), parsed)
    }
}
