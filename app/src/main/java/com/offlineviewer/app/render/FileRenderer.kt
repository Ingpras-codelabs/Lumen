package com.offlineviewer.app.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A pluggable renderer for a family of file formats.
 *
 * Each renderer advertises the set of MIME types and file extensions it can
 * handle. The [RendererRegistry] uses [canRender] to pick the best match for a
 * loaded file, and the UI calls [Render] to draw the content.
 *
 * Implementations should be stateless and cheap to construct so the registry
 * can hold a single instance of each. Matching logic lives here (and is pure)
 * so it can be unit-tested without the Android framework; the [Render] entry
 * point is the only Compose/UI-bound part.
 */
interface FileRenderer {

    /** Short human-readable name of the format, e.g. "Markdown". */
    val formatName: String

    /** Lower-cased MIME types this renderer handles, e.g. "text/markdown". */
    val supportedMimeTypes: Set<String>

    /** Lower-cased extensions (without dot) this renderer handles, e.g. "md". */
    val supportedExtensions: Set<String>

    /**
     * Returns true when this renderer can handle a file with the given
     * [mimeType] and/or [extension]. Both are matched case-insensitively.
     * Either argument may be null/blank; a match on just one is enough.
     */
    fun canRender(mimeType: String?, extension: String?): Boolean {
        val mime = mimeType?.trim()?.lowercase()
        if (!mime.isNullOrEmpty() && supportedMimeTypes.contains(mime)) return true
        val ext = extension?.trim()?.lowercase()?.removePrefix(".")
        if (!ext.isNullOrEmpty() && supportedExtensions.contains(ext)) return true
        return false
    }

    /** Draws the given [file] into the composition. */
    @Composable
    fun Render(file: LoadedFile, modifier: Modifier)
}
