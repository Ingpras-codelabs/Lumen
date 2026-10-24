package com.offlineviewer.app.render

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * The universal fallback renderer. It handles [text/plain] and the common
 * plain-text extensions, but the [RendererRegistry] also uses it as the
 * last-resort renderer for ANY file so nothing ever fails to display.
 *
 * Decoded text is shown verbatim in a scrollable monospace block. When the
 * file was loaded as raw bytes (no text payload), a short descriptor is shown
 * instead of attempting to decode binary data.
 */
class PlainTextRenderer : FileRenderer {

    override val formatName: String = "Plain text"

    override val supportedMimeTypes: Set<String> = setOf("text/plain")

    override val supportedExtensions: Set<String> = setOf("txt", "log", "text")

    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val body = file.text ?: binaryDescriptor(file)
        Text(
            text = body,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        )
    }

    private fun binaryDescriptor(file: LoadedFile): String {
        val size = file.bytes?.size ?: file.sizeBytes
        return buildString {
            append("Binary content (")
            append(size)
            append(" bytes)\n")
            append("This file type cannot be shown as text.")
        }
    }
}
