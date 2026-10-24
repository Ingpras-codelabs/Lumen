package com.offlineviewer.app.render.logic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YamlValidatorTest {

    @Test
    fun validMappingIsValid() {
        assertTrue(YamlValidator.isValid("name: test\nitems:\n  - a\n  - b\n"))
    }

    @Test
    fun validMultiDocumentIsValid() {
        assertTrue(YamlValidator.isValid("---\na: 1\n---\nb: 2\n"))
    }

    @Test
    fun malformedYamlFallsBack() {
        // Bad indentation / unclosed flow mapping.
        assertFalse(YamlValidator.isValid("a: [1, 2"))
    }

    @Test
    fun emptyIsInvalid() {
        assertFalse(YamlValidator.isValid("   "))
    }

    @Test
    fun scalarDocumentIsValid() {
        assertTrue(YamlValidator.isValid("just a string"))
    }
}
