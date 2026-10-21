package com.kanarek.ui

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BenchExportStoreTest {
    @Test fun stagedExportSurvivesIndependentStoreAccessAndCanBeDeleted() {
        val context = RuntimeEnvironment.getApplication()
        val bytes = byteArrayOf(0, 17, -1, 3)
        val token = BenchExportStore.stage(context, BenchExport("scan.png", "image/png", bytes))
        val stored = File(context.cacheDir, "bench-exports/$token")
        try {
            assertTrue(stored.isFile)
            assertArrayEquals(bytes, stored.readBytes())
        } finally {
            BenchExportStore.discard(context, token)
        }
        assertFalse(stored.exists())
    }

    @Test fun refusesArbitraryPaths() {
        val context = RuntimeEnvironment.getApplication()
        assertThrows(IllegalArgumentException::class.java) {
            BenchExportStore.discard(context, "../other-file")
        }
    }
}
