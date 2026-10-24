package com.offlineviewer.app.render

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineviewer.app.render.logic.DelimitedParser

/**
 * Renders CSV/TSV as a horizontally- and vertically-scrollable table with a
 * sticky-styled header row. Parsing uses the dependency-free
 * [DelimitedParser] (quoted fields, embedded delimiters/newlines, ragged rows).
 *
 * Very large tables are capped at [MAX_ROWS] rendered rows with a notice so the
 * UI stays responsive and never OOMs on huge files.
 */
class DelimitedTextRenderer : FileRenderer {

    override val formatName: String = "Table"
    override val supportedMimeTypes: Set<String> = setOf("text/csv", "text/tab-separated-values")
    override val supportedExtensions: Set<String> = setOf("csv", "tsv")

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val source = file.text
        if (source.isNullOrEmpty()) {
            RendererError(
                message = "This file is empty or could not be read as text.",
                modifier = modifier,
            )
            return
        }

        val table = remember(file) {
            val delimiter = DelimitedParser.delimiterFor(file.extension, file.mimeType)
            DelimitedParser.parse(source, delimiter)
        }

        if (table.rowCount == 0 || table.columnCount == 0) {
            RendererError(message = "No tabular data found.", modifier = modifier)
            return
        }

        val columnCount = table.columnCount
        val header = table.rows.first()
        val bodyRows = table.rows.drop(1)
        val capped = bodyRows.size > MAX_ROWS
        val shownRows = if (capped) bodyRows.take(MAX_ROWS) else bodyRows

        Column(modifier = modifier.fillMaxSize()) {
            if (capped) {
                FallbackNotice(
                    message = "Large table: showing the first $MAX_ROWS of ${bodyRows.size} data rows.",
                )
            }
            val hScroll = rememberScrollState()
            Column(modifier = Modifier.horizontalScroll(hScroll)) {
                // Header row.
                TableRow(
                    cells = header,
                    columnCount = columnCount,
                    isHeader = true,
                )
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

    companion object {
        const val MAX_ROWS = 2000
        private const val CELL_WIDTH = 160
    }
}
