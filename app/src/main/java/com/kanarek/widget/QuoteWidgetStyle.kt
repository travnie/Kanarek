package com.kanarek.widget

internal data class QuoteWidgetTypography(
    val quoteSp: Float,
    val authorSp: Float,
)

internal fun quoteWidgetTypography(
    widthDp: Int,
    heightDp: Int,
    quoteLength: Int,
): QuoteWidgetTypography {
    val base =
        when {
            widthDp >= 300 && heightDp >= 180 -> 24f
            widthDp >= 220 && heightDp >= 100 -> 20f
            else -> 17f
        }
    val penalty =
        when {
            quoteLength > 420 -> 7f
            quoteLength > 280 -> 5f
            quoteLength > 180 -> 3f
            quoteLength > 110 -> 1.5f
            else -> 0f
        }
    val quoteSp = (base - penalty).coerceAtLeast(13f)
    return QuoteWidgetTypography(
        quoteSp = quoteSp,
        authorSp = (quoteSp * 0.72f).coerceAtLeast(11f),
    )
}
