package com.offlineviewer.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineviewer.app.render.logic.TextSearch

/** Minimum touch-target size shared with the other state screens. */
private val MinTouchTarget = 48.dp

/**
 * An in-document find view for text-based content (plain text / source code /
 * structured data). It shows a search field plus previous/next controls, finds
 * every match of the query via the pure [TextSearch] helper, highlights all
 * matches, and emphasises the active one. Kept framework-light: the match
 * computation is pure and unit-tested; this composable only renders the result.
 *
 * The highlighted text stays inside a [SelectionContainer] so selecting and
 * copying still works while searching.
 */
@Composable
fun TextFindView(
    text: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var activeMatch by remember { mutableIntStateOf(0) }

    val matches = remember(text, query) { TextSearch.findMatches(text, query) }
    val total = matches.size
    val active = remember(total, activeMatch) { TextSearch.wrapIndex(activeMatch, total) }

    val highlight = MaterialTheme.colorScheme.primary
    val activeHighlight = MaterialTheme.colorScheme.tertiary
    val onHighlight = MaterialTheme.colorScheme.onPrimary

    val annotated = remember(text, matches, active, highlight, activeHighlight, onHighlight) {
        buildAnnotatedString {
            append(text)
            matches.forEachIndexed { index, match ->
                val background = if (index == active) activeHighlight else highlight
                addStyle(
                    SpanStyle(
                        background = background,
                        color = onHighlight,
                        fontWeight = if (index == active) FontWeight.Bold else FontWeight.Normal,
                    ),
                    match.start,
                    match.end,
                )
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; activeMatch = 0 },
                    singleLine = true,
                    label = { Text("Find in file") },
                    modifier = Modifier.weight(1f),
                )
                val counter = when {
                    query.isEmpty() -> ""
                    total == 0 -> "0/0"
                    else -> "${active + 1}/$total"
                }
                if (counter.isNotEmpty()) {
                    Text(
                        text = counter,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { if (total > 0) activeMatch = active - 1 },
                    enabled = total > 0,
                    modifier = Modifier.size(MinTouchTarget),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous match")
                }
                IconButton(
                    onClick = { if (total > 0) activeMatch = active + 1 },
                    enabled = total > 0,
                    modifier = Modifier.size(MinTouchTarget),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next match")
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(MinTouchTarget),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close find")
                }
            }
        }

        SelectionContainer {
            Text(
                text = annotated,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp),
            )
        }
    }
}
