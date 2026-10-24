package com.offlineviewer.app.render.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveListerTest {

    private fun zip(entries: List<Pair<String, ByteArray?>>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            for ((name, content) in entries) {
                zos.putNextEntry(ZipEntry(name))
                if (content != null) zos.write(content)
                zos.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    private fun gzip(payload: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(payload) }
        return bos.toByteArray()
    }

    /** Builds a minimal ustar tar archive from (name, content) pairs plus the end marker. */
    private fun tar(entries: List<Pair<String, ByteArray>>): ByteArray {
        val bos = ByteArrayOutputStream()
        for ((name, content) in entries) {
            bos.write(tarHeader(name, content.size.toLong(), directory = name.endsWith("/")))
            bos.write(content)
            val pad = (512 - content.size % 512) % 512
            if (pad > 0) bos.write(ByteArray(pad))
        }
        // Two zero blocks mark end-of-archive.
        bos.write(ByteArray(1024))
        return bos.toByteArray()
    }

    private fun tarHeader(name: String, size: Long, directory: Boolean): ByteArray {
        val header = ByteArray(512)
        val nameBytes = name.toByteArray(Charsets.US_ASCII)
        System.arraycopy(nameBytes, 0, header, 0, minOf(nameBytes.size, 100))
        // mode (octal), offset 100, 8 bytes
        writeOctal(header, 100, 8, 644)
        // size (octal), offset 124, 12 bytes
        writeOctal(header, 124, 12, size)
        // typeflag, offset 156
        header[156] = if (directory) '5'.code.toByte() else '0'.code.toByte()
        // ustar magic at offset 257
        val magic = "ustar\u0000".toByteArray(Charsets.US_ASCII)
        System.arraycopy(magic, 0, header, 257, magic.size)
        // version "00" at 263
        header[263] = '0'.code.toByte()
        header[264] = '0'.code.toByte()
        // checksum: fill with spaces, sum, then write octal.
        for (i in 148 until 156) header[i] = ' '.code.toByte()
        var checksum = 0
        for (b in header) checksum += (b.toInt() and 0xFF)
        writeOctal(header, 148, 7, checksum.toLong())
        header[155] = ' '.code.toByte()
        return header
    }

    private fun writeOctal(buf: ByteArray, offset: Int, len: Int, value: Long) {
        // Octal string, right-justified, NUL-terminated within the field.
        val octal = value.toString(8)
        val field = octal.padStart(len - 1, '0').takeLast(len - 1)
        val bytes = field.toByteArray(Charsets.US_ASCII)
        System.arraycopy(bytes, 0, buf, offset, bytes.size)
        buf[offset + len - 1] = 0
    }

    @Test
    fun listsZipEntriesWithSizesAndOrder() {
        val bytes = zip(
            listOf(
                "readme.txt" to "hello world".toByteArray(),
                "src/" to null,
                "src/Main.kt" to "fun main() {}".toByteArray(),
            ),
        )
        val listing = ArchiveLister.list(bytes, ArchiveLister.Kind.ZIP)

        assertEquals(3, listing.entries.size)
        assertEquals("readme.txt", listing.entries[0].name)
        assertEquals(11L, listing.entries[0].size)
        assertFalse(listing.entries[0].isDirectory)

        assertEquals("src/", listing.entries[1].name)
        assertTrue(listing.entries[1].isDirectory)

        assertEquals("src/Main.kt", listing.entries[2].name)
        assertEquals("fun main() {}".length.toLong(), listing.entries[2].size)

        assertEquals(2, listing.fileCount)
        assertEquals(1, listing.directoryCount)
        assertEquals(11L + "fun main() {}".length, listing.totalUncompressedSize)
        assertFalse(listing.truncated)
        assertFalse(listing.truncatedByError)
    }

    @Test
    fun listsTarEntriesWithSizesAndOrder() {
        val bytes = tar(
            listOf(
                "a.txt" to "AAAA".toByteArray(),
                "dir/" to ByteArray(0),
                "dir/b.txt" to "BBBBBBBB".toByteArray(),
            ),
        )
        val listing = ArchiveLister.list(bytes, ArchiveLister.Kind.TAR)

        assertEquals(3, listing.entries.size)
        assertEquals("a.txt", listing.entries[0].name)
        assertEquals(4L, listing.entries[0].size)
        assertEquals("dir/", listing.entries[1].name)
        assertTrue(listing.entries[1].isDirectory)
        assertEquals("dir/b.txt", listing.entries[2].name)
        assertEquals(8L, listing.entries[2].size)
    }

    @Test
    fun listsGzippedTarAsTarMembers() {
        val tarBytes = tar(listOf("one.txt" to "12345".toByteArray()))
        val listing = ArchiveLister.list(gzip(tarBytes), ArchiveLister.Kind.GZIP)

        assertEquals(1, listing.entries.size)
        assertEquals("one.txt", listing.entries[0].name)
        assertEquals(5L, listing.entries[0].size)
    }

    @Test
    fun listsPlainGzipAsSingleDecompressedMember() {
        val payload = "this is a plain gzipped text payload".toByteArray()
        val listing = ArchiveLister.list(gzip(payload), ArchiveLister.Kind.GZIP)

        assertEquals(1, listing.entries.size)
        assertFalse(listing.entries[0].isDirectory)
        assertEquals(payload.size.toLong(), listing.entries[0].size)
    }

    @Test
    fun capsListingAtMaxEntriesWithTruncationFlag() {
        val many = (0 until ArchiveLister.MAX_ENTRIES + 25).map { "f$it.txt" to "x".toByteArray() }
        val listing = ArchiveLister.list(zip(many), ArchiveLister.Kind.ZIP)

        assertEquals(ArchiveLister.MAX_ENTRIES, listing.entries.size)
        assertTrue(listing.truncated)
    }

    @Test
    fun malformedZipYieldsEmptyListingNeverThrows() {
        val listing = ArchiveLister.list(byteArrayOf(1, 2, 3, 4, 5), ArchiveLister.Kind.ZIP)
        assertTrue(listing.isEmpty)
    }

    @Test
    fun malformedGzipYieldsEmptyListingWithErrorFlag() {
        val listing = ArchiveLister.list(byteArrayOf(0x1f.toByte(), 0x8b.toByte(), 9, 9, 9), ArchiveLister.Kind.GZIP)
        assertTrue(listing.isEmpty)
        assertTrue(listing.truncatedByError)
    }

    @Test
    fun nullOrEmptyBytesYieldEmptyListing() {
        assertTrue(ArchiveLister.list(null, ArchiveLister.Kind.ZIP).isEmpty)
        assertTrue(ArchiveLister.list(ByteArray(0), ArchiveLister.Kind.TAR).isEmpty)
    }

    @Test
    fun kindForChoosesContainerFamilyFromHints() {
        assertEquals(ArchiveLister.Kind.TAR, ArchiveLister.kindFor("tar", null))
        assertEquals(ArchiveLister.Kind.TAR, ArchiveLister.kindFor(null, "application/x-tar"))
        assertEquals(ArchiveLister.Kind.GZIP, ArchiveLister.kindFor("gz", null))
        assertEquals(ArchiveLister.Kind.GZIP, ArchiveLister.kindFor("tgz", null))
        assertEquals(ArchiveLister.Kind.GZIP, ArchiveLister.kindFor(null, "application/gzip"))
        assertEquals(ArchiveLister.Kind.ZIP, ArchiveLister.kindFor("zip", null))
        assertEquals(ArchiveLister.Kind.ZIP, ArchiveLister.kindFor("jar", null))
        assertEquals(ArchiveLister.Kind.ZIP, ArchiveLister.kindFor(null, null))
    }

    @Test
    fun humanReadableSizeFormatsBinaryUnits() {
        assertEquals("0 B", ArchiveLister.humanReadableSize(0))
        assertEquals("512 B", ArchiveLister.humanReadableSize(512))
        assertEquals("1 KB", ArchiveLister.humanReadableSize(1024))
        assertEquals("1.5 KB", ArchiveLister.humanReadableSize(1536))
        assertEquals("1 MB", ArchiveLister.humanReadableSize(1024L * 1024))
        assertEquals("-", ArchiveLister.humanReadableSize(-1))
    }
}
