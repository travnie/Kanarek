package com.kanarek.data

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NewsRepositoryParseStatusTest {
    @Test
    fun malformedFeedFailsWhileValidRssWithFilteredItemsSucceeds() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/broken") { exchange ->
            respond(exchange, "not xml at all")
        }
        server.createContext("/empty") { exchange ->
            respond(exchange, "<rss version=\"2.0\"><channel><title>Empty</title><item><link>https://example.com/no-title</link></item></channel></rss>")
        }
        server.createContext("/blank") { exchange -> respond(exchange, "") }
        server.start()
        try {
            val root = "http://127.0.0.1:${server.address.port}"
            val results = runBlocking {
                NewsRepository().fetchEachWithStatus(listOf("$root/broken", "$root/empty", "$root/blank"))
            }
            assertEquals(3, results.size)
            assertFalse(results[0].successful)
            assertTrue(results[0].items.isEmpty())
            assertTrue(results[1].successful)
            assertTrue(results[1].items.isEmpty())
            assertFalse(results[2].successful)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun backendPerSourceStatusIsRespected() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            respond(exchange, """
                {"items":[],"sources":[
                  {"url":"https://example.com/good.xml","status":"ok"},
                  {"url":"https://example.com/bad.xml","status":"error"}
                ]}
            """.trimIndent())
        }
        server.start()
        try {
            val backend = "http://127.0.0.1:${server.address.port}"
            val results = runBlocking {
                NewsRepository().fetchEachWithStatus(
                    listOf("https://example.com/good.xml", "https://example.com/bad.xml"),
                    backendUrl = backend,
                )
            }
            assertTrue(results[0].successful)
            assertFalse(results[1].successful)
        } finally {
            server.stop(0)
        }
    }

    private fun respond(exchange: com.sun.net.httpserver.HttpExchange, body: String) {
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/rss+xml")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
