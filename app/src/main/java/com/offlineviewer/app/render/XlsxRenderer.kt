package com.offlineviewer.app.render

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineviewer.app.render.logic.XlsxParser

/**
 * Renders a spreadsheet (.xlsx) offline. The archive is read as raw bytes and
 * the sheets are extracted by the pure [XlsxParser] (shared-string resolution,
 * cell-reference column ordering, workbook sheet order). Each sheet is drawn
 * with the shared [TableView] so it matches CSV/TSV rendering, capped at
 * [TableView.MAX_ROWS]. When the workbook has several sheets a tab selector
 * switches between them.
 *
 * This is content extraction, not pixel-perfect layout: formulas are shown as
 * their cached values and cell styling/charts are not reproduced.
 */
class XlsxRenderer : FileRenderer {

    override val formatName: String = "Spreadsheet"
    override val supportedMimeTypes: Set<String> =
        setOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    override val supportedExtensions: Set<String> = setOf("xlsx")

    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val workbook = remember(file) {
            val bytes = file.bytes
            if (bytes == null) XlsxParser.Workbook(emptyList()) else XlsxParser.parse(bytes)
        }

        if (workbook.isEmpty) {
            RendererError(
                message = "No readable cells could be extracted from this spreadsheet. " +
                    "It may be empty, malformed, or an unsupported legacy format.",
                modifier = modifier,
            )
            return
        }

        val sheets = workbook.sheets
        var selected by rememberSaveable(file.displayName) { mutableIntStateOf(0) }
        val index = selected.coerceIn(0, sheets.size - 1)

        Column(modifier = modifier.fillMaxSize()) {
            if (sheets.size > 1) {
                ScrollableTabRow(selectedTabIndex = index) {
                    sheets.forEachIndexed { i, sheet ->
                        Tab(
                            selected = i == index,
                            onClick = { selected = i },
                            text = {
                                Text(
                                    text = sheet.name.ifBlank { "Sheet ${i + 1}" },
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }
                }
            }
            TableView.Render(
                rows = sheets[index].rows,
                modifier = Modifier.fillMaxWidth(),
                overflowLabel = { shown, total ->
                    "Large sheet: showing the first $shown of $total data rows."
                },
            )
        }
    }
}
