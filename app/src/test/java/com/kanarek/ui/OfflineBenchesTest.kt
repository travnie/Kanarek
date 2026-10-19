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
    @Test fun localOriginsAreIsolated() {
        for (name in listOf("codebench", "docbench", "streambench")) {
            assertTrue(OfflineBenches.isTrustedPage(Uri.parse(OfflineBenches.url(name)), name))
            assertTrue(OfflineBenches.isTrustedPage(Uri.parse(OfflineBenches.url(name) + "#editor"), name))
            assertFalse(OfflineBenches.isTrustedPage(Uri.parse(OfflineBenches.url(name)), "other"))
            assertFalse(OfflineBenches.isTrustedPage(Uri.parse(OfflineBenches.url(name) + "?q=1"), name))
            assertFalse(OfflineBenches.isTrustedPage(Uri.parse("http://appassets.androidplatform.net/benches/$name/portable.html"), name))
        }
    }

    @Test fun onlyExistingRestrictedBackendIsAllowed() {
        assertTrue(OfflineBenches.isAllowedStreamBackend(Uri.parse("https://streambench.travny.workers.dev/api/catalog")))
        assertFalse(OfflineBenches.isAllowedStreamBackend(Uri.parse("https://streambench.travny.workers.dev/secret")))
        assertFalse(OfflineBenches.isAllowedStreamBackend(Uri.parse("http://streambench.travny.workers.dev/api/catalog")))
        assertFalse(OfflineBenches.isAllowedStreamBackend(Uri.parse("https://streambench.travny.workers.dev.evil.test/api/catalog")))
    }

    @Test fun bundledPageOrHostedFallback() {
        val context = RuntimeEnvironment.getApplication()
        val loader = OfflineBenches.loader(context)
        for (name in listOf("codebench", "docbench", "streambench")) {
            val served = loader.shouldInterceptRequest(Uri.parse(OfflineBenches.url(name)))
            assertEquals(OfflineBenches.isBundled(context, name), served != null)
            served?.data?.close()
            assertEquals("https://example.com", OfflineBenches.destination(false, name, "https://example.com"))
            assertEquals(OfflineBenches.url(name), OfflineBenches.destination(true, name, "https://example.com"))
            assertEquals(403, OfflineBenches.intercept(
                loader, Uri.parse("https://blocked.test/tracker"), name,
            )?.statusCode)
        }
    }
}
