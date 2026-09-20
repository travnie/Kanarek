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

    private fun plainText(value: String): String =
        value
            .replace(TAGS, " ")
            .replace(WHITESPACE, " ")
            .trim()

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
    private val WHITESPACE = Regex("\\s+")
    private val IMAGE_URL = Regex("(?i)^https?://.*\\.(?:jpg|jpeg|png|webp|gif)(?:[?#].*)?$")
}

internal expect fun parseFeedDate(value: String): Long?

internal expect fun platformLanguage(): String
