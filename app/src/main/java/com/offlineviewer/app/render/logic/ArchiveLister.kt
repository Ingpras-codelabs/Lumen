package com.offlineviewer.app.render.logic

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

/**
 * A dependency-free archive *lister* built on [java.util.zip]. It reads the
 * table of contents of an archive (entry names, uncompressed sizes, whether
 * each entry is a directory) without extracting file contents to disk.
 *
 * Three container families are understood:
 *  - zip / jar via [ZipInputStream];
 *  - gzip (.gz / .tgz) via [GZIPInputStream] (a gzip stream wraps exactly one
 *    payload; a .tgz is a gzip-wrapped tar, so the decompressed bytes are
 *    re-read as tar);
 *  - tar (POSIX/ustar) via a minimal 512-byte header reader.
 *
 * All parsing is defensive: a malformed or truncated archive yields a partial
 * (possibly empty) listing and the [Listing.truncatedByError] flag rather than
 * throwing, so the renderer can degrade gracefully. The number of listed
 * entries is capped at [MAX_ENTRIES]; when more exist, [Listing.truncated] is
 * set so the UI can show a notice.
 */
object ArchiveLister {

    /** Upper bound on listed entries so a hostile/huge archive cannot exhaust memory or the UI. */
    const val MAX_ENTRIES = 5000

    /** The container family, chosen from an extension/MIME hint. */
    enum class Kind { ZIP, TAR, GZIP }

    /** A single archive entry. [size] is the uncompressed size in bytes, or -1 when unknown. */
    data class Entry(
        val name: String,
        val size: Long,
        val isDirectory: Boolean,
    )

    /**
     * The result of listing an archive.
     *
     * @param entries ordered entries as encountered in the archive (capped at [MAX_ENTRIES]).
     * @param truncated true when more than [MAX_ENTRIES] entries existed and the list was capped.
     * @param truncatedByError true when reading stopped early because the archive was malformed.
     */
    data class Listing(
        val entries: List<Entry>,
        val truncated: Boolean,
        val truncatedByError: Boolean,
    ) {
        val fileCount: Int get() = entries.count { !it.isDirectory }
        val directoryCount: Int get() = entries.count { it.isDirectory }

        /** Sum of known (>= 0) uncompressed file sizes. */
        val totalUncompressedSize: Long get() = entries.filter { !it.isDirectory && it.size >= 0 }.sumOf { it.size }

        val isEmpty: Boolean get() = entries.isEmpty()
    }

