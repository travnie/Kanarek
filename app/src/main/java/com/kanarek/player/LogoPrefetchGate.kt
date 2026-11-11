package com.kanarek.player

/** Per-service in-flight guard and cooldown for failed widget logo requests. */
internal class LogoPrefetchGate(
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000L },
    private val retryDelayMillis: Long = 5 * 60_000L,
) {
    private val inFlight = mutableSetOf<String>()
    private val retryAfter = mutableMapOf<String, Long>()

    @Synchronized
    fun tryStart(url: String): Boolean {
        if (url.isBlank() || url in inFlight) return false
        val now = nowMillis()
        if (now < (retryAfter[url] ?: Long.MIN_VALUE)) return false
        retryAfter.remove(url)
        inFlight.add(url)
        return true
    }

    @Synchronized
    fun finish(url: String, succeeded: Boolean) {
        inFlight.remove(url)
        if (succeeded) {
            retryAfter.remove(url)
        } else {
            retryAfter[url] = nowMillis() + retryDelayMillis
        }
    }
}
