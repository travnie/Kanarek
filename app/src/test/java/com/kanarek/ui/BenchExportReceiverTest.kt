package com.kanarek.ui

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BenchExportReceiverTest {
    private val receiver = BenchExportReceiver()
    @Test fun preservesBinaryAndFilename() {
        assertNull(receiver.accept("""{"v":1,"type":"start","name":"../../a.pdf","mime":"application/pdf","size":4}"""))
        assertNull(receiver.accept("""{"v":1,"type":"chunk","data":"AAH/Kg=="}"""))
        val result = receiver.accept("""{"v":1,"type":"finish"}""")!!
        assertEquals("a.pdf", result.name)
        assertEquals("application/pdf", result.mime)
        assertArrayEquals(byteArrayOf(0, 1, -1, 42), result.bytes)
    }
    @Test fun rejectsInvalidAndOutOfOrderMessages() {
        assertThrows(IllegalArgumentException::class.java) {
            receiver.accept("""{"v":1,"type":"start","name":"x","size":33554433}""")
        }
        assertThrows(IllegalStateException::class.java) {
            receiver.accept("""{"v":1,"type":"finish"}""")
        }
    }
    @Test fun handlesCancellationAndExactSize() {
        receiver.accept("""{"v":1,"type":"start","name":"x","size":1}""")
        assertThrows(IllegalArgumentException::class.java) {
            receiver.accept("""{"v":1,"type":"finish"}""")
        }
        receiver.reset()
        receiver.accept("""{"v":1,"type":"start","name":"empty","size":0}""")
        assertEquals(0, receiver.accept("""{"v":1,"type":"finish"}""")!!.bytes.size)
    }
}
