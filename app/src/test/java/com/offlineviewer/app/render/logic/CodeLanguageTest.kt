package com.offlineviewer.app.render.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeLanguageTest {

    @Test
    fun detectsCommonLanguagesByExtension() {
        assertEquals(CodeLanguage.KOTLIN, CodeLanguage.fromExtension("kt"))
        assertEquals(CodeLanguage.KOTLIN, CodeLanguage.fromExtension("kts"))
        assertEquals(CodeLanguage.JAVA, CodeLanguage.fromExtension("java"))
        assertEquals(CodeLanguage.JAVASCRIPT, CodeLanguage.fromExtension("js"))
        assertEquals(CodeLanguage.TYPESCRIPT, CodeLanguage.fromExtension("ts"))
        assertEquals(CodeLanguage.PYTHON, CodeLanguage.fromExtension("py"))
        assertEquals(CodeLanguage.CPP, CodeLanguage.fromExtension("cpp"))
        assertEquals(CodeLanguage.GO, CodeLanguage.fromExtension("go"))
        assertEquals(CodeLanguage.RUST, CodeLanguage.fromExtension("rs"))
        assertEquals(CodeLanguage.SQL, CodeLanguage.fromExtension("sql"))
        assertEquals(CodeLanguage.SHELL, CodeLanguage.fromExtension("sh"))
    }

    @Test
    fun handlesLeadingDotAndCaseAndWhitespace() {
        assertEquals(CodeLanguage.KOTLIN, CodeLanguage.fromExtension(".KT"))
        assertEquals(CodeLanguage.PYTHON, CodeLanguage.fromExtension("  Py  "))
    }

    @Test
    fun unknownExtensionFallsBackToPlain() {
        assertEquals(CodeLanguage.PLAIN, CodeLanguage.fromExtension("xyz"))
        assertEquals(CodeLanguage.PLAIN, CodeLanguage.fromExtension(null))
        assertEquals(CodeLanguage.PLAIN, CodeLanguage.fromExtension(""))
    }

    @Test
    fun allExtensionsIncludeExpectedSet() {
        val exts = CodeLanguage.allExtensions
        assertTrue(exts.containsAll(listOf("kt", "java", "js", "ts", "py", "c", "cpp", "cs", "go", "rs", "rb", "php", "swift", "sh", "sql", "css", "toml", "ini", "properties")))
    }
}
