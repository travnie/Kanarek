package com.kanarek.ui

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OfflineBenchesTest {
    @Test fun benchAssetMappingIsExplicit() {
        assertEquals(BenchKind.CODE, Bench.CODE.kind)
        assertEquals(BenchKind.DOC, Bench.DOC.kind)
        assertEquals(BenchKind.STREAM, Bench.STREAM.kind)
        assertEquals(BenchKind.entries.toSet(), Bench.entries.map { it.kind }.toSet())
        assertEquals("codebench", BenchKind.CODE.slug)
        assertEquals("docbench", BenchKind.DOC.slug)
        assertEquals("streambench", BenchKind.STREAM.slug)
        assertEquals(3, BenchKind.entries.size)
    }

    @Test fun localOriginsAreIsolated() {
        for (kind in BenchKind.entries) {
            assertTrue(OfflineBenches.isTrustedPage(Uri.parse(OfflineBenches.url(kind)), kind))
            assertTrue(OfflineBenches.isTrustedPage(Uri.parse(OfflineBenches.url(kind) + "#editor"), kind))
            assertFalse(OfflineBenches.isTrustedPage(
                Uri.parse(OfflineBenches.url(kind)),
                if (kind == BenchKind.DOC) BenchKind.CODE else BenchKind.DOC,
            ))
            assertFalse(OfflineBenches.isTrustedPage(Uri.parse(OfflineBenches.url(kind) + "?q=1"), kind))
            assertFalse(OfflineBenches.isTrustedPage(
                Uri.parse("http://appassets.androidplatform.net/benches/${kind.slug}/portable.html"),
                kind,
            ))
        }
    }

    @Test fun eachBenchHasSeparateStorageOrigin() {
        assertFalse(OfflineBenches.origin(BenchKind.CODE) == OfflineBenches.origin(BenchKind.DOC))
        assertFalse(OfflineBenches.origin(BenchKind.DOC) == OfflineBenches.origin(BenchKind.STREAM))
    }

    @Test fun onlyExistingRestrictedBackendIsAllowed() {
        assertTrue(OfflineBenches.isAllowedStreamBackend(Uri.parse("https://streambench.travny.workers.dev/api/catalog")))
        assertFalse(OfflineBenches.isAllowedStreamBackend(Uri.parse("https://streambench.travny.workers.dev/secret")))
        assertFalse(OfflineBenches.isAllowedStreamBackend(Uri.parse("http://streambench.travny.workers.dev/api/catalog")))
        assertFalse(OfflineBenches.isAllowedStreamBackend(Uri.parse("https://streambench.travny.workers.dev.evil.test/api/catalog")))
    }

    @Test fun bundledPageOrHostedFallback() {
        val context = RuntimeEnvironment.getApplication()
        for (kind in BenchKind.entries) {
            val loader = OfflineBenches.loader(context, kind)
            val served = loader.shouldInterceptRequest(Uri.parse(OfflineBenches.url(kind)))
            assertEquals(OfflineBenches.isBundled(context, kind), served != null)
            served?.data?.close()
            assertEquals("https://example.com", OfflineBenches.destination(false, kind, "https://example.com"))
            assertEquals(OfflineBenches.url(kind), OfflineBenches.destination(true, kind, "https://example.com"))
            assertEquals(403, OfflineBenches.intercept(
                loader, Uri.parse("https://blocked.test/tracker"), kind,
            )?.statusCode)
        }
    }
}
