package com.offlineviewer.app.render.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonFormatterTest {

    @Test
    fun prettyPrintsObjectWithIndentation() {
        val r = JsonFormatter.format("""{"a":1,"b":[2,3]}""")
        assertTrue(r is JsonFormatter.Result.Formatted)
        val text = (r as JsonFormatter.Result.Formatted).text
        // Indented output spans multiple lines.
        assertTrue(text.contains("\n"))
        assertTrue(text.contains("\"a\""))
        assertTrue(text.contains("  "))
    }

    @Test
    fun prettyPrintsTopLevelArray() {
        val r = JsonFormatter.format("""[1,2,3]""")
        assertTrue(r is JsonFormatter.Result.Formatted)
    }

    @Test
    fun invalidJsonReturnsInvalid() {
        val r = JsonFormatter.format("""{"a":}""")
        assertTrue(r is JsonFormatter.Result.Invalid)
    }

    @Test
    fun scalarIsNotTreatedAsDocument() {
        val r = JsonFormatter.format("42")
        assertTrue(r is JsonFormatter.Result.Invalid)
    }

    @Test
    fun emptyInputInvalid() {
        assertTrue(JsonFormatter.format("   ") is JsonFormatter.Result.Invalid)
    }
}
