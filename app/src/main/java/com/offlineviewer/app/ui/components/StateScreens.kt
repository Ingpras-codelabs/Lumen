package com.offlineviewer.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.offlineviewer.app.data.RecentFilesLogic
import com.offlineviewer.app.render.SupportedFormats
import com.offlineviewer.app.ui.theme.AppTheme

/** Shared minimum touch-target size for interactive controls (WCAG / Material). */
private val MinTouchTarget = 48.dp

/**
 * Centered progress indicator shown while a file is being read.
 * The indicator carries a text label so TalkBack announces the busy state.
 */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            text = "Opening file…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/**
 * Friendly empty / home prompt: a clear value proposition, a prominent
 * "Open a file" button, and the list of supported formats as chips. Scrolls on
 * small screens and is width-capped on large ones for comfortable reading.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyState(
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier,
    recentFiles: List<RecentFilesLogic.Entry> = emptyList(),
    onOpenRecent: (RecentFilesLogic.Entry) -> Unit = {},
    contentMaxWidth: androidx.compose.ui.unit.Dp = 520.dp,
) {
    var formatQuery by remember { mutableStateOf("") }
    val groupedFormats = remember(formatQuery) {
        SupportedFormats.grouped(SupportedFormats.filter(formatQuery))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 24.dp)
                .size(56.dp),
        )
        Text(
            text = "Open any file, offline",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Pick a local file to view it instantly. No internet, no account, no tracking.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = contentMaxWidth),
        )
        Button(
            onClick = onOpenFile,
            modifier = Modifier
                .padding(top = 8.dp)
                .sizeIn(minHeight = MinTouchTarget),
        ) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null)
            Text("Open a file", modifier = Modifier.padding(start = 8.dp))
        }

        if (recentFiles.isNotEmpty()) {
            RecentFilesSection(
                recentFiles = recentFiles,
                onOpenRecent = onOpenRecent,
                contentMaxWidth = contentMaxWidth,
            )
        }

        Text(
            text = "Supported formats",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp),
        )
        OutlinedTextField(
            value = formatQuery,
            onValueChange = { formatQuery = it },
            singleLine = true,
            label = { Text("Search formats") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = contentMaxWidth),
        )
        if (groupedFormats.isEmpty()) {
            Text(
                text = "No formats match \"$formatQuery\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            groupedFormats.forEach { (category, formats) ->
                Column(
                    modifier = Modifier.widthIn(max = contentMaxWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        formats.forEach { format ->
                            AssistChip(
                                onClick = onOpenFile,
                                label = { Text(format.label) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A "Recent" section of tappable rows that reopen a previously viewed file.
 * Each row meets the 48dp touch-target minimum and carries a content
 * description for TalkBack. Shown only when [recentFiles] is non-empty.
 */
@Composable
private fun RecentFilesSection(
    recentFiles: List<RecentFilesLogic.Entry>,
    onOpenRecent: (RecentFilesLogic.Entry) -> Unit,
    contentMaxWidth: androidx.compose.ui.unit.Dp,
) {
    Text(
        text = "Recent",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 24.dp),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = contentMaxWidth),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        recentFiles.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinTouchTarget)
                    .clickable { onOpenRecent(entry) }
                    .semantics { contentDescription = "Reopen ${entry.displayName}" }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

/**
 * Error state: a non-colour icon + a clear headline + the specific message +
 * a retry/open-another action, so the failure is communicated by text and
 * shape, never colour alone.
 */
@Composable
fun ErrorState(
    message: String,
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = "Couldn't open this file",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        OutlinedButton(
            onClick = onOpenFile,
            modifier = Modifier
                .padding(top = 16.dp)
                .sizeIn(minHeight = MinTouchTarget),
        ) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null)
            Text("Open another file", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

// --- Previews (design-time sanity checks; no device required) ---

@Preview(name = "Loading - light", showBackground = true)
@Preview(name = "Loading - dark", showBackground = true, uiMode = 0x20)
@Composable
private fun LoadingStatePreview() {
    AppTheme { LoadingState() }
}

@Preview(name = "Empty - light", showBackground = true)
@Preview(name = "Empty - dark", showBackground = true, uiMode = 0x20)
@Composable
private fun EmptyStatePreview() {
    AppTheme { EmptyState(onOpenFile = {}) }
}

@Preview(name = "Error - light", showBackground = true)
@Preview(name = "Error - dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ErrorStatePreview() {
    AppTheme {
        ErrorState(message = "This file is too large to open (48 MB).", onOpenFile = {})
    }
}
