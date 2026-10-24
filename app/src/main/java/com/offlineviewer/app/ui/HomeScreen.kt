package com.offlineviewer.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.offlineviewer.app.ui.components.EmptyState
import com.offlineviewer.app.ui.theme.AppTheme

/**
 * Shown when no file is loaded. Thin wrapper around the reusable
 * [EmptyState] so the viewer and the home route share one implementation.
 */
@Composable
fun HomeScreen(
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(onOpenFile = onOpenFile, modifier = modifier)
}

@Preview(name = "Home - light", showBackground = true)
@Preview(name = "Home - dark", showBackground = true, uiMode = 0x20)
@Composable
private fun HomeScreenPreview() {
    AppTheme { HomeScreen(onOpenFile = {}) }
}
