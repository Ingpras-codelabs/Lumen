package com.offlineviewer.app.render

/**
 * Holds every registered [FileRenderer] and resolves the best one for a given
 * (mimeType, extension) pair. A [fallback] renderer (plain text by default) is
 * always returned when nothing else matches, guaranteeing that EVERY file gets
 * rendered rather than failing.
 *
 * The registry is framework-free and its resolution is pure, so it is fully
 * unit-testable on the JVM.
 */
class RendererRegistry(
    private val renderers: List<FileRenderer>,
    val fallback: FileRenderer,
) {

    /**
     * Returns the first registered renderer that reports it [can render]
     * [FileRenderer.canRender] the given [mimeType] / [extension], preferring a
     * MIME-type match over an extension-only match. Falls back to [fallback]
     * when none match. Never returns null.
     */
    fun resolve(mimeType: String?, extension: String?): FileRenderer {
        val mime = mimeType?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val ext = extension?.trim()?.lowercase()?.removePrefix(".")?.takeIf { it.isNotEmpty() }

        // Prefer a renderer whose MIME set contains the exact MIME type.
        if (mime != null) {
            renderers.firstOrNull { it.supportedMimeTypes.contains(mime) }?.let { return it }
        }
        // Then a renderer whose extension set contains the extension.
        if (ext != null) {
            renderers.firstOrNull { it.supportedExtensions.contains(ext) }?.let { return it }
        }
        // Finally any renderer that reports it can render (covers wildcard-style logic).
        renderers.firstOrNull { it.canRender(mime, ext) }?.let { return it }

        return fallback
    }

    /** Convenience overload resolving against a [LoadedFile]. */
    fun resolve(file: LoadedFile): FileRenderer = resolve(file.mimeType, file.extension)

    companion object {
        /**
         * Builds the default registry with all FEAT-002 renderers registered.
         * Order matters only for overlapping claims; the plain-text fallback is
         * last-resort and never shadows a format-specific renderer.
         */
        fun createDefault(): RendererRegistry {
            val plainText = PlainTextRenderer()
            return RendererRegistry(
                renderers = listOf(
                    MarkdownRenderer(),
                    SvgRenderer(),
                    DelimitedTextRenderer(),
                    StructuredDataRenderer(),
                    HtmlRenderer(),
                    SourceCodeRenderer(),
                    plainText,
                ),
                fallback = plainText,
            )
        }
    }
}
