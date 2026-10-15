package com.kanarek.scanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScanTileTokenTest {
    @Test
    fun mismatchedIntentsDoNotConsumePendingRequest() {
        val token = ScanTileToken.issue()
        assertFalse(ScanTileToken.consume(null))
        assertFalse(ScanTileToken.consume("not-the-issued-token"))
        assertTrue(ScanTileToken.consume(token))
        assertFalse(ScanTileToken.consume(token))
    }

    @Test
    fun issuingAnotherRequestInvalidatesPreviousToken() {
        val first = ScanTileToken.issue()
        val second = ScanTileToken.issue()
        assertFalse(ScanTileToken.consume(first))
        assertTrue(ScanTileToken.consume(second))
    }
}
