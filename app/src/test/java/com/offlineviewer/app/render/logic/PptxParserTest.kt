package com.offlineviewer.app.render.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PptxParserTest {

    private fun zip(entries: Map<String, String>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            for ((name, content) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    private fun slide(vararg paragraphs: String): String {
        val body = paragraphs.joinToString("") { "<a:p><a:r><a:t>$it</a:t></a:r></a:p>" }
        return "<p:sld xmlns:a=\"x\"><p:cSld><p:spTree>$body</p:spTree></p:cSld></p:sld>"
    }

    @Test
    fun extractsSlidesInNumericOrder() {
        // Insert out of numeric order to prove sorting (slide10 must come last).
        val bytes = zip(
            mapOf(
                "ppt/slides/slide10.xml" to slide("Tenth"),
                "ppt/slides/slide2.xml" to slide("Second"),
                "ppt/slides/slide1.xml" to slide("First", "Subtitle"),
            ),
        )

        val deck = PptxParser.parse(bytes)
        assertEquals(3, deck.slides.size)
        assertEquals(listOf("First", "Subtitle"), deck.slides[0].lines)
        assertEquals(listOf("Second"), deck.slides[1].lines)
        assertEquals(listOf("Tenth"), deck.slides[2].lines)
    }

    @Test
    fun decodesEntities() {
        val bytes = zip(mapOf("ppt/slides/slide1.xml" to slide("A &amp; B &lt; C")))
        val deck = PptxParser.parse(bytes)
        assertEquals("A & B < C", deck.slides[0].lines[0])
    }

    @Test
    fun malformedArchiveYieldsEmptyDeck() {
        val deck = PptxParser.parse(byteArrayOf(0, 1, 2))
        assertTrue(deck.slides.isEmpty())
        assertTrue(deck.isEmpty)
    }

    @Test
    fun deckWithoutSlidesIsEmpty() {
        val deck = PptxParser.parse(zip(mapOf("ppt/presentation.xml" to "<x/>")))
        assertTrue(deck.isEmpty)
    }
}
