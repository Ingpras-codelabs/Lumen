package com.offlineviewer.app.render.logic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Lightweight, regex-based syntax highlighting producing a Compose
 * [AnnotatedString]. This is UI-adjacent (it uses Compose text types) but has
 * no Android framework dependency, so it stays in the logic package and could
 * be tested with Robolectric if needed; the pure parsing/formatting lives in
 * the sibling formatter objects which ARE unit-tested.
 *
 * The palette uses fixed, theme-independent colours chosen to read on both
 * light and dark surfaces.
 */
object SyntaxHighlighter {

    private val KEY = Color(0xFF1565C0)      // blue
    private val STRING = Color(0xFF2E7D32)   // green
    private val NUMBER = Color(0xFFAD1457)   // magenta
    private val KEYWORD = Color(0xFF6A1B9A)  // purple
    private val COMMENT = Color(0xFF757575)  // grey
    private val PUNCT = Color(0xFF455A64)    // blue-grey
    private val TAG = Color(0xFF1565C0)

    private val jsonTokens = Regex(
        """"(?:\\.|[^"\\])*"(?=\s*:)""" +          // keys
            """|"(?:\\.|[^"\\])*"""" +              // strings
            """|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?""" + // numbers
            """|\btrue\b|\bfalse\b|\bnull\b""",       // literals
    )

    fun highlightJson(text: String): AnnotatedString = buildAnnotatedString {
        var last = 0
        for (m in jsonTokens.findAll(text)) {
            append(text.substring(last, m.range.first))
            val tok = m.value
            val isString = tok.startsWith("\"")
            val followedByColon = text.substring(m.range.last + 1).trimStart().startsWith(":")
            val style = when {
                isString && followedByColon -> SpanStyle(color = KEY)
                isString -> SpanStyle(color = STRING)
                tok == "true" || tok == "false" || tok == "null" -> SpanStyle(color = KEYWORD)
                else -> SpanStyle(color = NUMBER)
            }
            withStyle(style) { append(tok) }
            last = m.range.last + 1
        }
        append(text.substring(last))
    }

    private val xmlTokens = Regex("""<!--[\s\S]*?-->|<[^>]+>""")

    fun highlightXml(text: String): AnnotatedString = buildAnnotatedString {
        var last = 0
        for (m in xmlTokens.findAll(text)) {
            append(text.substring(last, m.range.first))
            val tok = m.value
            if (tok.startsWith("<!--")) {
                withStyle(SpanStyle(color = COMMENT)) { append(tok) }
            } else {
                withStyle(SpanStyle(color = TAG)) { append(tok) }
            }
            last = m.range.last + 1
        }
        append(text.substring(last))
    }

    private val yamlKey = Regex("""(?m)^(\s*)([\w.\-]+)(\s*:)""")
    private val yamlComment = Regex("""(?m)#.*$""")

    fun highlightYaml(text: String): AnnotatedString = buildAnnotatedString {
        // Highlight line by line: comments, keys, strings.
        val lines = text.split("\n")
        for ((idx, line) in lines.withIndex()) {
            val commentAt = line.indexOf('#')
            val code = if (commentAt >= 0) line.substring(0, commentAt) else line
            val comment = if (commentAt >= 0) line.substring(commentAt) else null

            val keyMatch = yamlKey.find(code)
            if (keyMatch != null) {
                val (indent, key, colon) = keyMatch.destructured
                append(indent)
                withStyle(SpanStyle(color = KEY)) { append(key) }
                withStyle(SpanStyle(color = PUNCT)) { append(colon) }
                append(code.substring(keyMatch.range.last + 1))
            } else {
                append(code)
            }
            if (comment != null) {
                withStyle(SpanStyle(color = COMMENT)) { append(comment) }
            }
            if (idx < lines.size - 1) append("\n")
        }
    }

    /**
     * Highlights source [code] for the given [language]: line comments,
     * double/single-quoted strings, numbers, and keywords.
     */
    fun highlightCode(code: String, language: CodeLanguage): AnnotatedString {
        val keywords = language.keywords
        val lineComment = language.lineComment
        val wordRegex = Regex("""[A-Za-z_]\w*|\d+(?:\.\d+)?|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'""")

        return buildAnnotatedString {
            val lines = code.split("\n")
            for ((idx, line) in lines.withIndex()) {
                var commentStart = -1
                if (lineComment != null) {
                    commentStart = indexOfCommentOutsideString(line, lineComment)
                }
                val codePart = if (commentStart >= 0) line.substring(0, commentStart) else line
                val commentPart = if (commentStart >= 0) line.substring(commentStart) else null

                var last = 0
                for (m in wordRegex.findAll(codePart)) {
                    append(codePart.substring(last, m.range.first))
                    val tok = m.value
                    val style = when {
                        tok.startsWith("\"") || tok.startsWith("'") -> SpanStyle(color = STRING)
                        tok.first().isDigit() -> SpanStyle(color = NUMBER)
                        keywords.contains(tok) -> SpanStyle(color = KEYWORD)
                        else -> null
                    }
                    if (style != null) withStyle(style) { append(tok) } else append(tok)
                    last = m.range.last + 1
                }
                append(codePart.substring(last))
                if (commentPart != null) withStyle(SpanStyle(color = COMMENT)) { append(commentPart) }
                if (idx < lines.size - 1) append("\n")
            }
        }
    }

    /** Finds a line-comment marker that is not inside a quoted string. */
    private fun indexOfCommentOutsideString(line: String, marker: String): Int {
        var inSingle = false
        var inDouble = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inSingle -> inDouble = !inDouble
                c == '\'' && !inDouble -> inSingle = !inSingle
                !inSingle && !inDouble && line.startsWith(marker, i) -> return i
            }
            i++
        }
        return -1
    }
}
