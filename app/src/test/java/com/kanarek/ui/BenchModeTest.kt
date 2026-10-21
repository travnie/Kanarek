package com.kanarek.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchModeTest {
    @Test fun cannotSwitchToHostedWithoutValidatedInternet() {
        assertFalse(shouldUseHostedInternet(false, false))
        assertFalse(shouldUseHostedInternet(false, true))
        assertFalse(shouldUseHostedInternet(true, false))
    }

    @Test fun hostedRequiresInternetAndValidation() {
        assertTrue(shouldUseHostedInternet(true, true))
    }
}
