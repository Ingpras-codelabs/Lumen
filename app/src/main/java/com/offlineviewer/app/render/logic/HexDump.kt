package com.offlineviewer.app.render.logic

/**
 * A pure, dependency-free formatter that renders raw bytes as a classic hex
 * dump, e.g.:
 *
 * ```
 * 00000000  48 65 6c 6c 6f 20 77 6f  72 6c 64 21 0a 00 ff 7f  |Hello world!....|
 * ```
 *
 * Each line has an 8-digit hex offset, up to [BYTES_PER_ROW] two-digit hex byte
 * values (split into two groups for readability), and an ASCII gutter where
 * printable bytes (0x20-0x7E) show their character and everything else shows a
 * '.'.
 *
 * Only the first [MAX_BYTES] bytes are formatted; when the input is larger a
 * truncation notice line is appended, mirroring the row-capping discipline used
 * by DelimitedTextRenderer so huge binaries never exhaust memory.
 *
 * This object has no Android/Compose dependency so it is exercised directly by
 * JVM unit tests.
 */
object HexDump {

    /** Bytes rendered per output row. */
    const val BYTES_PER_ROW = 16

    /** Size of each hex group within a row (two groups of 8 bytes). */
    const val GROUP_SIZE = 8

    /** Maximum number of bytes formatted before truncation kicks in. */
    const val MAX_BYTES = 64 * 1024

    private const val PRINTABLE_MIN = 0x20
    private const val PRINTABLE_MAX = 0x7E

    /**
     * Formats [bytes] as a hex dump string. Returns an empty string for empty
     * input. At most [cap] bytes are rendered; a truncation notice is appended
     * when the input is longer.
     */
    fun format(bytes: ByteArray, cap: Int = MAX_BYTES): String {
        if (bytes.isEmpty()) return ""

        val limit = minOf(bytes.size, cap)
        val sb = StringBuilder()

        var offset = 0
        while (offset < limit) {
            appendLine(sb, bytes, offset, minOf(offset + BYTES_PER_ROW, limit))
            offset += BYTES_PER_ROW
            if (offset < limit) sb.append('\n')
        }

        if (bytes.size > cap) {
            sb.append('\n')
            sb.append("... truncated: showing the first ")
            sb.append(cap)
            sb.append(" of ")
            sb.append(bytes.size)
            sb.append(" bytes.")
        }

        return sb.toString()
    }

    /** Appends one hex-dump line covering bytes in [start, end). */
    private fun appendLine(sb: StringBuilder, bytes: ByteArray, start: Int, end: Int) {
        // 8-digit hex offset.
        appendHexOffset(sb, start)
        sb.append("  ")

        // Hex byte columns, padded so the ASCII gutter always aligns.
        for (col in 0 until BYTES_PER_ROW) {
            val index = start + col
            if (index < end) {
                appendHexByte(sb, bytes[index])
            } else {
                sb.append("  ")
            }
            // Space after every byte, plus an extra space between the two groups.
            sb.append(' ')
            if (col == GROUP_SIZE - 1) sb.append(' ')
        }

        // ASCII gutter.
        sb.append('|')
        for (index in start until end) {
            val value = bytes[index].toInt() and 0xFF
            sb.append(if (value in PRINTABLE_MIN..PRINTABLE_MAX) value.toChar() else '.')
        }
        sb.append('|')
    }

    private fun appendHexOffset(sb: StringBuilder, offset: Int) {
        val hex = Integer.toHexString(offset)
        for (i in hex.length until 8) sb.append('0')
        sb.append(hex)
    }

    private fun appendHexByte(sb: StringBuilder, byte: Byte) {
        val value = byte.toInt() and 0xFF
        sb.append(HEX_DIGITS[value ushr 4])
        sb.append(HEX_DIGITS[value and 0x0F])
    }

    private val HEX_DIGITS = "0123456789abcdef".toCharArray()
}
