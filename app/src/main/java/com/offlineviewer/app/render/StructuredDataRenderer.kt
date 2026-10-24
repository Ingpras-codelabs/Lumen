package com.offlineviewer.app.render

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.offlineviewer.app.render.logic.JsonFormatter
import com.offlineviewer.app.render.logic.SyntaxHighlighter
import com.offlineviewer.app.render.logic.XmlFormatter
import com.offlineviewer.app.render.logic.YamlValidator

/**
 * Renders JSON / XML / YAML. Each is pretty-printed / validated by its pure
 * formatter and syntax-coloured; on a parse failure the renderer shows a
 * non-fatal notice and falls back to the raw source text so nothing is lost.
 */
class StructuredDataRenderer : FileRenderer {

    override val formatName: String = "Structured data"
    override val supportedMimeTypes: Set<String> = setOf(
        "application/json", "text/json",
        "application/xml", "text/xml",
        "text/yaml", "application/x-yaml", "application/yaml",
    )
    override val supportedExtensions: Set<String> = setOf("json", "xml", "yaml", "yml")

    private enum class Kind { JSON, XML, YAML }

    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val source = file.text
        if (source == null) {
            RendererError(message = "This file could not be read as text.", modifier = modifier)
            return
        }

        val result = remember(file) { prepare(file, source) }

        Column(modifier = modifier.fillMaxSize()) {
            result.notice?.let { FallbackNotice(message = it) }
            Text(
                text = result.annotated,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp),
            )
        }
    }

    private data class Prepared(val annotated: AnnotatedString, val notice: String?)

    private fun prepare(file: LoadedFile, source: String): Prepared {
        return when (kindOf(file)) {
            Kind.JSON -> when (val r = JsonFormatter.format(source)) {
                is JsonFormatter.Result.Formatted ->
                    Prepared(SyntaxHighlighter.highlightJson(r.text), null)
                is JsonFormatter.Result.Invalid ->
                    Prepared(AnnotatedString(source), "Could not parse as JSON (${r.reason}). Showing raw text.")
            }
            Kind.XML -> when (val r = XmlFormatter.format(source)) {
                is XmlFormatter.Result.Formatted ->
                    Prepared(SyntaxHighlighter.highlightXml(r.text), null)
                is XmlFormatter.Result.Invalid ->
                    Prepared(AnnotatedString(source), "Could not parse as XML (${r.reason}). Showing raw text.")
            }
            Kind.YAML -> when (val r = YamlValidator.validate(source)) {
                is YamlValidator.Result.Valid ->
                    Prepared(SyntaxHighlighter.highlightYaml(source), null)
                is YamlValidator.Result.Invalid ->
                    Prepared(AnnotatedString(source), "Could not parse as YAML (${r.reason}). Showing raw text.")
            }
        }
    }

    private fun kindOf(file: LoadedFile): Kind {
        val mime = file.mimeType?.lowercase().orEmpty()
        val ext = file.extension.lowercase()
        return when {
            ext == "json" || mime.contains("json") -> Kind.JSON
            ext == "xml" || mime.contains("xml") -> Kind.XML
            ext == "yaml" || ext == "yml" || mime.contains("yaml") -> Kind.YAML
            else -> Kind.JSON
        }
    }
}
