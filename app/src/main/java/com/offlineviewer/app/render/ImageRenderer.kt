package com.offlineviewer.app.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Renders raster image formats that are not always previewable elsewhere
 * (webp, bmp, static gif, ico, plus png/jpeg) using Android's native
 * [BitmapFactory] decoders, with pinch-to-zoom and pan. SVG is intentionally
 * NOT handled here; the registry routes image/svg+xml to [SvgRenderer] first.
 *
 * A decode failure degrades to a clear error state rather than crashing.
 */
class ImageRenderer : FileRenderer {

    override val formatName: String = "Image"
    override val supportedMimeTypes: Set<String> = setOf(
        "image/webp", "image/bmp", "image/gif",
        "image/x-icon", "image/vnd.microsoft.icon",
        "image/png", "image/jpeg",
    )
    override val supportedExtensions: Set<String> = setOf(
        "webp", "bmp", "gif", "ico", "png", "jpg", "jpeg",
    )

    @Composable
    override fun Render(file: LoadedFile, modifier: Modifier) {
        val bitmap = remember(file) { decode(file) }

        if (bitmap == null) {
            RendererError(
                message = "This image could not be decoded. The format may be unsupported on this device.",
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
                .semantics { contentDescription = "Image: ${file.displayName}" }
                .pointerInput(bitmap) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .pointerInput(bitmap) {
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
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                // Decorative at the Image level; the Box carries the meaningful
                // description so TalkBack announces the image once.
                contentDescription = null,
                contentScale = ContentScale.Fit,
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

    private companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 10f
        const val DOUBLE_TAP_SCALE = 2.5f
    }

    private fun decode(file: LoadedFile): Bitmap? = try {
        val bytes = file.bytes ?: file.text?.toByteArray(Charsets.ISO_8859_1)
        if (bytes == null) {
            null
        } else {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    } catch (e: Throwable) {
        null
    }
}
