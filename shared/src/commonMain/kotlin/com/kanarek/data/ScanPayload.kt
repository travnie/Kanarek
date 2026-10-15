package com.kanarek.data

enum class ScanKind { WIFI, CONTACT, EVENT, WEB, EMAIL, PHONE, SMS, LOCATION, PLAYLIST, TEXT }

data class ScanPayload(
    val kind: ScanKind,
    val raw: String,
    val summary: String,
    val fields: Map<String, String> = emptyMap(),
)

// Untrusted QR/barcode content. Classification never performs an action.
object ScanPayloadParser {
    private const val MAX_CHARS = 16_384

    fun parse(raw: String): ScanPayload {
        val value = raw.take(MAX_CHARS)
        val trimmed = value.trim()
        if (raw.length > MAX_CHARS || trimmed.isEmpty()) {
            return ScanPayload(ScanKind.TEXT, value, "Text (not auto-interpreted)")
        }

        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            val attrs = fields(trimmed.substring(5))
            val ssid = attrs["S"].orEmpty()
            if (ssid.isNotBlank() && ssid.length <= 32 && attrs["T"]?.uppercase() in setOf("WPA", "WPA2", "WPA3", "NOPASS", null, "")) {
                return ScanPayload(ScanKind.WIFI, value, "Wi-Fi: " + ssid, attrs)
            }
        }
        if (trimmed.startsWith("BEGIN:VCARD", ignoreCase = true)) {
            val attrs = recordFields(trimmed)
            if (attrs["FN"].orEmpty().isNotBlank() || attrs["TEL"].orEmpty().isNotBlank()) {
                return ScanPayload(ScanKind.CONTACT, value, "Contact: " + (attrs["FN"] ?: attrs["TEL"]), attrs)
            }
        }
        if (trimmed.startsWith("MECARD:", ignoreCase = true)) {
            val attrs = fields(trimmed.substring(7))
            if (attrs["N"].orEmpty().isNotBlank()) {
                return ScanPayload(ScanKind.CONTACT, value, "Contact: " + attrs["N"], attrs)
            }
        }
        if (trimmed.startsWith("BEGIN:VEVENT", ignoreCase = true) ||
            trimmed.startsWith("BEGIN:VCALENDAR", ignoreCase = true)
        ) {
            val attrs = recordFields(trimmed)
            if (attrs["SUMMARY"].orEmpty().isNotBlank()) {
                return ScanPayload(ScanKind.EVENT, value, "Event: " + attrs["SUMMARY"], attrs)
            }
        }
        if (trimmed.startsWith("#EXTM3U")) {
            return ScanPayload(ScanKind.PLAYLIST, value, "M3U playlist (review before importing)")
        }
        if (Regex("^https?://[^\\s]+$", RegexOption.IGNORE_CASE).matches(trimmed)) {
            val playlist = Regex("\\.(m3u8?|pls|xspf)(\\?|#|$)", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)
            return ScanPayload(
                if (playlist) ScanKind.PLAYLIST else ScanKind.WEB,
                value,
                (if (playlist) "Playlist URL: " else "Web link: ") + trimmed.take(160),
            )
        }
        if (trimmed.startsWith("mailto:", true) && trimmed.length in 8..320) {
            return ScanPayload(ScanKind.EMAIL, value, "Email: " + trimmed.substringAfter(':').substringBefore('?'))
        }
        if (trimmed.startsWith("tel:", true) && trimmed.length in 7..80) {
            return ScanPayload(ScanKind.PHONE, value, "Phone: " + trimmed.substringAfter(':'))
        }
        if ((trimmed.startsWith("smsto:", true) || trimmed.startsWith("sms:", true)) && trimmed.length <= 2000) {
            return ScanPayload(ScanKind.SMS, value, "SMS: " + trimmed.substringAfter(':').substringBefore(':'))
        }
        if (trimmed.startsWith("geo:", true) && trimmed.length <= 250) {
            return ScanPayload(ScanKind.LOCATION, value, "Location: " + trimmed.substringAfter(':').take(120))
        }
        return ScanPayload(ScanKind.TEXT, value, "Text (" + value.length + " characters)")
    }

    // Standard WIFI/MECARD escaped semicolon pairs.
    private fun fields(source: String): Map<String, String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (ch in source) {
            when {
                escaped -> { current.append(ch); escaped = false }
                ch == '\\' -> escaped = true
                ch == ';' -> { parts.add(current.toString()); current.clear() }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) parts.add(current.toString())
        return parts.mapNotNull { part ->
            val index = part.indexOf(':')
            if (index <= 0) null else part.substring(0, index).uppercase() to part.substring(index + 1)
        }.toMap()
    }

    private fun recordFields(source: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        for (line in source.replace("\r\n", "\n").lines()) {
            val colon = line.indexOf(':')
            if (colon <= 0) continue
            val header = line.substring(0, colon)
            val key = header.substringBefore(';').uppercase()
            val value = line.substring(colon + 1).take(2000)
            val tzid = header.split(';').firstOrNull { it.startsWith("TZID=", ignoreCase = true) }
                ?.substringAfter('=')
            if (key in setOf("TEL", "EMAIL") && result.containsKey(key)) {
                var index = 2
                while (result.containsKey(key + "_" + index)) index++
                result[key + "_" + index] = value
            } else result[key] = value
            if (tzid != null) result[key + "_TZID"] = tzid
        }
        return result
    }
}
