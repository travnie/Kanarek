package com.kanarek.data

import com.prof18.rssparser.RssParser
import com.prof18.rssparser.exception.RssParsingException
import kotlin.time.Clock

/** Normalizes RSS, Atom and RDF feeds into Kanarek's small reader model. */
object FeedParser {
    private val parser = RssParser()

    suspend fun parse(xml: String): List<NewsItem> {
        if (xml.isBlank()) return emptyList()

        val channel =
            try {
                parser.parse(xml)
            } catch (_: RssParsingException) {
                return emptyList()
            }

        val source = plainText(channel.title.orEmpty())
        return channel.items.mapNotNull { item ->
            val title = item.title?.let(::plainText)?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            val link = item.link?.trim()?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            val summary = plainText(item.description ?: item.content.orEmpty()).takeCodePoints(280)
            val imageUrl =
                item.image?.trim()?.takeIf(String::isNotBlank)
                    ?: item.rawMediaContent
                        ?.takeIf { media ->
                            media.medium.equals("image", ignoreCase = true) ||
                                media.type?.startsWith("image/", ignoreCase = true) == true ||
                                media.url?.matches(IMAGE_URL) == true
                        }
                        ?.url
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                    ?: item.rawEnclosure
                        ?.takeIf { enclosure ->
                            enclosure.type?.startsWith("image/", ignoreCase = true) == true ||
                                enclosure.url?.matches(IMAGE_URL) == true
                        }
                        ?.url
                        ?.trim()
                        ?.takeIf(String::isNotBlank)

            NewsItem(
                title = title,
                link = link,
                summary = summary,
                imageUrl = imageUrl,
                source = source.ifBlank { urlHostLabel(link).orEmpty() },
                publishedAtMillis = item.pubDate?.trim()?.takeIf(String::isNotEmpty)?.let(::parseFeedDate),
            )
        }
    }

    // Entities after tag stripping: Google News & co. escape their HTML once more, so after
    // the XML layer the text still carries "&nbsp;" and friends.
    private fun plainText(value: String): String =
        decodeEntities(value.replace(TAGS, " "))
            .replace(WHITESPACE, " ")
            .trim()

    // Common references only (the full HTML table is ~2k names); unknown ones stay literal.
    internal fun decodeEntities(value: String): String =
        ENTITY.replace(value) { match ->
            val name = match.groupValues[1]
            val code =
                when {
                    name.startsWith("#x", ignoreCase = true) -> name.drop(2).toIntOrNull(16)
                    name.startsWith("#") -> name.drop(1).toIntOrNull()
                    else -> NAMED_ENTITIES[name]
                }
            // HTML maps numeric references in 0x80..0x9F through Windows-1252 (&#146; is ’).
            code
                ?.let { if (it in 0x80..0x9F) WINDOWS_1252_C1[it - 0x80] else it }
                ?.takeIf { it in 1..0x10FFFF && it !in 0xD800..0xDFFF }
                ?.let(::codePointString)
                ?: match.value
        }

    // take(n) counts UTF-16 units and can split a surrogate pair (emoji) in half.
    private fun String.takeCodePoints(n: Int): String {
        if (length <= n) return this
        val end = if (this[n - 1].isHighSurrogate()) n - 1 else n
        return substring(0, end)
    }

    private fun codePointString(code: Int): String =
        if (code < 0x10000) {
            Char(code).toString()
        } else {
            val offset = code - 0x10000
            charArrayOf(Char(0xD800 + (offset shr 10)), Char(0xDC00 + (offset and 0x3FF)))
                .concatToString()
        }

