package com.offlineviewer.app.render.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class XlsxParserTest {

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
    fun resolvesSharedStringsAndColumnOrder() {
        val sharedStrings = """
            <sst xmlns="x" count="3" uniqueCount="3">
              <si><t>Name</t></si>
              <si><t>Age</t></si>
              <si><t>Alice</t></si>
            </sst>
        """.trimIndent()
        // Columns intentionally out of order in the XML to prove reference ordering.
        val sheet1 = """
            <worksheet xmlns="x"><sheetData>
              <row r="1"><c r="B1" t="s"><v>1</v></c><c r="A1" t="s"><v>0</v></c></row>
              <row r="2"><c r="A2" t="s"><v>2</v></c><c r="B2"><v>30</v></c></row>
            </sheetData></worksheet>
        """.trimIndent()
        val workbook = """
            <workbook xmlns:r="rel">
              <sheets>
                <sheet name="People" sheetId="1" r:id="rId1"/>
              </sheets>
            </workbook>
        """.trimIndent()
        val rels = """
            <Relationships>
              <Relationship Id="rId1" Target="worksheets/sheet1.xml"/>
            </Relationships>
        """.trimIndent()

        val bytes = zip(
            mapOf(
                "xl/sharedStrings.xml" to sharedStrings,
                "xl/worksheets/sheet1.xml" to sheet1,
                "xl/workbook.xml" to workbook,
                "xl/_rels/workbook.xml.rels" to rels,
            ),
        )

        val wb = XlsxParser.parse(bytes)
        assertEquals(1, wb.sheets.size)
        assertEquals("People", wb.sheets[0].name)
        // Column A then B regardless of XML ordering.
        assertEquals(listOf("Name", "Age"), wb.sheets[0].rows[0])
        assertEquals(listOf("Alice", "30"), wb.sheets[0].rows[1])
    }

    @Test
    fun handlesInlineStringsAndNumbers() {
        val sheet1 = """
            <worksheet xmlns="x"><sheetData>
              <row r="1">
                <c r="A1" t="inlineStr"><is><t>Inline</t></is></c>
                <c r="B1"><v>42</v></c>
              </row>
            </sheetData></worksheet>
        """.trimIndent()
        val bytes = zip(mapOf("xl/worksheets/sheet1.xml" to sheet1))

        val wb = XlsxParser.parse(bytes)
        assertEquals(listOf("Inline", "42"), wb.sheets[0].rows[0])
    }

    @Test
    fun respectsWorkbookSheetOrder() {
        val workbook = """
            <workbook xmlns:r="rel"><sheets>
              <sheet name="Second" r:id="rId1"/>
              <sheet name="First" r:id="rId2"/>
            </sheets></workbook>
        """.trimIndent()
        val rels = """
            <Relationships>
              <Relationship Id="rId1" Target="worksheets/sheet2.xml"/>
              <Relationship Id="rId2" Target="worksheets/sheet1.xml"/>
            </Relationships>
        """.trimIndent()
        val sheet1 = """<worksheet><sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>one</t></is></c></row></sheetData></worksheet>"""
        val sheet2 = """<worksheet><sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>two</t></is></c></row></sheetData></worksheet>"""

        val bytes = zip(
            mapOf(
                "xl/workbook.xml" to workbook,
                "xl/_rels/workbook.xml.rels" to rels,
                "xl/worksheets/sheet1.xml" to sheet1,
                "xl/worksheets/sheet2.xml" to sheet2,
            ),
        )

        val wb = XlsxParser.parse(bytes)
        assertEquals(listOf("Second", "First"), wb.sheets.map { it.name })
        assertEquals("two", wb.sheets[0].rows[0][0])
        assertEquals("one", wb.sheets[1].rows[0][0])
    }

    @Test
    fun columnIndexConvertsReferences() {
        assertEquals(0, XlsxParser.columnIndex("A1"))
        assertEquals(1, XlsxParser.columnIndex("B2"))
        assertEquals(26, XlsxParser.columnIndex("AA10"))
        assertEquals(27, XlsxParser.columnIndex("AB1"))
    }

    @Test
    fun malformedArchiveYieldsEmptyWorkbook() {
        val wb = XlsxParser.parse(byteArrayOf(9, 8, 7))
        assertTrue(wb.sheets.isEmpty())
        assertTrue(wb.isEmpty)
    }
}
