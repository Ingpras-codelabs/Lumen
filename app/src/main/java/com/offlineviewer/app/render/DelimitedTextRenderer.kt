package com.offlineviewer.app.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.offlineviewer.app.render.logic.DelimitedParser

/**
 * Renders CSV/TSV as a horizontally- and vertically-scrollable table via the
 * shared [TableView]. Parsing uses the dependency-free [DelimitedParser]
 * (quoted fields, embedded delimiters/newlines, ragged rows). Very large tables
 * are capped by [TableView.MAX_ROWS] so the UI stays responsive.
 */
class DelimitedTextRenderer : FileRenderer {

    override val formatName: String = "Table"
    override val supportedMimeTypes: Set<String> = setOf("text/csv", "text/tab-separated-values")
    override val supportedExtensions: Set<String> = setOf("csv", "tsv")

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

        TableView.Render(rows = table.rows, modifier = modifier)
    }
}
