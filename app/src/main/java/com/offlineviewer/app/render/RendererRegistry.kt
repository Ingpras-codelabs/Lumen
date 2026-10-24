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
         * Builds the default registry with every FEAT-003 renderer registered.
         *
         * Resolution is deterministic (see [resolve]): a renderer earlier in
         * this list wins only when two renderers claim the same MIME type or
         * extension. The curated format sets are disjoint except that SVG is an
         * image type, so [SvgRenderer] is listed BEFORE [ImageRenderer] to
         * guarantee image/svg+xml and .svg route to the vector renderer rather
         * than the raster one. The plain-text renderer is both the explicit
         * handler for text/plain/.txt/.log and the universal last-resort
         * fallback so EVERY file renders something.
         */
        fun createDefault(): RendererRegistry {
            val plainText = PlainTextRenderer()
            return RendererRegistry(
                renderers = listOf(
                    SvgRenderer(),            // must precede ImageRenderer (svg is image/*)
                    MarkdownRenderer(),
                    DelimitedTextRenderer(),  // CSV / TSV
                    StructuredDataRenderer(), // JSON / XML / YAML
                    HtmlRenderer(),
                    RtfRenderer(),
                    EpubRenderer(),
                    DocxRenderer(),           // Word OOXML (content extraction)
                    XlsxRenderer(),           // Excel OOXML (content extraction)
                    PptxRenderer(),           // PowerPoint OOXML (content extraction)
                    ArchiveRenderer(),        // zip/jar/tar/gz/tgz (entry listing)
                    SourceCodeRenderer(),
                    ImageRenderer(),          // webp/bmp/gif/ico/png/jpeg
                    plainText,                // text/plain + explicit text extensions
                ),
                fallback = plainText,         // universal last resort
            )
        }
    }
}
