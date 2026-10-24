package com.offlineviewer.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    contentMaxWidth: androidx.compose.ui.unit.Dp = 520.dp,
) {
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

        Text(
            text = "Supported formats",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp),
        )
        FlowRow(
            modifier = Modifier.widthIn(max = contentMaxWidth),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SupportedFormats.all.forEach { format ->
                AssistChip(
                    onClick = onOpenFile,
                    label = { Text(format.label) },
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
