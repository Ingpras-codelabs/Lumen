package com.offlineviewer.app.render

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineviewer.app.render.logic.DocxParser

/**
 * Renders a Word document (.docx) offline. The archive is read as raw bytes and
 * the ordered paragraphs and simple tables are extracted by the pure
 * [DocxParser]. Content shows as scrollable, selectable text; tables render as
 * a lightweight bordered grid. A document with no recoverable text degrades to
 * a clear error state.
 *
 * This is content extraction, not pixel-perfect layout: fonts, images and
 * complex formatting are intentionally not reproduced.
 */
class DocxRenderer : FileRenderer {

    override val formatName: String = "Word document"
    override val supportedMimeTypes: Set<String> =
        setOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    override val supportedExtensions: Set<String> = setOf("docx")

    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val document = remember(file) {
            val bytes = file.bytes
            if (bytes == null) DocxParser.Document(emptyList()) else DocxParser.parse(bytes)
        }

        if (document.isEmpty) {
            RendererError(
                message = "No readable text could be extracted from this document. " +
                    "It may be empty, malformed, or an unsupported legacy format.",
                modifier = modifier,
            )
            return
        }

        SelectionContainer(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                for (block in document.blocks) {
                    when (block) {
                        is DocxParser.Paragraph -> ParagraphBlock(block.text)
                        is DocxParser.Table -> TableBlock(block.rows)
                    }
                }
            }
        }
    }

    @Composable
    private fun ParagraphBlock(text: String) {
        if (text.isEmpty()) {
            // Preserve blank lines as vertical spacing.
            Text(text = " ", style = MaterialTheme.typography.bodyMedium)
            return
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        )
    }

    @Composable
    private fun TableBlock(rows: List<List<String>>) {
        if (rows.isEmpty()) return
        val columnCount = rows.maxOf { it.size }
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
        ) {
            rows.forEachIndexed { index, cells ->
                val isHeader = index == 0
                Row(
                    modifier = Modifier.background(
                        if (isHeader) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    for (col in 0 until columnCount) {
                        Text(
                            text = cells.getOrNull(col) ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 10,
                            modifier = Modifier
                                .width(160.dp)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
