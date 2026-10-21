package com.kanarek.ui

import android.net.NetworkCapabilities
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BenchModeTest {
    @Test fun noNetworkCannotReplaceOfflineWorkshop() {
        assertFalse(hasHostedConnection(null))
        assertFalse(hasHostedConnection(NetworkCapabilities.Builder().build()))
        assertFalse(hasHostedConnection(
            NetworkCapabilities.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
        ))
    }

    @Test fun hostedRequiresValidatedInternet() {
        val connected = NetworkCapabilities.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        assertTrue(hasHostedConnection(connected))
    }
}
