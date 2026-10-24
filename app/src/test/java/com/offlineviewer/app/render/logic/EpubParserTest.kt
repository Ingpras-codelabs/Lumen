package com.offlineviewer.app.render.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubParserTest {

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

    @Test
    fun readsSpineOrderedChapters() {
        val container = """
            <?xml version="1.0"?>
            <container><rootfiles>
            <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
            </rootfiles></container>
        """.trimIndent()
        val opf = """
            <?xml version="1.0"?>
            <package>
              <manifest>
                <item id="c1" href="chap1.xhtml" media-type="application/xhtml+xml"/>
                <item id="c2" href="chap2.xhtml" media-type="application/xhtml+xml"/>
              </manifest>
              <spine>
                <itemref idref="c1"/>
                <itemref idref="c2"/>
              </spine>
            </package>
        """.trimIndent()
        val bytes = zip(
            mapOf(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to opf,
                "OEBPS/chap1.xhtml" to "<html><body>One</body></html>",
                "OEBPS/chap2.xhtml" to "<html><body>Two</body></html>",
            ),
        )

        val book = EpubParser.parse(bytes)
        assertEquals(2, book.chapters.size)
        assertTrue(book.chapters[0].html.contains("One"))
        assertTrue(book.chapters[1].html.contains("Two"))
    }

    @Test
    fun malformedArchiveYieldsEmptyBook() {
        val book = EpubParser.parse(byteArrayOf(1, 2, 3, 4))
        assertEquals(0, book.chapters.size)
    }

    @Test
    fun missingOpfYieldsEmptyBook() {
        val bytes = zip(mapOf("random.txt" to "hi"))
        assertEquals(0, EpubParser.parse(bytes).chapters.size)
    }
}
