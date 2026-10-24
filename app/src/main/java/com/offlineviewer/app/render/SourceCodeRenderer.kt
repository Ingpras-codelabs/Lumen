package com.offlineviewer.app.render

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.offlineviewer.app.render.logic.CodeLanguage
import com.offlineviewer.app.render.logic.SyntaxHighlighter
import com.offlineviewer.app.ui.theme.LocalThemeIsDark

/**
 * Renders source code with lightweight regex syntax highlighting selected by
 * file extension via [CodeLanguage]. Unknown languages fall back to plain
 * monospace. The content is both vertically and horizontally scrollable so long
 * lines are not truncated.
 */
class SourceCodeRenderer : FileRenderer {

    override val formatName: String = "Source code"
    override val supportedMimeTypes: Set<String> = setOf(
        "text/x-java", "text/x-kotlin", "text/x-python",
        "application/javascript", "application/x-sh",
        "text/x-csrc", "text/x-c++src",
    )
    override val supportedExtensions: Set<String> = CodeLanguage.allExtensions

    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val source = file.text
        if (source == null) {
            RendererError(message = "This file could not be read as text.", modifier = modifier)
            return
        }

        val dark = LocalThemeIsDark.current
        val language = remember(file) { CodeLanguage.fromExtension(file.extension) }
        val annotated: AnnotatedString = remember(file, dark) {
            try {
                SyntaxHighlighter.highlightCode(source, language, dark)
            } catch (e: Throwable) {
                AnnotatedString(source)
            }
        }

        Column(modifier = modifier.fillMaxSize()) {
            SelectionContainer {
                Text(
                    text = annotated,
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
    }
}
