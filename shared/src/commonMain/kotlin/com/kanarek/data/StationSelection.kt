package com.kanarek.data

/** Carry station identity across a platform-player handoff; index is only fallback. */
fun stationIndexForHandoff(stations: List<Station>, sourceMediaId: String?, fallbackIndex: Int): Int {
    if (stations.isEmpty()) return 0
    val byId = stations.indexOfFirst { it.id == sourceMediaId }
    return if (byId >= 0) byId else fallbackIndex.coerceIn(0, stations.lastIndex)
}
