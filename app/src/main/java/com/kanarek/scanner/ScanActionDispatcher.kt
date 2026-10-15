package com.kanarek.scanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import com.kanarek.data.ScanKind
import com.kanarek.data.ScanPayload
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// System previews only. No silent connections, calls, messages or inserts.
object ScanActionDispatcher {
    fun label(payload: ScanPayload): String? = when (payload.kind) {
        ScanKind.WIFI -> "Review Wi-Fi network"
        ScanKind.CONTACT -> "Create contact"
        ScanKind.EVENT -> "Add to calendar"
        ScanKind.WEB, ScanKind.PLAYLIST -> if (payload.raw.trim().startsWith("https://", ignoreCase = true)) "Open in browser" else null
        ScanKind.EMAIL -> "Compose email"
        ScanKind.PHONE -> "Open dialer"
        ScanKind.SMS -> "Compose SMS"
        ScanKind.LOCATION -> "Open maps"
        ScanKind.TEXT -> null
    }

    fun open(context: Context, payload: ScanPayload): Boolean {
        val intent = when (payload.kind) {
            ScanKind.WIFI -> wifiIntent(payload)
            ScanKind.CONTACT -> Intent(Intent.ACTION_INSERT).apply {
                type = ContactsContract.Contacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.NAME, payload.fields["FN"] ?: payload.fields["N"].orEmpty())
                payload.fields["TEL"]?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
                payload.fields["EMAIL"]?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
                payload.fields["ORG"]?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
            }
            ScanKind.EVENT -> Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
                putExtra(CalendarContract.Events.TITLE, payload.fields["SUMMARY"].orEmpty())
                putExtra(CalendarContract.Events.DESCRIPTION, payload.fields["DESCRIPTION"].orEmpty().take(1800))
                putExtra(CalendarContract.Events.EVENT_LOCATION, payload.fields["LOCATION"].orEmpty())
                parseDate(payload.fields["DTSTART"], payload.fields["DTSTART_TZID"])?.let {
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it)
                }
                parseDate(payload.fields["DTEND"], payload.fields["DTEND_TZID"])?.let {
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it)
                }
            }
            ScanKind.WEB, ScanKind.PLAYLIST -> {
                val link = payload.raw.trim()
                if (!link.startsWith("https://", ignoreCase = true)) return false
                Intent(Intent.ACTION_VIEW, Uri.parse(link)).addCategory(Intent.CATEGORY_BROWSABLE)
            }
            ScanKind.EMAIL -> {
                val uri = Uri.parse(payload.raw.trim())
                if (uri.scheme != "mailto") return false
                Intent(Intent.ACTION_SENDTO, uri)
            }
            ScanKind.PHONE -> {
                val number = payload.raw.trim().substringAfter(':')
                if (!Regex("^[+0-9() .-]{3,40}$").matches(number)) return false
                Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
            }
            ScanKind.SMS -> {
                val recipient = payload.raw.trim().substringAfter(':').substringBefore(':').substringBefore('?')
                if (!Regex("^[+0-9() .-]{3,40}$").matches(recipient)) return false
                Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", recipient, null)).apply {
                    val raw = payload.raw.trim().substringAfter(':')
                    val body = if (raw.contains("?body=", ignoreCase = true)) {
                        Uri.decode(raw.substringAfter('?').substringAfter("body=", ""))
                    } else raw.substringAfter(':', "")
                    if (body.isNotEmpty()) putExtra("sms_body", body.take(1000))
                }
            }
            ScanKind.LOCATION -> {
                val position = payload.raw.trim()
                if (!Regex("^geo:-?[0-9.]+,-?[0-9.]+.*$", RegexOption.IGNORE_CASE).matches(position)) return false
                Intent(Intent.ACTION_VIEW, Uri.parse(position))
            }
            ScanKind.TEXT -> return false
        }
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    private fun wifiIntent(payload: ScanPayload): Intent {
        val ssid = payload.fields["S"].orEmpty()
        val auth = payload.fields["T"]?.uppercase().orEmpty()
        val pass = payload.fields["P"].orEmpty()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || ssid.isBlank() || ssid.length > 32) {
            return Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        val builder = WifiNetworkSuggestion.Builder().setSsid(ssid)
        val valid = when (auth) {
            "", "NOPASS" -> pass.isEmpty()
            "WPA", "WPA2" -> pass.length in 8..63 && pass.all { it.code in 32..126 }
            "WPA3" -> pass.length in 8..63 && pass.all { it.code in 32..126 }
            else -> false
        }
        if (!valid) return Intent(Settings.ACTION_WIFI_SETTINGS)
        return runCatching {
            when (auth) {
                "WPA", "WPA2" -> builder.setWpa2Passphrase(pass)
                "WPA3" -> builder.setWpa3Passphrase(pass)
            }
            Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
                putParcelableArrayListExtra(
                    Settings.EXTRA_WIFI_NETWORK_LIST,
                    arrayListOf(builder.build()),
                )
            }
        }.getOrElse { Intent(Settings.ACTION_WIFI_SETTINGS) }
    }

    private fun parseDate(input: String?, tzid: String? = null): Long? = runCatching {
        val zone = tzid?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val date = input?.takeIf { it.length == 8 || it.length == 15 || it.length == 16 } ?: return null
        when {
            date.length == 8 ->
                LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE)
                    .atStartOfDay(zone).toInstant().toEpochMilli()
            date.endsWith("Z") ->
                LocalDateTime.parse(date.dropLast(1), DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
                    .toInstant(ZoneOffset.UTC).toEpochMilli()
            else ->
                LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
                    .atZone(zone).toInstant().toEpochMilli()
        }
    }.getOrNull()
}
