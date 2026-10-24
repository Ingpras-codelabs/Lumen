package com.offlineviewer.app.render

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

/**
 * Renders HTML fully offline in a locked-down [WebView]:
 *  - JavaScript DISABLED
 *  - file/content access DISABLED
 *  - a [WebViewClient] that blocks every network/remote resource request
 *  - content injected via [WebView.loadDataWithBaseURL] with a null base URL
 *
 * This guarantees no network access even though the renderer uses a WebView,
 * matching the app's fully-offline contract.
 */
class HtmlRenderer : FileRenderer {

    override val formatName: String = "HTML"
    override val supportedMimeTypes: Set<String> = setOf("text/html")
    override val supportedExtensions: Set<String> = setOf("html", "htm")

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val html = file.text
        if (html == null) {
            RendererError(message = "This HTML file could not be read as text.", modifier = modifier)
            return
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    @Suppress("DEPRECATION")
                    run {
                        settings.allowFileAccessFromFileURLs = false
                        settings.allowUniversalAccessFromFileURLs = false
                    }
                    settings.blockNetworkLoads = true
                    settings.blockNetworkImage = true
                    settings.setGeolocationEnabled(false)
                    webViewClient = OfflineWebViewClient
                }
            },
            update = { web ->
                web.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            },
            modifier = modifier.fillMaxSize(),
        )
    }
}

/**
 * A [WebViewClient] that refuses to navigate anywhere and returns an empty
 * response for every subresource request, so nothing can ever touch the
 * network. Shared across HTML and EPUB renderers.
 */
object OfflineWebViewClient : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        // Block all navigation (no new page loads).
        return true
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val url = request.url?.scheme?.lowercase()
        // Allow the initial in-memory data: document; block everything else
        // (http/https/file/content) by returning an empty response.
        return if (url == null || url == "data") {
            null
        } else {
            WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
        }
    }
}
