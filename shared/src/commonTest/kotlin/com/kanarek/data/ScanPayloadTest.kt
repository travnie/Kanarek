package com.kanarek.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScanPayloadTest {
    @Test fun wifiIsOnlyClassified() {
        val result = ScanPayloadParser.parse("WIFI:T:WPA;S:Guest\\;Network;P:secret123;;")
        assertEquals(ScanKind.WIFI, result.kind)
        assertEquals("Guest;Network", result.fields["S"])
        assertEquals("secret123", result.fields["P"])
        assertFalse(result.summary.contains("secret123"))
    }

    @Test fun contactAndCalendar() {
        assertEquals(ScanKind.CONTACT, ScanPayloadParser.parse("BEGIN:VCARD\\nFN:Jane Doe\\nTEL:+123456789\\nEND:VCARD").kind)
        assertEquals(ScanKind.CONTACT, ScanPayloadParser.parse("MECARD:N:Jane;TEL:123;;").kind)
        assertEquals(ScanKind.EVENT, ScanPayloadParser.parse("BEGIN:VEVENT\\nSUMMARY:Meeting\\nEND:VEVENT").kind)
    }

    @Test fun linksAndUnknowns() {
        assertEquals(ScanKind.WEB, ScanPayloadParser.parse("https://example.com").kind)
        assertEquals(ScanKind.PLAYLIST, ScanPayloadParser.parse("https://example.org/tv.m3u8").kind)
        assertEquals(ScanKind.TEXT, ScanPayloadParser.parse("javascript:alert(1)").kind)
        assertEquals(ScanKind.TEXT, ScanPayloadParser.parse("intent://fake").kind)
        assertTrue(ScanPayloadParser.parse("x".repeat(20_000)).raw.length <= 16_384)
    }
}
