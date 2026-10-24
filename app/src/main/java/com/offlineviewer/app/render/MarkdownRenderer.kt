package com.offlineviewer.app.render

import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.MaterialTheme
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin

/**
 * Renders Markdown fully offline with Markwon (Apache-2.0) into an
 * AndroidView(TextView). Supports headings, lists, tables, code blocks, task
 * lists, blockquotes, strikethrough and inline HTML.
 *
 * Crucially NO image/network plugin is installed, so no remote image fetching
 * can ever occur (the app also declares no INTERNET permission). Any rendering
 * failure degrades to showing the raw markdown text rather than crashing.
 */
class MarkdownRenderer : FileRenderer {

    override val formatName: String = "Markdown"
    override val supportedMimeTypes: Set<String> = setOf("text/markdown", "text/x-markdown")
    override val supportedExtensions: Set<String> = setOf("md", "markdown")

    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val source = file.text
        if (source == null) {
            RendererError(
                message = "This Markdown file could not be read as text.",
                modifier = modifier,
            )
            return
        }

        val context = LocalContext.current
        val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
        // Build Markwon once per context; no image/linkify-network plugins.
        val markwon = remember(context) {
            Markwon.builder(context)
                .usePlugin(CorePlugin.create())
                .usePlugin(TablePlugin.create(context))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TaskListPlugin.create(context))
                .usePlugin(HtmlPlugin.create())
                .build()
        }

        AndroidView(
            factory = { ctx ->
                TextView(ctx).apply {
                    setTextColor(textColor)
                    setTextIsSelectable(true)
                    // Only linkify already-present text; never load anything.
                    autoLinkMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES
                }
            },
            update = { tv ->
                tv.setTextColor(textColor)
                try {
                    markwon.setMarkdown(tv, source)
                } catch (e: Throwable) {
                    tv.text = source
                }
            },
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        )
    }
}
