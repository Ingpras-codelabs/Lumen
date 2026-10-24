package com.offlineviewer.app.render.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class DelimitedParserTest {

    @Test
    fun parsesSimpleCsv() {
        val t = DelimitedParser.parse("a,b,c\n1,2,3", ',')
        assertEquals(2, t.rowCount)
        assertEquals(3, t.columnCount)
        assertEquals(listOf("a", "b", "c"), t.rows[0])
        assertEquals(listOf("1", "2", "3"), t.rows[1])
    }

    @Test
    fun handlesQuotedFieldWithEmbeddedDelimiter() {
        val t = DelimitedParser.parse("\"a,b\",c", ',')
        assertEquals(listOf("a,b", "c"), t.rows[0])
    }

    @Test
    fun handlesEscapedQuotesInsideQuotedField() {
        val t = DelimitedParser.parse("\"she said \"\"hi\"\"\",x", ',')
        assertEquals(listOf("she said \"hi\"", "x"), t.rows[0])
    }

    @Test
    fun handlesEmbeddedNewlineInQuotedField() {
        val t = DelimitedParser.parse("\"line1\nline2\",b\nc,d", ',')
        assertEquals(2, t.rowCount)
        assertEquals(listOf("line1\nline2", "b"), t.rows[0])
        assertEquals(listOf("c", "d"), t.rows[1])
    }

    @Test
    fun handlesCrlfLineEndings() {
        val t = DelimitedParser.parse("a,b\r\nc,d\r\n", ',')
        assertEquals(2, t.rowCount)
        assertEquals(listOf("a", "b"), t.rows[0])
        assertEquals(listOf("c", "d"), t.rows[1])
    }

    @Test
    fun preservesRaggedRows() {
        val t = DelimitedParser.parse("a,b,c\n1,2\nx", ',')
        assertEquals(3, t.rowCount)
        assertEquals(3, t.columnCount)
        assertEquals(listOf("1", "2"), t.rows[1])
        assertEquals(listOf("x"), t.rows[2])
    }

    @Test
    fun parsesTsv() {
        val t = DelimitedParser.parse("a\tb\tc\n1\t2\t3", '\t')
        assertEquals(listOf("a", "b", "c"), t.rows[0])
        assertEquals(listOf("1", "2", "3"), t.rows[1])
    }

    @Test
    fun emptyInputYieldsNoRows() {
        val t = DelimitedParser.parse("", ',')
        assertEquals(0, t.rowCount)
    }

    @Test
    fun delimiterSelectionByExtensionAndMime() {
        assertEquals('\t', DelimitedParser.delimiterFor("tsv", null))
        assertEquals('\t', DelimitedParser.delimiterFor(null, "text/tab-separated-values"))
        assertEquals(',', DelimitedParser.delimiterFor("csv", null))
        assertEquals(',', DelimitedParser.delimiterFor(null, "text/csv"))
    }
}
