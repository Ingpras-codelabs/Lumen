package com.offlineviewer.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for [FileLoaderLogic]: MIME/extension resolution, the size
 * guard, text-vs-binary decision, and display-name/extension parsing. No
 * ContentResolver or Android framework is involved.
 */
class FileLoaderLogicTest {

    // --- extensionOf -------------------------------------------------------

    @Test
    fun extensionOf_parsesAndLowercases() {
        assertEquals("md", FileLoaderLogic.extensionOf("README.MD"))
        assertEquals("json", FileLoaderLogic.extensionOf("data.tar.json"))
        assertEquals("svg", FileLoaderLogic.extensionOf("/path/to/image.svg"))
    }

    @Test
    fun extensionOf_handlesNoExtension() {
        assertEquals("", FileLoaderLogic.extensionOf("Makefile"))
        assertEquals("", FileLoaderLogic.extensionOf(".gitignore")) // leading dot only
        assertEquals("", FileLoaderLogic.extensionOf("trailingdot."))
        assertEquals("", FileLoaderLogic.extensionOf(null))
        assertEquals("", FileLoaderLogic.extensionOf("  "))
    }

    // --- fallbackDisplayName ----------------------------------------------

    @Test
    fun fallbackDisplayName_usesTail() {
        assertEquals("file.txt", FileLoaderLogic.fallbackDisplayName("content://auth/path/file.txt"))
        assertEquals("a.json", FileLoaderLogic.fallbackDisplayName("content://x/a.json?q=1#frag"))
        assertEquals("document", FileLoaderLogic.fallbackDisplayName("content://auth/"))
    }

    // --- resolveMimeType ---------------------------------------------------

    @Test
    fun resolveMimeType_prefersConcreteProviderType() {
        val mime = FileLoaderLogic.resolveMimeType(
            providerMime = "text/markdown",
            extension = "md",
            mimeFromExtension = { "text/plain" },
        )
        assertEquals("text/markdown", mime)
    }

    @Test
    fun resolveMimeType_fallsBackToExtensionWhenProviderGeneric() {
        val mime = FileLoaderLogic.resolveMimeType(
            providerMime = FileLoaderLogic.GENERIC_BINARY,
            extension = "json",
            mimeFromExtension = { ext -> if (ext == "json") "application/json" else null },
        )
        assertEquals("application/json", mime)
    }

    @Test
    fun resolveMimeType_fallsBackToExtensionWhenProviderNull() {
        val mime = FileLoaderLogic.resolveMimeType(
            providerMime = null,
            extension = "svg",
            mimeFromExtension = { "image/svg+xml" },
        )
        assertEquals("image/svg+xml", mime)
    }

    @Test
    fun resolveMimeType_returnsGenericWhenNothingElseKnown() {
        val mime = FileLoaderLogic.resolveMimeType(
            providerMime = FileLoaderLogic.GENERIC_BINARY,
            extension = "",
            mimeFromExtension = { null },
        )
        assertEquals(FileLoaderLogic.GENERIC_BINARY, mime)
    }

    @Test
    fun resolveMimeType_returnsNullWhenNothingKnown() {
        val mime = FileLoaderLogic.resolveMimeType(
            providerMime = null,
            extension = "unknownext",
            mimeFromExtension = { null },
        )
        assertNull(mime)
    }

    // --- sizeGuardMessage --------------------------------------------------

    @Test
    fun sizeGuard_allowsWithinLimit() {
        assertNull(FileLoaderLogic.sizeGuardMessage(10, 100))
        assertNull(FileLoaderLogic.sizeGuardMessage(100, 100)) // boundary allowed
    }

    @Test
    fun sizeGuard_rejectsOverLimitWithMessage() {
        val msg = FileLoaderLogic.sizeGuardMessage(101, 100)
        assertTrue(msg != null && msg.contains("too large"))
    }

    @Test
    fun sizeGuard_messageMentionsLimitInMb() {
        val tenMb = 10L * 1024 * 1024
        val msg = FileLoaderLogic.sizeGuardMessage(tenMb + 1, tenMb)
        assertTrue(msg!!.contains("10 MB"))
    }

    // --- readWithLimit -----------------------------------------------------

    @Test
    fun readWithLimit_returnsBytesWithinLimit() {
        val bytes = ByteArray(10)
        assertSameSize(bytes, FileLoaderLogic.readWithLimit(bytes, 10))
    }

    @Test
    fun readWithLimit_throwsWhenOverLimit() {
        assertThrows(IllegalStateException::class.java) {
            FileLoaderLogic.readWithLimit(ByteArray(11), 10)
        }
    }

    // --- shouldDecodeAsText ------------------------------------------------

    @Test
    fun shouldDecodeAsText_trueForTextMimes() {
        assertTrue(FileLoaderLogic.shouldDecodeAsText("text/plain", ""))
        assertTrue(FileLoaderLogic.shouldDecodeAsText("text/markdown", ""))
        assertTrue(FileLoaderLogic.shouldDecodeAsText("application/json", ""))
        assertTrue(FileLoaderLogic.shouldDecodeAsText("application/xml", ""))
        assertTrue(FileLoaderLogic.shouldDecodeAsText("image/svg+xml", ""))
    }

    @Test
    fun shouldDecodeAsText_trueForTextExtensionsWhenMimeUnknown() {
        assertTrue(FileLoaderLogic.shouldDecodeAsText(null, "md"))
        assertTrue(FileLoaderLogic.shouldDecodeAsText(null, "csv"))
        assertTrue(FileLoaderLogic.shouldDecodeAsText(FileLoaderLogic.GENERIC_BINARY, "yaml"))
    }

    @Test
    fun shouldDecodeAsText_falseForBinaryContainers() {
        // epub is a zip container -> read as bytes
        assertFalse(FileLoaderLogic.shouldDecodeAsText("application/epub+zip", "epub"))
        assertFalse(FileLoaderLogic.shouldDecodeAsText(FileLoaderLogic.GENERIC_BINARY, "epub"))
        assertFalse(FileLoaderLogic.shouldDecodeAsText(null, "bin"))
    }

    private fun assertSameSize(expected: ByteArray, actual: ByteArray) {
        assertEquals(expected.size, actual.size)
    }
}
