package com.kanarek.scanner

import android.os.SystemClock
import java.util.UUID

// One-use request, process-local. Do not accept camera autostart from external intents.
internal object ScanTileToken {
    private const val MAX_AGE_MS = 60_000L
    private var value: String? = null
    private var issuedAt: Long = 0

    @Synchronized
    fun issue(): String {
        val next = UUID.randomUUID().toString()
        value = next
        issuedAt = SystemClock.elapsedRealtime()
        return next
    }

    @Synchronized
    fun consume(candidate: String?): Boolean {
        if (candidate == null || candidate != value) return false
        val accepted = SystemClock.elapsedRealtime() - issuedAt in 0..MAX_AGE_MS
        value = null
        issuedAt = 0
        return accepted
    }
}
