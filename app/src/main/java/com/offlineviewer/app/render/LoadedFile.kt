package com.offlineviewer.app.render

/**
 * An in-memory representation of a file that has been loaded from a content Uri.
 *
 * Exactly one of [text] / [bytes] carries the payload depending on whether the
 * file was decoded as text or kept as raw bytes. This type is deliberately free
 * of any Android framework dependency so renderers and tests can use it on the JVM.
 */
data class LoadedFile(
    /** Human-readable display name, e.g. "report.md". */
    val displayName: String,
    /** Resolved MIME type, e.g. "text/markdown". May be a best-effort fallback. */
    val mimeType: String?,
    /** Lower-cased file extension without the dot, e.g. "md". Empty when unknown. */
    val extension: String,
    /** Decoded text content when the file was read as text, otherwise null. */
    val text: String?,
    /** Raw bytes when the file was read as binary, otherwise null. */
    val bytes: ByteArray?,
    /** Total size in bytes as reported by the content provider (or payload size). */
    val sizeBytes: Long,
) {
    /** True when this file was loaded as decoded text. */
    val isText: Boolean get() = text != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LoadedFile) return false
        return displayName == other.displayName &&
            mimeType == other.mimeType &&
            extension == other.extension &&
            text == other.text &&
            (bytes?.contentEquals(other.bytes) ?: (other.bytes == null)) &&
            sizeBytes == other.sizeBytes
    }

    override fun hashCode(): Int {
        var result = displayName.hashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        result = 31 * result + extension.hashCode()
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        result = 31 * result + sizeBytes.hashCode()
        return result
    }
}
