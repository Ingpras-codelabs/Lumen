package com.offlineviewer.app.render

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.offlineviewer.app.render.logic.RtfConverter

/**
 * Renders RTF offline by converting common control words (bold/italic/
 * underline, paragraphs, tabs, unicode/hex escapes) into a styled
 * AnnotatedString via the pure [RtfConverter]. Unknown control words are
 * ignored gracefully, so malformed RTF never crashes the app.
 */
class RtfRenderer : FileRenderer {

    override val formatName: String = "Rich Text"
    override val supportedMimeTypes: Set<String> = setOf("application/rtf", "text/rtf")
    override val supportedExtensions: Set<String> = setOf("rtf")

    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val source = file.text
        if (source == null) {
            RendererError(message = "This RTF file could not be read as text.", modifier = modifier)
            return
        }

        val annotated = remember(file) {
            val runs = try {
                RtfConverter.convert(source)
            } catch (e: Throwable) {
                listOf(RtfConverter.Run(source))
            }
            buildAnnotatedString {
                for (run in runs) {
                    withStyle(
                        SpanStyle(
                            fontWeight = if (run.bold) FontWeight.Bold else null,
                            fontStyle = if (run.italic) FontStyle.Italic else null,
                            textDecoration = if (run.underline) TextDecoration.Underline else null,
                        ),
                    ) {
                        append(run.text)
                    }
                }
            }
        }

        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        )
    }
}
