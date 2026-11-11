package com.kanarek.player

import com.kanarek.data.Station

/** Local ExoPlayer must be prepared after stop(), even when it still has a queue. */
internal fun needsHandoffRestore(toLocalPlayer: Boolean, targetMediaItemCount: Int): Boolean =
    toLocalPlayer || targetMediaItemCount == 0

/** Carry the station identity across Cast reordering; index only as fallback. */
internal fun handoffTargetIndex(stations: List<Station>, sourceMediaId: String?, fallbackIndex: Int): Int {
    if (stations.isEmpty()) return 0
    val byId = stations.indexOfFirst { it.id == sourceMediaId }
    return if (byId >= 0) byId else fallbackIndex.coerceIn(0, stations.lastIndex)
}
