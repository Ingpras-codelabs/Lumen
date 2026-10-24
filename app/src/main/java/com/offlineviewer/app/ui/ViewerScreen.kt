package com.offlineviewer.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.offlineviewer.app.R
import com.offlineviewer.app.data.RecentFilesLogic
import com.offlineviewer.app.render.LoadedFile
import com.offlineviewer.app.ui.components.EmptyState
import com.offlineviewer.app.ui.components.ErrorState
import com.offlineviewer.app.ui.components.LoadingState
import com.offlineviewer.app.ui.components.TextFindView
import com.offlineviewer.app.ui.theme.ThemeMode

/**
 * Top-level viewer screen. Material 3 Scaffold + TopAppBar showing the file
 * name and detected format, with an overflow menu (open another, share,
 * format details, theme). The content area dispatches to the resolved renderer
 * and adapts its horizontal padding to the available width via [WindowSizeClass].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    state: ViewerUiState,
    onOpenFile: () -> Unit,
    windowSizeClass: WindowSizeClass,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onShare: (LoadedFile) -> Unit,
    onReturnHome: () -> Unit,
    onExit: () -> Unit,
    recentFiles: List<RecentFilesLogic.Entry> = emptyList(),
    onOpenRecent: (RecentFilesLogic.Entry) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val content = state as? ViewerUiState.Content
    val title = content?.file?.displayName ?: "Offline Viewer"
    val subtitle = content?.renderer?.formatName

    var menuExpanded by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var findActive by remember { mutableStateOf(false) }

    val clipboard = LocalClipboardManager.current
    // Text-based content exposes a decoded text payload; those renderers
    // (plain text / source code / structured data) support in-document find
    // and copy-all. Binary content (images, archives, Office) does not.
    val searchableText = content?.file?.text
    // A new file resets the find view so it never lingers across documents.
    LaunchedEffect(content?.file) { findActive = false }

    // System back handling. The two handlers' enabled flags are exact
    // complements (see BackNavigation), so precisely one intercepts back for
    // any given state. When a file/error/load is on screen, back closes it and
    // returns home; at the root (Empty) back asks before leaving the app. The
    // SAF picker and share sheet run in their own activities, so neither is
    // affected by these handlers.
    BackHandler(enabled = BackNavigation.shouldReturnHome(state)) {
        onReturnHome()
    }
    BackHandler(enabled = BackNavigation.shouldConfirmExit(state)) {
        showExitDialog = true
    }
    // While the in-document find view is open, back closes it first (it is
    // registered last so it takes precedence over the return-home handler).
    BackHandler(enabled = findActive) {
        findActive = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (searchableText != null) {
                        IconButton(onClick = { findActive = !findActive }) {
                            Icon(Icons.Filled.Search, contentDescription = "Find in file")
                        }
                    }
                    IconButton(onClick = onOpenFile) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Open a file")
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (content != null) {
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = { menuExpanded = false; onShare(content.file) },
                                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text("File details") },
                                onClick = { menuExpanded = false; showDetails = true },
                                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                            )
                        }
                        if (searchableText != null) {
                            DropdownMenuItem(
                                text = { Text("Find in file") },
                                onClick = { menuExpanded = false; findActive = true },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Copy all text") },
                                onClick = {
                                    menuExpanded = false
                                    clipboard.setText(AnnotatedString(searchableText))
                                },
                                leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Open another file") },
                            onClick = { menuExpanded = false; onOpenFile() },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Theme") },
                            onClick = { menuExpanded = false; showThemeDialog = true },
                            leadingIcon = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val horizontalPadding = when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Expanded -> 48.dp
            WindowWidthSizeClass.Medium -> 24.dp
            else -> 0.dp
        }
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = horizontalPadding)

        when (state) {
            is ViewerUiState.Empty -> EmptyState(
                onOpenFile = onOpenFile,
                recentFiles = recentFiles,
                onOpenRecent = onOpenRecent,
                modifier = contentModifier,
            )
            is ViewerUiState.Loading -> LoadingState(contentModifier)
            is ViewerUiState.Error -> ErrorState(state.message, onOpenFile, contentModifier)
            is ViewerUiState.Content ->
                if (findActive && searchableText != null) {
                    TextFindView(
                        text = searchableText,
                        onClose = { findActive = false },
                        modifier = contentModifier,
                    )
                } else {
                    state.renderer.Render(state.file, contentModifier)
                }
        }
    }

    if (showDetails && content != null) {
        FileDetailsDialog(file = content.file, format = subtitle, onDismiss = { showDetails = false })
    }
    if (showThemeDialog) {
        ThemeChooserDialog(
            current = themeMode,
            onSelect = { onThemeModeChange(it); showThemeDialog = false },
            onDismiss = { showThemeDialog = false },
        )
    }
    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = { showExitDialog = false; onExit() },
            onDismiss = { showExitDialog = false },
        )
    }
}

/**
 * Confirms leaving the app when back is pressed at the home state. Cancel
 * keeps the app open; Exit lets the hosting activity finish. Follows the same
 * confirm/dismiss TextButton layout as the other dialogs on this screen.
 */
@Composable
private fun ExitConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.exit_dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.exit_dialog_cancel)) }
        },
        title = { Text(stringResource(R.string.exit_dialog_title)) },
        text = { Text(stringResource(R.string.exit_dialog_message)) },
    )
}

/** Shows the detected MIME type, extension, and human-readable size. */
@Composable
private fun FileDetailsDialog(
    file: LoadedFile,
    format: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("File details") },
        text = {
            Column {
                DetailRow("Name", file.displayName)
                format?.let { DetailRow("Format", it) }
                DetailRow("Type", file.mimeType ?: "unknown")
                DetailRow("Extension", if (file.extension.isBlank()) "none" else ".${file.extension}")
                DetailRow("Size", formatSize(file.sizeBytes))
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** System / Light / Dark chooser persisted by the caller. */
@Composable
private fun ThemeChooserDialog(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Theme") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    val selected = mode == current
                    Row(
                        label = mode.label(),
                        selected = selected,
                        onClick = { onSelect(mode) },
                    )
                }
            }
        },
    )
}

@Composable
private fun Row(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(min = 200.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

/** Human-readable byte size (e.g. "1.2 MB"). */
private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.1f GB", gb)
}
