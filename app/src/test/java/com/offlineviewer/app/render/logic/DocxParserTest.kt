package com.offlineviewer.app.render.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxParserTest {

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

    private fun docx(documentXml: String): ByteArray =
        zip(mapOf("word/document.xml" to documentXml))

    @Test
    fun extractsOrderedParagraphsJoiningRuns() {
        val xml = """
            <?xml version="1.0"?>
            <w:document xmlns:w="x"><w:body>
              <w:p><w:r><w:t>Hello</w:t></w:r><w:r><w:t xml:space="preserve"> world</w:t></w:r></w:p>
              <w:p><w:r><w:t>Second &amp; last</w:t></w:r></w:p>
            </w:body></w:document>
        """.trimIndent()

        val doc = DocxParser.parse(docx(xml))
        val paragraphs = doc.blocks.filterIsInstance<DocxParser.Paragraph>()
        assertEquals("Hello world", paragraphs[0].text)
        assertEquals("Second & last", paragraphs[1].text)
    }

    @Test
    fun extractsSimpleTable() {
        val xml = """
            <w:document xmlns:w="x"><w:body>
              <w:p><w:r><w:t>Intro</w:t></w:r></w:p>
              <w:tbl>
                <w:tr>
                  <w:tc><w:p><w:r><w:t>A1</w:t></w:r></w:p></w:tc>
                  <w:tc><w:p><w:r><w:t>B1</w:t></w:r></w:p></w:tc>
                </w:tr>
                <w:tr>
                  <w:tc><w:p><w:r><w:t>A2</w:t></w:r></w:p></w:tc>
                  <w:tc><w:p><w:r><w:t>B2</w:t></w:r></w:p></w:tc>
                </w:tr>
              </w:tbl>
            </w:body></w:document>
        """.trimIndent()

        val doc = DocxParser.parse(docx(xml))
        val table = doc.blocks.filterIsInstance<DocxParser.Table>().single()
        assertEquals(listOf("A1", "B1"), table.rows[0])
        assertEquals(listOf("A2", "B2"), table.rows[1])
        // The top-level paragraph before the table is preserved and ordered first.
        assertTrue(doc.blocks.first() is DocxParser.Paragraph)
    }

    @Test
    fun honoursTabsAndBreaks() {
        val xml = """
            <w:document xmlns:w="x"><w:body>
              <w:p><w:r><w:t>Left</w:t></w:r><w:r><w:tab/></w:r><w:r><w:t>Right</w:t></w:r><w:br/><w:r><w:t>Next</w:t></w:r></w:p>
            </w:body></w:document>
        """.trimIndent()

        val doc = DocxParser.parse(docx(xml))
        val p = doc.blocks.filterIsInstance<DocxParser.Paragraph>().first()
        assertEquals("Left\tRight\nNext", p.text)
    }

    @Test
    fun malformedArchiveYieldsEmptyDocument() {
        val doc = DocxParser.parse(byteArrayOf(1, 2, 3, 4))
        assertTrue(doc.blocks.isEmpty())
        assertTrue(doc.isEmpty)
    }

    @Test
    fun missingDocumentPartYieldsEmptyDocument() {
        val doc = DocxParser.parse(zip(mapOf("random.txt" to "hi")))
        assertTrue(doc.isEmpty)
    }

    @Test
    fun documentWithTextIsNotEmpty() {
        val doc = DocxParser.parse(docx("<w:document><w:body><w:p><w:r><w:t>x</w:t></w:r></w:p></w:body></w:document>"))
        assertFalse(doc.isEmpty)
    }
}
