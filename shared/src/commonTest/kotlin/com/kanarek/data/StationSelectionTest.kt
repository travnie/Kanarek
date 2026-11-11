package com.kanarek.data

import kotlin.test.Test
import kotlin.test.assertEquals

class StationSelectionTest {
    private val stations = listOf(
        Station("one", "One", "https://example.com/one"),
        Station("two", "Two", "https://example.com/two"),
    )

    @Test
    fun selectsByIdentityAcrossQueueReordering() {
        assertEquals(1, stationIndexForHandoff(stations, "two", 0))
        assertEquals(0, stationIndexForHandoff(stations, null, -1))
        assertEquals(1, stationIndexForHandoff(stations, "missing", 99))
        assertEquals(0, stationIndexForHandoff(emptyList(), null, 5))
    }
}
