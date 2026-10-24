package com.offlineviewer.app.render.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RtfConverterTest {

    @Test
    fun detectsRtfHeader() {
        assertTrue(RtfConverter.looksLikeRtf("{\\rtf1\\ansi hello}"))
        assertFalse(RtfConverter.looksLikeRtf("plain text"))
    }

    @Test
    fun nonRtfYieldsSinglePlainRun() {
        val runs = RtfConverter.convert("just text")
        assertEquals(1, runs.size)
        assertEquals("just text", runs[0].text)
        assertFalse(runs[0].bold)
    }

    @Test
    fun convertsBoldAndItalic() {
        val runs = RtfConverter.convert("{\\rtf1 normal \\b bold\\b0  \\i italic\\i0 }")
        val plain = RtfConverter.toPlainText(runs)
        assertTrue(plain.contains("normal"))
        assertTrue(plain.contains("bold"))
        assertTrue(plain.contains("italic"))
        val boldRun = runs.firstOrNull { it.text.contains("bold") }
        assertTrue(boldRun != null && boldRun.bold)
        val italicRun = runs.firstOrNull { it.text.contains("italic") }
        assertTrue(italicRun != null && italicRun.italic)
    }

    @Test
    fun parBecomesNewline() {
        val runs = RtfConverter.convert("{\\rtf1 line1\\par line2}")
        val plain = RtfConverter.toPlainText(runs)
        assertTrue(plain.contains("line1"))
        assertTrue(plain.contains("line2"))
        assertTrue(plain.contains("\n"))
    }

    @Test
    fun unicodeEscapeConverted() {
        val runs = RtfConverter.convert("{\\rtf1 \\u233 e}")
        val plain = RtfConverter.toPlainText(runs)
        assertTrue(plain.contains("\u00e9"))
    }

    @Test
    fun hexEscapeConverted() {
        val runs = RtfConverter.convert("{\\rtf1 caf\\'e9}")
        val plain = RtfConverter.toPlainText(runs)
        assertTrue(plain.contains("\u00e9"))
    }

    @Test
    fun unknownControlWordsIgnoredGracefully() {
        val runs = RtfConverter.convert("{\\rtf1\\ansi\\deff0\\b0 hello \\xyzzy123 world}")
        val plain = RtfConverter.toPlainText(runs)
        assertTrue(plain.contains("hello"))
        assertTrue(plain.contains("world"))
        // Unknown control words themselves must not leak into the text.
        assertFalse(plain.contains("xyzzy"))
        assertFalse(plain.contains("\\"))
    }
}
