package com.kanarek.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kanarek.R
import com.kanarek.data.SettingsStore

// Single picker for app default + per-widget config.
@Composable
internal fun SlideshowIntervalChips(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (SettingsStore.INTERVAL_OPTIONS + SettingsStore.INTERVAL_OFF).forEach { seconds ->
            FilterChip(
                selected = selected == seconds,
                onClick = { onSelect(seconds) },
                label = { Text(slideshowIntervalLabel(seconds)) },
            )
        }
    }
}

@Composable
private fun slideshowIntervalLabel(seconds: Int): String =
    when {
        seconds == SettingsStore.INTERVAL_OFF -> stringResource(R.string.background_refresh_off)
        seconds % 60 == 0 -> stringResource(R.string.widget_interval_minutes, seconds / 60)
        else -> stringResource(R.string.widget_interval_seconds, seconds)
    }
