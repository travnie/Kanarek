package com.kanarek.files

import org.junit.Assert.assertEquals
import org.junit.Test

class IncomingBenchFilesTest {
    @Test fun imageNamesRouteToCodebench() {
        assertEquals(BenchFileTarget.CODE, IncomingBenchFiles.classify("code.webp", "application/octet-stream"))
        assertEquals(BenchFileTarget.CODE, IncomingBenchFiles.classify("photo.bin", "image/png"))
    }

    @Test fun playlistsRouteToStreambenchEvenWhenReportedAsPlainText() {
        assertEquals(BenchFileTarget.STREAM, IncomingBenchFiles.classify("radio.m3u", "text/plain"))
        assertEquals(BenchFileTarget.STREAM, IncomingBenchFiles.classify("iptv.M3U8", "application/octet-stream"))
        assertEquals(BenchFileTarget.STREAM, IncomingBenchFiles.classify("list.bin", "audio/x-mpegurl"))
    }

    @Test fun documentsRouteToDocbench() {
        for (filename in listOf("script.sh", ".env", "notes.md", "data.json", "book.pdf", "draft.docx")) {
            assertEquals(BenchFileTarget.DOC, IncomingBenchFiles.classify(filename, "application/octet-stream"))
        }
    }
}
