package com.kanarek.data

/** Canonical public Feedseek host; raw main snapshots are being retired. */
const val FEEDSEEK_BASE = "https://trvny.github.io/feedseek/"

private const val FEEDSEEK_RAW_BASE = "https://raw.githubusercontent.com/trvny/feedseek/main/feeds/"

/**
 * Legacy Feedseek URLs -> canonical short Pages URL.
 * raw .../main/feeds/feed_<key>.<ext> and Pages feed_<key>.<ext> -> FEEDSEEK_BASE + <key>.<ext>.
 * Anything else unchanged.
 */
fun canonicalFeedUrl(url: String): String {
    val name = when {
        url.startsWith(FEEDSEEK_RAW_BASE) -> url.removePrefix(FEEDSEEK_RAW_BASE)
        url.startsWith(FEEDSEEK_BASE + "feed_") -> url.removePrefix(FEEDSEEK_BASE)
        else -> return url
    }
    if (!name.startsWith("feed_") || '/' in name) return url
    return FEEDSEEK_BASE + name.removePrefix("feed_")
}

/** Canonicalize and drop duplicates created by the mapping; order kept. */
fun canonicalFeedUrls(urls: List<String>): List<String> = urls.map(::canonicalFeedUrl).distinct()
