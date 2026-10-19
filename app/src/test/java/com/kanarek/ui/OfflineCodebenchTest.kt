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
class OfflineCodebenchTest {
    @Test fun onlyPinnedPageIsTrusted() {
        assertTrue(OfflineCodebench.isTrustedPage(Uri.parse(OfflineCodebench.URL)))
        assertTrue(OfflineCodebench.isTrustedPage(Uri.parse(OfflineCodebench.URL + "#scan")))
        assertFalse(OfflineCodebench.isTrustedPage(Uri.parse("http://appassets.androidplatform.net/bench/portable.html")))
        assertFalse(OfflineCodebench.isTrustedPage(Uri.parse("https://appassets.androidplatform.net/bench/other.html")))
        assertFalse(OfflineCodebench.isTrustedPage(Uri.parse("https://codebench.trfny.com/")))
        assertFalse(OfflineCodebench.isTrustedPage(Uri.parse(OfflineCodebench.URL + "?redirect=1")))
    }

    @Test @Config(sdk = [33]) fun assetLoaderServesOnlyTheOfflinePage() {
        val context = RuntimeEnvironment.getApplication()
        val loader = OfflineCodebench.loader(context)
        val served = loader.shouldInterceptRequest(Uri.parse(OfflineCodebench.URL))
        assertEquals(OfflineCodebench.isBundled(context), served != null)
        served?.data?.close()
        assertEquals(403, OfflineCodebench.intercept(
            loader, Uri.parse("https://example.com/tracker.js"),
        )?.statusCode)
        assertEquals(403, OfflineCodebench.intercept(
            loader, Uri.parse("https://appassets.androidplatform.net/bench/other.js"),
        )?.statusCode)
    }

    @Test fun hostedFallbackStaysAvailable() {
        val hosted = "https://codebench.trfny.com"
        assertEquals(hosted, OfflineCodebench.destination(false, hosted))
        assertEquals(OfflineCodebench.URL, OfflineCodebench.destination(true, hosted))
    }
}
