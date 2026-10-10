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
            val summary = plainText(item.description ?: item.content.orEmpty()).take(280)
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

    internal fun decodeEntities(value: String): String =
        ENTITY.replace(value) { match ->
            val name = match.groupValues[1]
            val code =
                when {
                    name.startsWith("#x", ignoreCase = true) -> name.drop(2).toIntOrNull(16)
                    name.startsWith("#") -> name.drop(1).toIntOrNull()
                    else -> NAMED_ENTITIES[name]
                }
            code?.takeIf { it in 1..0x10FFFF && it !in 0xD800..0xDFFF }?.let(::codePointString)
                ?: match.value
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
    private val NAMED_ENTITIES =
        mapOf(
            "nbsp" to 0xA0, "amp" to 0x26, "lt" to 0x3C, "gt" to 0x3E, "quot" to 0x22, "apos" to 0x27,
            "hellip" to 0x2026, "ndash" to 0x2013, "mdash" to 0x2014, "laquo" to 0xAB, "raquo" to 0xBB,
            "lsquo" to 0x2018, "rsquo" to 0x2019, "ldquo" to 0x201C, "rdquo" to 0x201D, "bdquo" to 0x201E,
            "copy" to 0xA9, "reg" to 0xAE, "deg" to 0xB0, "euro" to 0x20AC,
        )
    private val IMAGE_URL = Regex("(?i)^https?://.*\\.(?:jpg|jpeg|png|webp|gif)(?:[?#].*)?$")
}

internal expect fun parseFeedDate(value: String): Long?

internal expect fun platformLanguage(): String
