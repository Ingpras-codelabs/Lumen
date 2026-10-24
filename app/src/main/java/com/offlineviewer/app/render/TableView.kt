package com.offlineviewer.app.render

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A horizontally- and vertically-scrollable table with a styled header row,
 * shared by the CSV/TSV renderer and the spreadsheet (.xlsx) renderer so both
 * present tabular data identically. Very large tables are capped at [MAX_ROWS]
 * rendered body rows with a notice so the UI stays responsive and never OOMs.
 */
object TableView {

    const val MAX_ROWS = 2000
    private const val CELL_WIDTH = 160

    /**
     * Renders [rows] as a scrollable table. The first row is treated as the
     * header. [overflowLabel] builds the "showing first N of M" notice when the
     * body is capped (callers may customise the wording per format).
     */
    @Composable
    fun Render(
        rows: List<List<String>>,
        modifier: Modifier = Modifier,
        overflowLabel: (shown: Int, total: Int) -> String = { shown, total ->
            "Large table: showing the first $shown of $total data rows."
        },
    ) {
        if (rows.isEmpty()) {
            RendererError(message = "No tabular data found.", modifier = modifier)
            return
        }

        val columnCount = rows.maxOf { it.size }
        val header = rows.first()
        val bodyRows = rows.drop(1)
        val capped = bodyRows.size > MAX_ROWS
        val shownRows = if (capped) bodyRows.take(MAX_ROWS) else bodyRows

        Column(modifier = modifier.fillMaxSize()) {
            if (capped) {
                FallbackNotice(message = overflowLabel(MAX_ROWS, bodyRows.size))
            }
            val hScroll = rememberScrollState()
            Column(modifier = Modifier.horizontalScroll(hScroll)) {
                TableRow(cells = header, columnCount = columnCount, isHeader = true)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(shownRows) { row ->
                        TableRow(cells = row, columnCount = columnCount, isHeader = false)
                    }
                }
            }
        }
    }

    @Composable
    private fun TableRow(cells: List<String>, columnCount: Int, isHeader: Boolean) {
        val bg = if (isHeader) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        }
        Row(modifier = Modifier.background(bg)) {
            for (col in 0 until columnCount) {
                val value = cells.getOrNull(col) ?: ""
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 6,
                    modifier = Modifier
                        .width(CELL_WIDTH.dp)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}
