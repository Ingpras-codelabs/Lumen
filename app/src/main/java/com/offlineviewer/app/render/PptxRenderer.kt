package com.offlineviewer.app.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineviewer.app.render.logic.PptxParser

/**
 * Renders a presentation (.pptx) offline. The archive is read as raw bytes and
 * the per-slide text is extracted in slide order by the pure [PptxParser].
 * Slides are shown one after another with a clear numbered separator. A deck
 * with no recoverable text degrades to a clear error state.
 *
 * This is content extraction, not pixel-perfect layout: images, shapes,
 * animations and positioning are intentionally not reproduced.
 */
class PptxRenderer : FileRenderer {

    override val formatName: String = "Presentation"
    override val supportedMimeTypes: Set<String> =
        setOf("application/vnd.openxmlformats-officedocument.presentationml.presentation")
    override val supportedExtensions: Set<String> = setOf("pptx")

    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val deck = remember(file) {
            val bytes = file.bytes
            if (bytes == null) PptxParser.Deck(emptyList()) else PptxParser.parse(bytes)
        }

        if (deck.isEmpty) {
            RendererError(
                message = "No readable text could be extracted from this presentation. " +
                    "It may be empty, malformed, or an unsupported legacy format.",
                modifier = modifier,
            )
            return
        }

        SelectionContainer(modifier = modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(deck.slides) { index, slide ->
                    SlideBlock(number = index + 1, total = deck.slides.size, lines = slide.lines)
                }
            }
        }
    }

    @Composable
    private fun SlideBlock(number: Int, total: Int, lines: List<String>) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Slide $number of $total",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (lines.isEmpty()) {
                    Text(
                        text = "(No text on this slide)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    for (line in lines) {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
