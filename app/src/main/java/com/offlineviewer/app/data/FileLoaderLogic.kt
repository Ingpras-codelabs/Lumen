package com.offlineviewer.app.data

import com.offlineviewer.app.render.SupportedFormats

/**
 * Pure, framework-free helpers backing [FileLoader]. Kept separate so the
 * MIME/extension resolution, size-guard, text-vs-binary and charset-default
 * logic can be unit-tested on the JVM without a real ContentResolver.
 */
object FileLoaderLogic {

    /** Lower-cased extension (no dot) parsed from a display name; "" when none. */
    fun extensionOf(displayName: String?): String {
        if (displayName.isNullOrBlank()) return ""
        val name = displayName.substringAfterLast('/').substringAfterLast('\\')
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.length - 1) return ""
        return name.substring(dot + 1).lowercase()
    }

    /** A usable display name derived from a Uri string when the provider gives none. */
    fun fallbackDisplayName(uriString: String): String {
        val tail = uriString.substringAfterLast('/').substringBefore('?').substringBefore('#')
        return tail.ifBlank { "document" }
    }

    /**
     * Resolves the effective MIME type. Prefers a concrete type from the
     * provider; otherwise falls back to a type derived from the file extension,
     * finally to null when nothing can be determined. A provider type of the
     * generic "application/octet-stream" is treated as weak and overridden by a
     * known extension mapping when one exists.
     */
    fun resolveMimeType(
        providerMime: String?,
        extension: String,
        mimeFromExtension: (String) -> String?,
    ): String? {
        val provider = providerMime?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val fromExt = extension.takeIf { it.isNotEmpty() }
            ?.let { mimeFromExtension(it) }
            ?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

        if (provider != null && provider != GENERIC_BINARY) return provider
        if (fromExt != null) return fromExt
        return provider // may be the generic type or null
    }

    /** Returns an error message when [sizeBytes] exceeds [maxSizeBytes], else null. */
    fun sizeGuardMessage(sizeBytes: Long, maxSizeBytes: Long): String? {
        if (sizeBytes <= maxSizeBytes) return null
        val maxMb = maxSizeBytes / (1024 * 1024)
        val sizeMb = sizeBytes.toDouble() / (1024 * 1024)
        return "File is too large (%.1f MB). The maximum supported size is %d MB.".format(sizeMb, maxMb)
    }

    /**
     * Guards an already-read byte array against the limit. Throws
     * [IllegalStateException] when over the limit so callers can convert it to a
     * failure result. (The provider-reported size is checked first; this is the
     * belt-and-braces check for providers that under-report.)
     */
    fun readWithLimit(bytes: ByteArray, maxSizeBytes: Long): ByteArray {
        check(bytes.size.toLong() <= maxSizeBytes) { "File exceeds the maximum supported size." }
        return bytes
    }

    /**
     * Decide whether to decode the payload as text. True for text MIME types
     * and for the curated set of text-friendly extensions; false for binary
     * container formats (e.g. epub).
     */
    fun shouldDecodeAsText(mimeType: String?, extension: String): Boolean {
        val mime = mimeType?.trim()?.lowercase()
        if (mime != null) {
            if (mime.startsWith("text/")) return true
            if (mime in TEXTUAL_APPLICATION_MIMES) return true
            if (mime == "image/svg+xml") return true
        }
        return extension.lowercase() in SupportedFormats.textExtensions
    }

    const val GENERIC_BINARY = "application/octet-stream"

    private val TEXTUAL_APPLICATION_MIMES = setOf(
        "application/json",
        "application/xml",
        "application/x-yaml",
        "application/yaml",
        "application/javascript",
        "application/rtf",
    )
}
