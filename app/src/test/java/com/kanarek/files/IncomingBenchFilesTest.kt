package com.kanarek.files

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
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

    @Test fun formatsNotParsedByStreambenchStayRawDocuments() {
        assertEquals(BenchFileTarget.DOC, IncomingBenchFiles.classify("radio.pls", "text/plain"))
        assertEquals(BenchFileTarget.DOC, IncomingBenchFiles.classify("channels.xspf", "application/xml"))
    }

    @Test fun documentsRouteToDocbench() {
        for (filename in listOf("script.sh", ".env", "notes.md", "data.json", "book.pdf", "draft.docx")) {
            assertEquals(BenchFileTarget.DOC, IncomingBenchFiles.classify(filename, "application/octet-stream"))
        }
    }

    @Test fun docbenchScriptPrefersCanonicalFilesIngress() {
        val script = IncomingBenchFiles.importJavascript(
            PreparedBenchFile(1L, "archive.zip", "application/zip", byteArrayOf(1, 2), BenchFileTarget.DOC),
        )
        org.junit.Assert.assertTrue(script.contains("window.DocBenchFiles"))
        org.junit.Assert.assertTrue(script.contains("docbench?.version === 1"))
        org.junit.Assert.assertTrue(script.contains("if (true && docbench?.version === 1"))
        org.junit.Assert.assertTrue(script.contains("return docbench.importFile(file)?.ok === true;"))
        org.junit.Assert.assertTrue(script.contains("document-tab"))
        org.junit.Assert.assertTrue(script.contains("input.files = transfer.files;"))
        org.junit.Assert.assertTrue(script.contains("input.dispatchEvent(new Event(\"change\""))
        org.junit.Assert.assertTrue(script.contains("DataTransfer"))
    }

    @Test fun nativeRoutePreservesCodebenchAndPlaylistInputs() {
        val image = IncomingBenchFiles.importJavascript(
            PreparedBenchFile(2L, "code.png", "image/png", byteArrayOf(3), BenchFileTarget.CODE),
        )
        val playlist = IncomingBenchFiles.importJavascript(
            PreparedBenchFile(3L, "radio.m3u", "text/plain", byteArrayOf(4), BenchFileTarget.STREAM),
        )
        org.junit.Assert.assertTrue(image.contains("const input = document.getElementById(\"fileInput\")"))
        org.junit.Assert.assertTrue(playlist.contains("const input = document.getElementById(\"playlistFile\")"))
        org.junit.Assert.assertTrue(image.contains("if (false && docbench?.version === 1"))
        org.junit.Assert.assertTrue(playlist.contains("if (false && docbench?.version === 1"))
    }
}
