package com.offlineviewer.app.render

import androidx.compose.foundation.layout.Column
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
 * Minimal format-aware renderers.
 *
 * FEAT-002 only needs the architecture to be real and extensible; the full
 * format-specific rendering (styled Markdown, rasterised SVG, CSV tables, etc.)
 * lands in FEAT-003. For now each of these advertises the correct MIME
 * types / extensions so the [RendererRegistry] resolution is exercised, and
 * renders the source text behind a format banner so every file is still
 * readable. FEAT-003 replaces the body of [Render] without touching the
 * registry wiring.
 */
abstract class SourceTextRenderer(
    override val formatName: String,
    override val supportedMimeTypes: Set<String>,
    override val supportedExtensions: Set<String>,
) : FileRenderer {

    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = "$formatName (preview)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = file.text ?: "Binary content (${file.sizeBytes} bytes)",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Markdown renderer stub. FEAT-003 swaps in a Markwon-backed implementation. */
class MarkdownRenderer : SourceTextRenderer(
    formatName = "Markdown",
    supportedMimeTypes = setOf("text/markdown", "text/x-markdown"),
    supportedExtensions = setOf("md", "markdown"),
)

/** SVG renderer stub. FEAT-003 swaps in an AndroidSVG-backed implementation. */
class SvgRenderer : SourceTextRenderer(
    formatName = "SVG",
    supportedMimeTypes = setOf("image/svg+xml"),
    supportedExtensions = setOf("svg"),
)

/** CSV / TSV renderer stub. FEAT-003 renders a proper table. */
class DelimitedTextRenderer : SourceTextRenderer(
    formatName = "Table",
    supportedMimeTypes = setOf("text/csv", "text/tab-separated-values"),
    supportedExtensions = setOf("csv", "tsv"),
)

/** JSON / XML / YAML renderer stub. FEAT-003 pretty-prints and highlights. */
class StructuredDataRenderer : SourceTextRenderer(
    formatName = "Structured data",
    supportedMimeTypes = setOf(
        "application/json", "text/json",
        "application/xml", "text/xml",
        "text/yaml", "application/x-yaml", "application/yaml",
    ),
    supportedExtensions = setOf("json", "xml", "yaml", "yml"),
)

/** HTML renderer stub. FEAT-003 renders via an offline (JS-disabled) WebView. */
class HtmlRenderer : SourceTextRenderer(
    formatName = "HTML",
    supportedMimeTypes = setOf("text/html"),
    supportedExtensions = setOf("html", "htm"),
)

/** Source-code renderer stub. FEAT-003 adds syntax highlighting. */
class SourceCodeRenderer : SourceTextRenderer(
    formatName = "Source code",
    supportedMimeTypes = setOf("text/x-java", "text/x-kotlin", "text/x-python", "application/javascript"),
    supportedExtensions = setOf(
        "kt", "java", "js", "ts", "py", "c", "cpp", "h", "cs",
        "go", "rb", "rs", "sh", "sql", "css",
    ),
)