    /**
     * Formats a byte count as a short human-readable string using binary units
     * (1024-based). Negative sizes (unknown) render as "-". Pure so it can be
     * unit-tested on the JVM.
     */
    fun humanReadableSize(bytes: Long): String {
        if (bytes < 0) return "-"
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB", "PB")
        var value = bytes.toDouble() / 1024.0
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.size - 1) {
            value /= 1024.0
            unitIndex++
        }
        val rounded = (value * 10).toLong() / 10.0
        return if (rounded == rounded.toLong().toDouble()) {
            "${rounded.toLong()} ${units[unitIndex]}"
        } else {
            "$rounded ${units[unitIndex]}"
        }
    }

    /** Picks the container family from a file extension and/or MIME type hint. */
    fun kindFor(extension: String?, mimeType: String?): Kind {
        val ext = extension?.trim()?.lowercase()?.removePrefix(".").orEmpty()
        val mime = mimeType?.trim()?.lowercase().orEmpty()
        return when {
            ext == "tar" || mime == "application/x-tar" -> Kind.TAR
            ext == "gz" || ext == "tgz" || mime == "application/gzip" || mime == "application/x-gzip" -> Kind.GZIP
            else -> Kind.ZIP // zip, jar, and anything else default to the zip reader
        }
    }

    /** Lists an archive of the given [kind] from its raw [bytes]. */
    fun list(bytes: ByteArray?, kind: Kind): Listing {
        if (bytes == null || bytes.isEmpty()) {
            return Listing(emptyList(), truncated = false, truncatedByError = false)
        }
        return when (kind) {
            Kind.ZIP -> listZip(bytes)
            Kind.TAR -> listTar(ByteArrayInputStream(bytes))
            Kind.GZIP -> listGzip(bytes)
        }
    }

    private fun listZip(bytes: ByteArray): Listing {
        val entries = ArrayList<Entry>()
        var truncated = false
        var error = false
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entries.size >= MAX_ENTRIES) {
                        truncated = true
                        break
                    }
                    // ZipEntry.size is -1 until the entry is read for some streams;
                    // drain the entry so the size is populated when available.
                    var declared = entry.size
                    if (declared < 0) {
                        var skipped = 0L
                        val buf = ByteArray(8192)
                        var read = zis.read(buf)
                        while (read >= 0) {
                            skipped += read
                            read = zis.read(buf)
                        }
                        declared = if (entry.size >= 0) entry.size else skipped
                    }
                    entries.add(Entry(entry.name, if (entry.isDirectory) -1L else declared, entry.isDirectory))
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Throwable) {
            error = true
        }
        return Listing(entries, truncated, error)
    }

    private fun listGzip(bytes: ByteArray): Listing {
        // A gzip stream decompresses to a single payload. When that payload is a
        // tar archive (the common .tgz case) list its members; otherwise report
        // the single decompressed member with its size.
        return try {
            val decompressed = GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
            if (looksLikeTar(decompressed)) {
                listTar(ByteArrayInputStream(decompressed))
            } else {
                Listing(
                    entries = listOf(Entry("(decompressed contents)", decompressed.size.toLong(), isDirectory = false)),
                    truncated = false,
                    truncatedByError = false,
                )
            }
        } catch (e: Throwable) {
            Listing(emptyList(), truncated = false, truncatedByError = true)
        }
    }

    /** Reads just enough of [data] to tell whether it is a ustar/POSIX tar archive. */
    private fun looksLikeTar(data: ByteArray): Boolean {
        if (data.size < TAR_BLOCK) return false
        // The ustar magic lives at offset 257 ("ustar"). GNU tar also uses "ustar  ".
        val magic = String(data, 257, 5, Charsets.US_ASCII)
        return magic == "ustar"
    }

    /**
     * Minimal POSIX/ustar tar reader. Tar is a sequence of 512-byte blocks: a
     * header block (name, octal size, type flag) followed by the file content
     * rounded up to a whole number of blocks. Two consecutive zero blocks mark
     * the end. This reads headers only, skipping content, so it never buffers a
     * whole member in memory.
     */
    private fun listTar(input: InputStream): Listing {
        val entries = ArrayList<Entry>()
        var truncated = false
        var error = false
        try {
            input.use { stream ->
                val header = ByteArray(TAR_BLOCK)
                while (true) {
                    if (!readFully(stream, header)) break
                    if (header.all { it == 0.toByte() }) break // end-of-archive marker

                    val name = cString(header, 0, 100)
                    if (name.isEmpty()) break

                    val size = parseOctal(header, 124, 12)
                    val typeFlag = header[156]
                    // '5' = directory; trailing '/' is also treated as a directory.
                    val isDir = typeFlag == '5'.code.toByte() || name.endsWith("/")

                    if (entries.size >= MAX_ENTRIES) {
                        truncated = true
                        break
                    }
                    entries.add(Entry(name, if (isDir) -1L else size, isDir))

                    // Skip the content blocks (content is padded to a 512-byte boundary).
                    val toSkip = roundUpToBlock(size)
                    if (!skipFully(stream, toSkip)) break
                }
            }
        } catch (e: Throwable) {
            error = true
        }
        return Listing(entries, truncated, error)
    }

    private const val TAR_BLOCK = 512

    private fun roundUpToBlock(size: Long): Long {
        if (size <= 0) return 0
        val rem = size % TAR_BLOCK
        return if (rem == 0L) size else size + (TAR_BLOCK - rem)
    }

    /** Reads a NUL-terminated ASCII string from [buf] within [offset, offset+len). */
    private fun cString(buf: ByteArray, offset: Int, len: Int): String {
        var end = offset
        val limit = offset + len
        while (end < limit && buf[end] != 0.toByte()) end++
        return String(buf, offset, end - offset, Charsets.US_ASCII).trim()
    }

    /** Parses a space/NUL-padded octal number (the tar size/mode encoding). */
    private fun parseOctal(buf: ByteArray, offset: Int, len: Int): Long {
        val s = cString(buf, offset, len).trim().trim('\u0000').trim()
        if (s.isEmpty()) return 0L
        return try {
            s.toLong(radix = 8)
        } catch (e: NumberFormatException) {
            0L
        }
    }

    /** Reads exactly [buf].size bytes; returns false if the stream ended first. */
    private fun readFully(stream: InputStream, buf: ByteArray): Boolean {
        var off = 0
        while (off < buf.size) {
            val read = stream.read(buf, off, buf.size - off)
            if (read < 0) return false
            off += read
        }
        return true
    }

    /** Skips exactly [count] bytes; returns false if the stream ended first. */
    private fun skipFully(stream: InputStream, count: Long): Boolean {
        var remaining = count
        val scratch = ByteArray(8192)
        while (remaining > 0) {
            val want = minOf(remaining, scratch.size.toLong()).toInt()
            val read = stream.read(scratch, 0, want)
            if (read < 0) return false
            remaining -= read
        }
        return true
    }
}
