package com.kanarek.data

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsRepositoryParseStatusTest {
    @Test
    fun malformedFeedIsFailureButValidEmptyRssIsSuccess() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/broken") { exchange ->
            respond(exchange, "not xml at all")
        }
        server.createContext("/empty") { exchange ->
            respond(exchange, "<rss version=\"2.0\"><channel><title>Empty</title></channel></rss>")
        }
        server.start()
        try {
            val root = "http://127.0.0.1:${server.address.port}"
            val results = runBlocking {
                NewsRepository().fetchEachWithStatus(listOf("$root/broken", "$root/empty"))
            }
            assertEquals(2, results.size)
            assertFalse(results[0].successful)
            assertTrue(results[0].items.isEmpty())
            assertTrue(results[1].successful)
            assertTrue(results[1].items.isEmpty())
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
