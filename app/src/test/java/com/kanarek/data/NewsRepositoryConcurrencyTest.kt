package com.kanarek.data

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsRepositoryConcurrencyTest {
    @Test
    fun suspendPerFeedFetchCompletesOnSmallIoPool() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/feed") { exchange ->
            val body = RSS.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/rss+xml")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()

        Executors.newFixedThreadPool(2).asCoroutineDispatcher().use { dispatcher ->
            try {
                val repository =
                    NewsRepository(dispatcher) {
                        listOf(
                            NewsItem(
                                title = "Item",
                                link = "https://example.com/item",
                                summary = "",
                                imageUrl = null,
                                source = "Test",
                                publishedAtMillis = null,
                            ),
                        )
                    }
                val feeds = (1..10).map { "http://127.0.0.1:${server.address.port}/feed?id=$it" }

                val results =
                    runBlocking(dispatcher) {
                        withTimeout(5_000) { repository.fetchEachWithStatus(feeds) }
                    }

                assertEquals(feeds, results.map(SourceNewsFetchResult::feed))
                assertTrue(results.all(SourceNewsFetchResult::successful))
            } finally {
                server.stop(0)
            }
        }
    }

    @Test
    fun concurrentMapLimitsWorkAndPropagatesCancellation() {
        val active = AtomicInteger()
        val peak = AtomicInteger()

        val error =
            runCatching {
                runBlocking {
                    withTimeout(100) {
                        mapConcurrent((1..20).toList(), parallelism = 3) {
                            val now = active.incrementAndGet()
                            peak.updateAndGet { maxOf(it, now) }
                            try {
                                delay(1_000)
                            } finally {
                                active.decrementAndGet()
                            }
                        }
                    }
                }
            }.exceptionOrNull()

        assertTrue(error is kotlinx.coroutines.TimeoutCancellationException)
        assertTrue(peak.get() <= 3)
        assertEquals(0, active.get())
    }

    companion object {
        private const val RSS =
            """<?xml version="1.0"?><rss version="2.0"><channel><title>Test</title>""" +
                """<item><title>Item</title><link>https://example.com/item</link></item>""" +
                """</channel></rss>"""
    }
}
