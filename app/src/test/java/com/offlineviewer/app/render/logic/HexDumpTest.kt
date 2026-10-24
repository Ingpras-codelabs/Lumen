package com.offlineviewer.app.render.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HexDumpTest {

    @Test
    fun emptyInputYieldsEmptyString() {
        assertEquals("", HexDump.format(ByteArray(0)))
    }

    @Test
    fun subRowInputFormatsOffsetHexAndAsciiGutter() {
        // "Hello" = 48 65 6c 6c 6f, only 5 of 16 columns filled.
        val out = HexDump.format("Hello".toByteArray(Charsets.US_ASCII))
        val expected =
            "00000000  48 65 6c 6c 6f                                   |Hello|"
        assertEquals(expected, out)
    }

    @Test
    fun nonPrintableBytesRenderAsDotInGutter() {
        // 0x00, 'A' (0x41), 0x1f (below printable), 0x7f (above printable), 0x7e '~'.
        val bytes = byteArrayOf(0x00, 0x41, 0x1f, 0x7f, 0x7e)
        val out = HexDump.format(bytes)
        val expected =
            "00000000  00 41 1f 7f 7e                                   |.A..~|"
        assertEquals(expected, out)
    }

    @Test
    fun fullSingleRowHasBothHexGroupsAndFullGutter() {
        val bytes = ByteArray(16) { it.toByte() } // 0x00 .. 0x0f
        val out = HexDump.format(bytes)
        val expected =
            "00000000  00 01 02 03 04 05 06 07  08 09 0a 0b 0c 0d 0e 0f |................|"
        assertEquals(expected, out)
    }

    @Test
    fun multiRowInputAdvancesOffsetPerRow() {
        // 18 bytes: first 16 on row 0 (offset 0), remaining 2 on row 1 (offset 16).
        val bytes = ByteArray(18) { (0x41 + it).toByte() } // 'A' onward
        val out = HexDump.format(bytes)
        val lines = out.split("\n")
        assertEquals(2, lines.size)
        assertEquals(
            "00000000  41 42 43 44 45 46 47 48  49 4a 4b 4c 4d 4e 4f 50 |ABCDEFGHIJKLMNOP|",
            lines[0],
        )
        assertEquals(
            "00000010  51 52                                            |QR|",
            lines[1],
        )
    }

    @Test
    fun overCapInputIsTruncatedWithNotice() {
        val bytes = ByteArray(40) { 0x61 } // 40 'a' bytes
        val out = HexDump.format(bytes, cap = 32)
        val lines = out.split("\n")
        // 32 bytes -> exactly two full rows, then a truncation notice line.
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("00000000  61 61"))
        assertTrue(lines[1].startsWith("00000010  61 61"))
        assertEquals("... truncated: showing the first 32 of 40 bytes.", lines[2])
    }

    @Test
    fun inputExactlyAtCapHasNoTruncationNotice() {
        val bytes = ByteArray(16) { 0x62 }
        val out = HexDump.format(bytes, cap = 16)
        assertTrue("should not append a truncation notice", !out.contains("truncated"))
        assertEquals(1, out.split("\n").size)
    }
}