    /** Human-readable age for the reader UI without Android dependencies. */
    fun relativeTime(
        millis: Long?,
        now: Long = Clock.System.now().toEpochMilliseconds(),
        language: String = platformLanguage(),
    ): String {
        if (millis == null) return ""
        val seconds = ((now - millis).coerceAtLeast(0L)) / 1000L
        val normalizedLanguage = language.lowercase()
        return when {
            seconds < 60L -> if (normalizedLanguage == "pl") "przed chwilą" else "just now"
            seconds < 3_600L -> formatAge(seconds / 60L, AgeUnit.MINUTE, normalizedLanguage)
            seconds < 86_400L -> formatAge(seconds / 3_600L, AgeUnit.HOUR, normalizedLanguage)
            else -> formatAge(seconds / 86_400L, AgeUnit.DAY, normalizedLanguage)
        }
    }

    private enum class AgeUnit { MINUTE, HOUR, DAY }

    private fun formatAge(
        count: Long,
        unit: AgeUnit,
        language: String,
    ): String =
        if (language == "pl") {
            val word =
                when (unit) {
                    AgeUnit.MINUTE -> polishForm(count, "minutę", "minuty", "minut")
                    AgeUnit.HOUR -> polishForm(count, "godzinę", "godziny", "godzin")
                    AgeUnit.DAY -> polishForm(count, "dzień", "dni", "dni")
                }
            "$count $word temu"
        } else {
            val word =
                when (unit) {
                    AgeUnit.MINUTE -> if (count == 1L) "minute" else "minutes"
                    AgeUnit.HOUR -> if (count == 1L) "hour" else "hours"
                    AgeUnit.DAY -> if (count == 1L) "day" else "days"
                }
            "$count $word ago"
        }

    private fun polishForm(
        count: Long,
        one: String,
        few: String,
        many: String,
    ): String {
        if (count == 1L) return one
        val lastTwo = count % 100L
        val last = count % 10L
        return if (last in 2L..4L && lastTwo !in 12L..14L) few else many
    }

    private val TAGS = Regex("<[^>]+>")
    private val WHITESPACE = Regex("[\\s\u00A0]+") // incl. decoded &nbsp;
    private val ENTITY = Regex("&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z]{2,8});")
    private val WINDOWS_1252_C1 =
        intArrayOf(
            0x20AC, 0x81, 0x201A, 0x192, 0x201E, 0x2026, 0x2020, 0x2021,
            0x2C6, 0x2030, 0x160, 0x2039, 0x152, 0x8D, 0x17D, 0x8F,
            0x90, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014,
            0x2DC, 0x2122, 0x161, 0x203A, 0x153, 0x9D, 0x17E, 0x178,
        )
    private val NAMED_ENTITIES =
        mapOf(
            "nbsp" to 0xA0, "amp" to 0x26, "lt" to 0x3C, "gt" to 0x3E, "quot" to 0x22, "apos" to 0x27,
            "hellip" to 0x2026, "ndash" to 0x2013, "mdash" to 0x2014, "laquo" to 0xAB, "raquo" to 0xBB,
            "lsquo" to 0x2018, "rsquo" to 0x2019, "ldquo" to 0x201C, "rdquo" to 0x201D, "bdquo" to 0x201E,
            "copy" to 0xA9, "reg" to 0xAE, "deg" to 0xB0, "euro" to 0x20AC,
            "trade" to 0x2122, "bull" to 0x2022, "middot" to 0xB7, "times" to 0xD7, "shy" to 0xAD,
            "sbquo" to 0x201A, "prime" to 0x2032, "pound" to 0xA3, "sect" to 0xA7, "para" to 0xB6,
            "eacute" to 0xE9, "egrave" to 0xE8, "aacute" to 0xE1, "oacute" to 0xF3, "uuml" to 0xFC,
            "ouml" to 0xF6, "auml" to 0xE4, "szlig" to 0xDF, "ccedil" to 0xE7, "ntilde" to 0xF1,
        )
    private val IMAGE_URL = Regex("(?i)^https?://.*\\.(?:jpg|jpeg|png|webp|gif)(?:[?#].*)?$")
}

internal expect fun parseFeedDate(value: String): Long?

internal expect fun platformLanguage(): String
