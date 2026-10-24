package com.offlineviewer.app.render.logic

import org.junit.Assert.assertTrue
import org.junit.Test

class XmlFormatterTest {

    @Test
    fun indentsNestedElements() {
        val r = XmlFormatter.format("<a><b>x</b></a>")
        assertTrue(r is XmlFormatter.Result.Formatted)
        val text = (r as XmlFormatter.Result.Formatted).text
        val lines = text.split("\n")
        assertTrue(lines.any { it.trimStart() == "<a>" })
        // <b> is nested, so it must be indented deeper than <a>.
        val aIndent = lines.first { it.contains("<a>") }.takeWhile { it == ' ' }.length
        val bIndent = lines.first { it.contains("<b>") }.takeWhile { it == ' ' }.length
        assertTrue(bIndent > aIndent)
    }

    @Test
    fun handlesSelfClosingAndDeclaration() {
        val r = XmlFormatter.format("""<?xml version="1.0"?><root><item/></root>""")
        assertTrue(r is XmlFormatter.Result.Formatted)
    }

    @Test
    fun unterminatedTagIsInvalid() {
        val r = XmlFormatter.format("<a><b")
        assertTrue(r is XmlFormatter.Result.Invalid)
    }

    @Test
    fun nonTagInputInvalid() {
        assertTrue(XmlFormatter.format("hello world") is XmlFormatter.Result.Invalid)
        assertTrue(XmlFormatter.format("  ") is XmlFormatter.Result.Invalid)
    }
}
