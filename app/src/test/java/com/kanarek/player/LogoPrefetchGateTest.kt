package com.kanarek.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoPrefetchGateTest {
    @Test fun suppressesConcurrentFetches() {
        val gate = LogoPrefetchGate(nowMillis = { 10L })
        assertTrue(gate.tryStart("https://example.com/logo.png"))
        assertFalse(gate.tryStart("https://example.com/logo.png"))
        gate.finish("https://example.com/logo.png", succeeded = true)
        assertTrue(gate.tryStart("https://example.com/logo.png"))
    }

    @Test fun backsOffAfterFailureButAllowsOtherLogos() {
        var now = 100L
        val gate = LogoPrefetchGate(nowMillis = { now }, retryDelayMillis = 1_000L)
        val failed = "https://example.com/missing.png"
        assertTrue(gate.tryStart(failed))
        gate.finish(failed, succeeded = false)
        assertFalse(gate.tryStart(failed))
        assertTrue(gate.tryStart("https://example.com/other.png"))
        now += 999L
        assertFalse(gate.tryStart(failed))
        now++
        assertTrue(gate.tryStart(failed))
    }

    @Test fun failedUrlCacheIsBounded() {
        val gate = LogoPrefetchGate(nowMillis = { 1L }, maxFailedUrls = 2)
        listOf("first", "second", "third").forEach {
            assertTrue(gate.tryStart(it))
            gate.finish(it, succeeded = false)
        }
        assertTrue(gate.tryStart("first"))
        assertFalse(gate.tryStart("third"))
    }

    @Test fun cachedLogoRefreshesAtMostOnceUntilRefetched() {
        val gate = LogoPrefetchGate(nowMillis = { 1L })
        assertTrue(gate.shouldRefresh("logo", downloaded = false))
        assertFalse(gate.shouldRefresh("logo", downloaded = false))
        assertTrue(gate.shouldRefresh("logo", downloaded = true))
        assertFalse(gate.shouldRefresh("logo", downloaded = false))
    }

    @Test fun blankUrlsAreIgnored() {
        val gate = LogoPrefetchGate(nowMillis = { 10L })
        assertFalse(gate.tryStart(""))
    }
}
