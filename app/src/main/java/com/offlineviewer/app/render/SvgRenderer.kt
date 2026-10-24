package com.offlineviewer.app.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Picture
import android.graphics.drawable.PictureDrawable
import android.widget.ImageView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import com.caverock.androidsvg.SVG

/**
 * Renders SVG fully offline via AndroidSVG (Apache-2.0). The SVG is drawn to a
 * [Picture] wrapped in a [PictureDrawable] inside an [ImageView] (software
 * layer, since Picture drawables are not hardware-accelerated), and the whole
 * view supports pinch-to-zoom and pan via a Compose [graphicsLayer].
 *
 * Parsing happens off the Render path's hot loop (remembered per file) and any
 * parse failure degrades to a clear [RendererError] instead of crashing.
 */
class SvgRenderer : FileRenderer {

    override val formatName: String = "SVG"
    override val supportedMimeTypes: Set<String> = setOf("image/svg+xml")
    override val supportedExtensions: Set<String> = setOf("svg")

    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val parsed = remember(file) { parse(file) }

        if (parsed == null) {
            RendererError(
                message = "The SVG could not be parsed. It may be malformed or use unsupported features.",
                modifier = modifier,
            )
            return
        }

        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = modifier
                .fillMaxSize()
                .semantics { contentDescription = "SVG image: ${file.displayName}" }
                .pointerInput(parsed) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .pointerInput(parsed) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f; offsetX = 0f; offsetY = 0f
                            } else {
                                scale = DOUBLE_TAP_SCALE
                            }
                        },
                    )
                },
        ) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageDrawable(PictureDrawable(parsed))
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                    ),
            )
        }
    }

    private fun parse(file: LoadedFile): Picture? = try {
        val svg = when {
            file.text != null -> SVG.getFromString(file.text)
            file.bytes != null -> SVG.getFromInputStream(file.bytes.inputStream())
            else -> null
        }
        svg?.let { doc ->
            // Give the document a sane default size when it declares none.
            if (doc.documentWidth <= 0f) doc.setDocumentWidth("100%")
            if (doc.documentHeight <= 0f) doc.setDocumentHeight("100%")
            val width = if (doc.documentWidth > 0f) doc.documentWidth.toInt() else 1024
            val height = if (doc.documentHeight > 0f) doc.documentHeight.toInt() else 1024
            val picture: Picture = doc.renderToPicture(
                width.coerceIn(1, 4096),
                height.coerceIn(1, 4096),
            )
            // Force a non-transparent background behind transparent SVGs so they
            // are visible on dark themes too is left to the view; return as-is.
            picture
        }
    } catch (e: Throwable) {
        null
    }

    @Suppress("unused")
    private fun blankPicture(): Picture = Picture().apply {
        val c: Canvas = beginRecording(1, 1)
        c.drawColor(Color.TRANSPARENT)
        endRecording()
    }

    private companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 10f
        const val DOUBLE_TAP_SCALE = 2.5f
    }
}
