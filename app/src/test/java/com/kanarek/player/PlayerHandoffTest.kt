package com.kanarek.player

import com.kanarek.data.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerHandoffTest {
    private val stations = listOf(
        Station("one", "One", "https://example.com/one"),
        Station("two", "Two", "https://example.com/two"),
    )

    @Test fun returningToLocalRestoresExistingStoppedQueue() {
        assertTrue(needsHandoffRestore(toLocalPlayer = true, targetMediaItemCount = 2))
        assertTrue(needsHandoffRestore(toLocalPlayer = false, targetMediaItemCount = 0))
        assertFalse(needsHandoffRestore(toLocalPlayer = false, targetMediaItemCount = 2))
    }

    @Test fun usesCastMediaIdBeforeStaleQueueIndex() {
        assertEquals(1, handoffTargetIndex(stations, sourceMediaId = "two", fallbackIndex = 0))
        assertEquals(0, handoffTargetIndex(stations, sourceMediaId = null, fallbackIndex = -1))
        assertEquals(1, handoffTargetIndex(stations, sourceMediaId = "missing", fallbackIndex = 99))
        assertEquals(0, handoffTargetIndex(emptyList(), sourceMediaId = null, fallbackIndex = 5))
    }
}
