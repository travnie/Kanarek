package com.kanarek.data

/** Canonical public Feedseek host; raw main snapshots are being retired. */
const val FEEDSEEK_BASE = "https://trvny.github.io/feedseek/"

private const val LEGACY_PREFIX = "feed_"
private const val FEEDSEEK_RAW_BASE = "https://raw.githubusercontent.com/trvny/feedseek/main/feeds/"

/**
 * Legacy Feedseek URLs -> canonical short Pages URL, applied at fetch time only:
 * saved subscriptions keep their URL as the key for cache, bookmarks, widgets, notifications.
 * raw .../main/feeds/feed_<key>.<ext> and Pages feed_<key>.<ext> -> FEEDSEEK_BASE + <key>.<ext>.
 * Anything else unchanged.
 */
fun canonicalFeedUrl(url: String): String {
    val name = when {
        url.startsWith(FEEDSEEK_RAW_BASE) -> url.removePrefix(FEEDSEEK_RAW_BASE)
        url.startsWith(FEEDSEEK_BASE) -> url.removePrefix(FEEDSEEK_BASE)
        else -> return url
    }
    if (!name.startsWith(LEGACY_PREFIX) || '/' in name) return url
    return FEEDSEEK_BASE + name.removePrefix(LEGACY_PREFIX)
}
