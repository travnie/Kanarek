package com.kanarek.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kanarek.R

/**
 * Full source list, reached from a button in [ReaderTopBar] rather than sitting inline on the
 * main feed - the previous [ReaderSourcePicker] chip row hid sources past the screen edge and
 * left no room for search. Same selection/favorite semantics, presented as a searchable vertical
 * list instead.
 */
@Composable
internal fun ReaderSourceListDialog(
    sources: List<String>,
    selectedSources: Set<String>,
    favoriteSources: Set<String>,
    onSelectSource: (String) -> Unit,
    onClearSources: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val sortedSources =
        remember(sources, favoriteSources) {
            sources.sortedWith(
                compareByDescending<String> { source ->
                    favoriteSources.any { it.equals(source, ignoreCase = true) }
                }.thenBy(String.CASE_INSENSITIVE_ORDER) { it },
            )
        }
    val filteredSources =
        remember(sortedSources, query) {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isEmpty()) {
                sortedSources
            } else {
                sortedSources.filter { it.contains(trimmedQuery, ignoreCase = true) }
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_sources)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_sources)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item(key = "source-list:all") {
                        ReaderSourceListRow(
                            label = stringResource(R.string.filter_all_sources),
                            selected = selectedSources.isEmpty(),
                            favorite = null,
                            onClick = onClearSources,
                            onToggleFavorite = null,
                        )
                    }
                    items(filteredSources, key = { "source-list:source:$it" }) { source ->
                        val selected = selectedSources.any { it.equals(source, ignoreCase = true) }
                        val favorite = favoriteSources.any { it.equals(source, ignoreCase = true) }
                        ReaderSourceListRow(
                            label = source,
                            selected = selected,
                            favorite = favorite,
                            onClick = { onSelectSource(source) },
                            onToggleFavorite = { onToggleFavorite(source) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun ReaderSourceListRow(
    label: String,
    selected: Boolean,
    favorite: Boolean?,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)?,
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
            } else {
                Spacer(Modifier.size(24.dp))
            }
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (onToggleFavorite != null && favorite != null) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription =
                            stringResource(
                                if (favorite) {
                                    R.string.remove_source_favorite
                                } else {
                                    R.string.add_source_favorite
                                },
                            ),
                    )
                }
            }
        }
        HorizontalDivider()
    }
}
