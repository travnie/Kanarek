package com.kanarek.widget

import kotlin.math.ceil

internal data class QuoteWidgetTypography(
    val quoteSp: Float,
    val authorSp: Float,
    /** Lines the quote may use before ellipsizing, so the author line stays visible. */
    val maxLines: Int = Int.MAX_VALUE,
    /** False at heights (down to the 60dp resize minimum) that can't hold a line plus author. */
    val showAuthor: Boolean = true,
)

/**
 * Largest quote size (from a size-class cap down to [MIN_QUOTE_SP]) whose estimated wrapped
 * height still fits the widget with the author line, so the author isn't pushed off-screen.
 * Estimate only: average glyph width ~0.52em plus word-wrap slack. At the floor, a quote too
 * long for the widget is ellipsized to the lines that fit; tapping opens the full source.
 * Sizes are applied as sp, so all space estimates use the user's [fontScale].
 */
internal fun quoteWidgetTypography(
    widthDp: Int,
    heightDp: Int,
    quoteLength: Int,
    fontScale: Float = 1f,
    hasAuthor: Boolean = true,
): QuoteWidgetTypography {
    val cap =
        when {
            widthDp >= 300 && heightDp >= 180 -> 24f
            widthDp >= 220 && heightDp >= 100 -> 20f
            else -> 17f
        }
    val box = QuoteBox(widthDp, heightDp, fontScale)
    var quoteSp = cap
    while (quoteSp > MIN_QUOTE_SP && !box.fits(quoteSp, quoteLength, hasAuthor)) {
        quoteSp -= STEP_SP
    }
    quoteSp = quoteSp.coerceAtLeast(MIN_QUOTE_SP)
    val linesWithAuthor = if (hasAuthor) box.lines(quoteSp, withAuthor = true) else 0
    if (linesWithAuthor < 1) {
        // No author, or no room for one: the quote gets the whole height.
        return QuoteWidgetTypography(
            quoteSp = quoteSp,
            authorSp = authorSp(quoteSp),
            maxLines = box.lines(quoteSp, withAuthor = false).coerceAtLeast(1),
            showAuthor = false,
        )
    }
    return QuoteWidgetTypography(quoteSp = quoteSp, authorSp = authorSp(quoteSp), maxLines = linesWithAuthor)
}

private fun authorSp(quoteSp: Float) = (quoteSp * 0.72f).coerceAtLeast(11f)

/** Text area of quote_widget.xml + quote_widget_item.xml, in dp; sizes passed in are sp. */
private class QuoteBox(
    widthDp: Int,
    private val heightDp: Int,
    private val fontScale: Float,
) {
    // List padding 18dp per side.
    private val textWidth = widthDp - 36f

    // List padding 14dp top/bottom, item padding 4dp top/bottom; with an author also its 8dp
    // margin and single ellipsized line.
    private fun textHeight(
        quoteSp: Float,
        withAuthor: Boolean,
    ): Float {
        val base = heightDp - 28f - 8f
        return if (withAuthor) base - 8f - authorSp(quoteSp) * fontScale * 1.3f else base
    }

    private fun lineHeight(quoteSp: Float) = quoteSp * fontScale * 1.17f + 2f

    fun lines(
        quoteSp: Float,
        withAuthor: Boolean,
    ): Int = (textHeight(quoteSp, withAuthor) / lineHeight(quoteSp)).toInt()

    fun fits(
        quoteSp: Float,
        quoteLength: Int,
        withAuthor: Boolean,
    ): Boolean {
        val height = textHeight(quoteSp, withAuthor)
        if (textWidth <= 0f || height <= 0f) return false
        val charsPerLine = textWidth / (quoteSp * fontScale * 0.52f)
        return ceil(quoteLength * WRAP_SLACK / charsPerLine) * lineHeight(quoteSp) <= height
    }
}

private const val MIN_QUOTE_SP = 13f
private const val STEP_SP = 0.5f
private const val WRAP_SLACK = 1.12f
