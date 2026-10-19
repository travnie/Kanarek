package com.kanarek.ui

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineCodebenchTest {
    @Test fun onlyPinnedPageIsTrusted() {
        assertTrue(OfflineCodebench.isTrustedPage(Uri.parse(OfflineCodebench.URL)))
        assertTrue(OfflineCodebench.isTrustedPage(Uri.parse(OfflineCodebench.URL + "#scan")))
        assertFalse(OfflineCodebench.isTrustedPage(Uri.parse("http://appassets.androidplatform.net/bench/portable.html")))
        assertFalse(OfflineCodebench.isTrustedPage(Uri.parse("https://appassets.androidplatform.net/bench/other.html")))
        assertFalse(OfflineCodebench.isTrustedPage(Uri.parse("https://codebench.trfny.com/")))
        assertFalse(OfflineCodebench.isTrustedPage(Uri.parse(OfflineCodebench.URL + "?redirect=1")))
    }

    @Test fun hostedFallbackStaysAvailable() {
        val hosted = "https://codebench.trfny.com"
        assertEquals(hosted, OfflineCodebench.destination(false, hosted))
        assertEquals(OfflineCodebench.URL, OfflineCodebench.destination(true, hosted))
    }
}
