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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

/** Always-visible entry to the saved RSS subscriptions, independent of loaded story count. */
@Composable
internal fun ReaderSubscriptionBar(
    selectedTitle: String?,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.reader_subscriptions),
            style = MaterialTheme.typography.labelMedium,
        )
        OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
            Text(
                text = selectedTitle ?: stringResource(R.string.reader_all_subscriptions),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
    }
}

/** Selecting one subscription switches the Reader to its cached/fetched feed URL. */
@Composable
internal fun ReaderSourceListDialog(
    subscriptions: List<ReaderSubscription>,
    selectedFeedUrl: String?,
    favoriteSources: Set<String>,
    onSelectFeed: (String) -> Unit,
    onAllFeeds: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val sorted = remember(subscriptions, favoriteSources) {
        subscriptions.sortedWith(
            compareByDescending<ReaderSubscription> { sub ->
                favoriteSources.any { it.equals(sub.title, ignoreCase = true) }
            }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
        )
    }
    val filtered = remember(sorted, query) {
        val term = query.trim()
        if (term.isEmpty()) sorted
        else sorted.filter { it.title.contains(term, ignoreCase = true) ||
            it.url.contains(term, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_subscriptions)) },
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
                    item(key = "subscription:all") {
                        ReaderSubscriptionRow(
                            title = stringResource(R.string.reader_all_subscriptions),
                            url = null,
                            selected = selectedFeedUrl == null,
                            favorite = null,
                            onClick = onAllFeeds,
                            onToggleFavorite = null,
                        )
                    }
                    items(filtered, key = { it.url }) { subscription ->
                        val favorite = favoriteSources.any {
                            it.equals(subscription.title, ignoreCase = true)
                        }
                        ReaderSubscriptionRow(
                            title = subscription.title,
                            url = subscription.url,
                            selected = selectedFeedUrl == subscription.url,
                            favorite = favorite,
                            onClick = { onSelectFeed(subscription.url) },
                            onToggleFavorite = { onToggleFavorite(subscription.title) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun ReaderSubscriptionRow(
    title: String,
    url: String?,
    selected: Boolean,
    favorite: Boolean?,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)?,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
            } else {
                Spacer(Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (url != null) {
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (onToggleFavorite != null && favorite != null) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = stringResource(
                            if (favorite) R.string.remove_source_favorite
                            else R.string.add_source_favorite,
                        ),
                    )
                }
            }
        }
        HorizontalDivider()
    }
}
