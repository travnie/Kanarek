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
 */
internal fun quoteWidgetTypography(
    widthDp: Int,
    heightDp: Int,
    quoteLength: Int,
    fontScale: Float = 1f,
): QuoteWidgetTypography {
    val cap =
        when {
            widthDp >= 300 && heightDp >= 180 -> 24f
            widthDp >= 220 && heightDp >= 100 -> 20f
            else -> 17f
        }
    var quoteSp = cap
    // Sizes are applied as sp, so estimate with the user's font scale.
    while (quoteSp > MIN_QUOTE_SP && !fits(quoteSp * fontScale, widthDp, heightDp, quoteLength)) {
        quoteSp -= STEP_SP
    }
    quoteSp = quoteSp.coerceAtLeast(MIN_QUOTE_SP)
    val linesWithAuthor = (textHeight(quoteSp * fontScale, heightDp) / lineHeight(quoteSp * fontScale)).toInt()
    if (linesWithAuthor < 1) {
        // Quote alone: list + item padding only.
        val lines = ((heightDp - 28f - 8f) / lineHeight(quoteSp * fontScale)).toInt()
        return QuoteWidgetTypography(quoteSp, authorSp(quoteSp), lines.coerceAtLeast(1), showAuthor = false)
    }
    return QuoteWidgetTypography(quoteSp = quoteSp, authorSp = authorSp(quoteSp), maxLines = linesWithAuthor)
}

private fun authorSp(quoteSp: Float) = (quoteSp * 0.72f).coerceAtLeast(11f)

private fun fits(
    quoteSp: Float,
    widthDp: Int,
    heightDp: Int,
    quoteLength: Int,
): Boolean {
    // quote_widget.xml list padding is 18dp per side.
    val textWidth = widthDp - 36f
    val textHeight = textHeight(quoteSp, heightDp)
    if (textWidth <= 0f || textHeight <= 0f) return false
    val charsPerLine = textWidth / (quoteSp * 0.52f)
    val lines = ceil(quoteLength * WRAP_SLACK / charsPerLine)
    return lines * lineHeight(quoteSp) <= textHeight
}

// List padding (14dp top/bottom), item padding (4dp top/bottom), author margin and its single
// line (quote_widget_item.xml keeps the author to one ellipsized line).
private fun textHeight(
    quoteSp: Float,
    heightDp: Int,
) = heightDp - 28f - 8f - 8f - authorSp(quoteSp) * 1.3f

private fun lineHeight(quoteSp: Float) = quoteSp * 1.17f + 2f

private const val MIN_QUOTE_SP = 13f
private const val STEP_SP = 0.5f
private const val WRAP_SLACK = 1.12f
