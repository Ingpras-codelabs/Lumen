package com.offlineviewer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.offlineviewer.app.render.SupportedFormats

/**
 * Shown when no file is loaded. Prompts the user to open a local file via the
 * Storage Access Framework and lists the formats the app can display, so the
 * value proposition (fully offline, broad format support) is immediately clear.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier,
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
            modifier = Modifier.padding(top = 32.dp),
            tint = MaterialTheme.colorScheme.primary,
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
            modifier = Modifier.widthIn(max = 480.dp),
        )
        Button(onClick = onOpenFile, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null)
            Text("Open a file", modifier = Modifier.padding(start = 8.dp))
        }

        Text(
            text = "Supported formats",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp),
        )
        FlowRow(
            modifier = Modifier.widthIn(max = 520.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SupportedFormats.all.forEach { format ->
                AssistChip(
                    onClick = onOpenFile,
                    label = { Text(format.label) },
                    colors = AssistChipDefaults.assistChipColors(),
                )
            }
        }
    }
}
