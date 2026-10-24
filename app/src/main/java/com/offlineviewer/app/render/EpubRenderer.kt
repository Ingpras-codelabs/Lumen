package com.offlineviewer.app.render

import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.offlineviewer.app.render.logic.EpubParser

/**
 * Renders an EPUB offline: the .epub is read as a ZIP, the OPF spine order is
 * resolved via the pure [EpubParser], and each chapter's XHTML is shown in the
 * same locked-down [WebView] used for HTML (JS off, network blocked). Simple
 * next/previous buttons move through the spine. A malformed archive shows a
 * clear error state.
 */
class EpubRenderer : FileRenderer {

    override val formatName: String = "EPUB"
    override val supportedMimeTypes: Set<String> = setOf("application/epub+zip")
    override val supportedExtensions: Set<String> = setOf("epub")

    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val book = remember(file) {
            val bytes = file.bytes
            if (bytes == null) EpubParser.Book(emptyList()) else EpubParser.parse(bytes)
        }

        if (book.chapters.isEmpty()) {
            RendererError(
                message = "This EPUB could not be opened. It may be malformed or empty.",
                modifier = modifier,
            )
            return
        }

        var index by rememberSaveable(file.displayName) { mutableIntStateOf(0) }
        val chapter = book.chapters[index.coerceIn(0, book.chapters.size - 1)]

        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { if (index > 0) index-- },
                    enabled = index > 0,
                ) { Text("Previous") }
                Text(
                    text = "Chapter ${index + 1} of ${book.chapters.size}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { if (index < book.chapters.size - 1) index++ },
                    enabled = index < book.chapters.size - 1,
                ) { Text("Next") }
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.blockNetworkLoads = true
                        settings.blockNetworkImage = true
                        webViewClient = OfflineWebViewClient
                    }
                },
                update = { web ->
                    web.loadDataWithBaseURL(null, chapter.html, "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            )
        }
    }
}
