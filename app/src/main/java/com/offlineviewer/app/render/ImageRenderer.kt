package com.offlineviewer.app.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
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
 * (webp, bmp, static gif, ico, wbmp, plus png/jpeg) using Android's native
 * decoders, with pinch-to-zoom and pan. SVG is intentionally NOT handled here;
 * the registry routes image/svg+xml to [SvgRenderer] first.
 *
 * HEIF/HEIC is additionally supported from API 28 (where platform HEIF decoding
 * landed) via [ImageDecoder]; on older devices, or on any decode failure, the
 * renderer degrades to a clear error state rather than crashing. Formats the
 * platform cannot reliably decode across minSdk 21..targetSdk 34 (e.g. TIFF,
 * AVIF) are deliberately NOT advertised to keep the supported-format claims
 * honest.
 */
class ImageRenderer : FileRenderer {

    override val formatName: String = "Image"
    override val supportedMimeTypes: Set<String> = setOf(
        "image/webp", "image/bmp", "image/gif",
        "image/x-icon", "image/vnd.microsoft.icon",
        "image/png", "image/jpeg",
        "image/vnd.wap.wbmp",
        // HEIF/HEIC: decoded via ImageDecoder on API 28+ (gated in decode()).
        "image/heif", "image/heic",
    )
    override val supportedExtensions: Set<String> = setOf(
        "webp", "bmp", "gif", "ico", "png", "jpg", "jpeg", "wbmp",
        "heif", "heic",
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

        /**
         * Target maximum decoded dimension. Images larger than this on either
         * axis are downsampled (power-of-two [BitmapFactory.Options.inSampleSize])
         * so a large file near the 32 MB load cap decodes to a bitmap that fits
         * comfortably in memory on low-RAM devices while still filling a typical
         * viewport when zoomed. 2048px keeps a full-screen image crisp.
         */
        const val TARGET_MAX_DIMENSION = 2048
    }

    /**
     * Decodes the image with a bounds-first pass so large images downsample
     * instead of OOMing. First reads only the dimensions
     * ([BitmapFactory.Options.inJustDecodeBounds]) to compute an
     * [BitmapFactory.Options.inSampleSize] against [TARGET_MAX_DIMENSION], then
     * decodes at that reduced resolution. The [Throwable] catch (covering
     * [OutOfMemoryError]) remains as a final safety net so a pathological image
     * degrades to an error state rather than crashing.
     */
    private fun decode(file: LoadedFile): Bitmap? = try {
        val bytes = file.bytes ?: file.text?.toByteArray(Charsets.ISO_8859_1)
        if (bytes == null) {
            null
        } else if (needsImageDecoder(file.mimeType, file.extension)) {
            decodeWithImageDecoder(bytes)
        } else {
            decodeWithBitmapFactory(bytes)
        }
    } catch (e: Throwable) {
        null
    }

    /** Standard bounds-first [BitmapFactory] path for the broadly-supported raster formats. */
    private fun decodeWithBitmapFactory(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, TARGET_MAX_DIMENSION)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    }

    /**
     * Decodes HEIF/HEIC via [ImageDecoder] (API 28+). Returns null on older
     * APIs so the caller shows the error state; the [ImageDecoder] path also
     * downsamples to [TARGET_MAX_DIMENSION] to stay within memory. The decoded
     * bitmap is made mutable/software-backed-compatible by Compose's
     * asImageBitmap, so no extra copy is needed here.
     */
    private fun decodeWithImageDecoder(bytes: ByteArray): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        val source = ImageDecoder.createSource(java.nio.ByteBuffer.wrap(bytes))
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val sample = computeInSampleSize(info.size.width, info.size.height, TARGET_MAX_DIMENSION)
            if (sample > 1) {
                decoder.setTargetSize(info.size.width / sample, info.size.height / sample)
            }
        }
    }
}

/**
 * Pure decoder-path selector: HEIF/HEIC must go through [ImageDecoder] (API
 * 28+) because [BitmapFactory] does not reliably decode them across the
 * supported API range, while every other advertised raster format uses the
 * [BitmapFactory] bounds-first path. Framework-free so it is unit-testable.
 */
internal fun needsImageDecoder(mimeType: String?, extension: String?): Boolean {
    val mime = mimeType?.trim()?.lowercase()
    if (mime == "image/heif" || mime == "image/heic") return true
    val ext = extension?.trim()?.lowercase()?.removePrefix(".")
    return ext == "heif" || ext == "heic"
}

/**
 * Returns a power-of-two sample size (the standard Android downsampling value)
 * that brings both decoded dimensions close to [targetMax] without dropping
 * below it: it is the largest power of two such that halving again would put a
 * dimension under [targetMax]. Returns 1 (no downsampling) when the bounds pass
 * could not read valid dimensions or the image already fits within [targetMax].
 */
internal fun computeInSampleSize(width: Int, height: Int, targetMax: Int): Int {
    if (width <= 0 || height <= 0 || targetMax <= 0) return 1
    var sampleSize = 1
    while (width / (sampleSize * 2) >= targetMax || height / (sampleSize * 2) >= targetMax) {
        sampleSize *= 2
    }
    return sampleSize
}
