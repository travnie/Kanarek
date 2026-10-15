package com.kanarek.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kanarek.data.ScanKind
import com.kanarek.data.ScanPayload
import com.kanarek.scanner.ScanActionDispatcher

@Composable
internal fun ScanResultDialog(
    payload: ScanPayload,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var pendingSave by remember { mutableStateOf<String?>(null) }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val bytes = pendingSave?.toByteArray(Charsets.UTF_8)
        pendingSave = null
        if (uri != null && bytes != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } != null
            }.getOrDefault(false)
            Toast.makeText(context, if (saved) "Saved" else "Could not save file", Toast.LENGTH_SHORT).show()
        }
    }
    val actionLabel = ScanActionDispatcher.label(payload)
    val content = if (payload.kind == ScanKind.WIFI) {
        payload.summary + "\nType: " + payload.fields["T"].orEmpty() + "\nPassword: " +
            if (payload.fields["P"].isNullOrEmpty()) "(none)" else "(hidden)"
    } else payload.raw.take(1200)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scanned: " + payload.kind.name.lowercase()) },
        text = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                Text(payload.summary)
                Text(content)
                Text("Review before proceeding. Nothing runs automatically.")
                Row {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Codebench scan", payload.raw))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy") }
                    TextButton(onClick = {
                        pendingSave = payload.raw
                        saveFile.launch("scan-" + payload.kind.name.lowercase() + ".txt")
                    }) { Text("Save as text") }
                }
            }
        },
        confirmButton = {
            if (actionLabel != null) {
                TextButton(onClick = {
                    if (ScanActionDispatcher.open(context, payload)) onDismiss()
                    else Toast.makeText(context, "No suitable app or unsafe data", Toast.LENGTH_SHORT).show()
                }) { Text(actionLabel) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
