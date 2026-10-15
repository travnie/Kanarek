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
        assertEquals(ScanKind.CONTACT, ScanPayloadParser.parse("BEGIN:VCARD\nFN:Jane Doe\nTEL:+123456789\nEND:VCARD").kind)
        assertEquals(ScanKind.CONTACT, ScanPayloadParser.parse("MECARD:N:Jane;TEL:123;;").kind)
        assertEquals(ScanKind.EVENT, ScanPayloadParser.parse("BEGIN:VEVENT\nSUMMARY:Meeting\nEND:VEVENT").kind)
    }

    @Test fun commonQuickActionsRemainPreviews() {
        assertEquals(ScanKind.EMAIL, ScanPayloadParser.parse("mailto:alice@example.org").kind)
        assertEquals(ScanKind.PHONE, ScanPayloadParser.parse("tel:+48123123123").kind)
        assertEquals(ScanKind.SMS, ScanPayloadParser.parse("sms:+48123123123?body=Hi").kind)
        assertEquals(ScanKind.LOCATION, ScanPayloadParser.parse("geo:50.1,19.3").kind)
        assertEquals(ScanKind.PLAYLIST, ScanPayloadParser.parse("#EXTM3U\n#EXTINF:-1,Example").kind)
    }

    @Test fun suspiciousSchemesStayPlainText() {
        for (source in listOf("javascript:alert(1)", "intent://scan/#Intent", "file:///etc/passwd", "content://com.example/file")) {
            assertEquals(ScanKind.TEXT, ScanPayloadParser.parse(source).kind)
        }
    }

    @Test fun repeatsPreservedForContactDraft() {
        val result = ScanPayloadParser.parse("BEGIN:VCARD\nFN:Jane\nTEL:111\nTEL:222\nEMAIL:a@example.org\nEMAIL:b@example.org\nEND:VCARD")
        assertEquals(ScanKind.CONTACT, result.kind)
        assertEquals("222", result.fields["TEL_2"])
        assertEquals("b@example.org", result.fields["EMAIL_2"])
    }

    @Test fun linksAndUnknowns() {
        assertEquals(ScanKind.WEB, ScanPayloadParser.parse("https://example.com").kind)
        assertEquals(ScanKind.PLAYLIST, ScanPayloadParser.parse("https://example.org/tv.m3u8").kind)
        assertEquals(ScanKind.TEXT, ScanPayloadParser.parse("javascript:alert(1)").kind)
        assertEquals(ScanKind.TEXT, ScanPayloadParser.parse("intent://fake").kind)
        assertTrue(ScanPayloadParser.parse("x".repeat(20_000)).raw.length <= 16_384)
    }
}
