package com.offlineviewer.app.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for [RendererRegistry] resolution: by MIME type, by extension,
 * and the plain-text fallback for unknown types. Uses lightweight fake
 * renderers so no Compose runtime is needed.
 */
class RendererRegistryTest {

    private class FakeRenderer(
        override val formatName: String,
        override val supportedMimeTypes: Set<String>,
        override val supportedExtensions: Set<String>,
    ) : FileRenderer {
        @Composable
        override fun Render(file: LoadedFile, modifier: Modifier) {
            // no-op for tests
        }
    }

    private val markdown = FakeRenderer("Markdown", setOf("text/markdown"), setOf("md", "markdown"))
    private val svg = FakeRenderer("SVG", setOf("image/svg+xml"), setOf("svg"))
    private val fallback = FakeRenderer("Plain text", setOf("text/plain"), setOf("txt"))

    private val registry = RendererRegistry(
        renderers = listOf(markdown, svg, fallback),
        fallback = fallback,
    )

    @Test
    fun resolvesByMimeType() {
        assertSame(markdown, registry.resolve("text/markdown", null))
        assertSame(svg, registry.resolve("image/svg+xml", null))
    }

    @Test
    fun resolvesByExtensionWhenMimeMissing() {
        assertSame(markdown, registry.resolve(null, "md"))
        assertSame(markdown, registry.resolve(null, "markdown"))
        assertSame(svg, registry.resolve(null, "svg"))
    }

    @Test
    fun resolvesByExtensionIgnoringLeadingDotAndCase() {
        assertSame(svg, registry.resolve(null, ".SVG"))
        assertSame(markdown, registry.resolve("TEXT/MARKDOWN", null))
    }

    @Test
    fun mimeTypeTakesPrecedenceOverExtension() {
        // MIME says markdown, extension says svg -> MIME wins.
        assertSame(markdown, registry.resolve("text/markdown", "svg"))
    }

    @Test
    fun fallsBackToPlainTextForUnknownTypes() {
        assertSame(fallback, registry.resolve("application/unknown", "xyz"))
        assertSame(fallback, registry.resolve(null, null))
        assertSame(fallback, registry.resolve("", ""))
    }

    @Test
    fun resolvesFromLoadedFile() {
        val file = LoadedFile(
            displayName = "doc.md",
            mimeType = "text/markdown",
            extension = "md",
            text = "# hi",
            bytes = null,
            sizeBytes = 4,
        )
        assertSame(markdown, registry.resolve(file))
    }

    @Test
    fun defaultRegistryWiresFormatRenderersAndFallback() {
        val reg = RendererRegistry.createDefault()
        assertEquals("Markdown", reg.resolve("text/markdown", null).formatName)
        assertEquals("SVG", reg.resolve("image/svg+xml", null).formatName)
        assertEquals("Table", reg.resolve(null, "csv").formatName)
        assertEquals("Structured data", reg.resolve("application/json", null).formatName)
        assertEquals("HTML", reg.resolve(null, "html").formatName)
        assertEquals("Source code", reg.resolve(null, "kt").formatName)
        // Unknown type still resolves to the plain-text fallback.
        assertEquals("Plain text", reg.resolve("application/weird", "weird").formatName)
        assertTrue(reg.fallback is PlainTextRenderer)
    }

    /**
     * Locks the documented invariant (see [RendererRegistry.createDefault]):
     * because SVG is an image type, [SvgRenderer] MUST resolve ahead of the
     * raster [ImageRenderer] for both the `image/svg+xml` MIME type AND the
     * `.svg` extension. The extension case is load-bearing on list order, so a
     * future reorder that put ImageRenderer first would silently route SVGs to
     * the raster decoder; this test fails loudly if that happens.
     */
    @Test
    fun svgResolvesAheadOfImageRendererForSvgMimeAndExtension() {
        val reg = RendererRegistry.createDefault()

        val byMime = reg.resolve("image/svg+xml", null)
        assertTrue(
            "image/svg+xml must resolve to SvgRenderer, not ImageRenderer (got ${byMime.formatName})",
            byMime is SvgRenderer,
        )

        val byExtension = reg.resolve(null, "svg")
        assertTrue(
            ".svg must resolve to SvgRenderer, not ImageRenderer (got ${byExtension.formatName})",
            byExtension is SvgRenderer,
        )

        // Guard against a renderer that claims both: ensure it is NOT the raster one.
        assertTrue("image/svg+xml must not route to ImageRenderer", byMime !is ImageRenderer)
        assertTrue(".svg must not route to ImageRenderer", byExtension !is ImageRenderer)
    }
}
