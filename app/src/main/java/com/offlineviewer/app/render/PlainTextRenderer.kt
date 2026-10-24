package com.offlineviewer.app.render

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.offlineviewer.app.render.logic.HexDump

/**
 * The universal fallback renderer. It handles [text/plain] and the common
 * plain-text extensions, but the [RendererRegistry] also uses it as the
 * last-resort renderer for ANY file so nothing ever fails to display.
 *
 * Decoded text is shown verbatim in a scrollable monospace block. When the
 * file was loaded as raw bytes (no text payload), a classic hex + ASCII preview
 * is shown in the same monospace container instead of attempting to decode the
 * binary data, so even unknown formats open with something meaningful.
 */
class PlainTextRenderer : FileRenderer {

    override val formatName: String = "Plain text"

    override val supportedMimeTypes: Set<String> = setOf("text/plain")

    override val supportedExtensions: Set<String> = setOf("txt", "log", "text")

    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val body = file.text ?: binaryPreview(file)
        SelectionContainer(modifier = modifier) {
            Text(
                text = body,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            )
        }
    }

    private fun binaryPreview(file: LoadedFile): String {
        val bytes = file.bytes
        val size = bytes?.size?.toLong() ?: file.sizeBytes
        val header = "${file.displayName} ($size bytes)\n\n"
        if (bytes == null || bytes.isEmpty()) {
            return header + "No content to preview."
        }
        return header + HexDump.format(bytes)
    }
}
