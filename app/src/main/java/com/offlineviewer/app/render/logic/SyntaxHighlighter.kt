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
 * Colours are theme-aware: callers pass [dark] so tokens keep adequate
 * contrast on BOTH light and dark surfaces (lighter, less saturated hues on
 * dark; deeper hues on light). Colour is always paired with the surrounding
 * monospace layout, so it is never the sole carrier of meaning.
 */
object SyntaxHighlighter {

    /** A per-token-type colour palette resolved for the current theme. */
    private class Palette(dark: Boolean) {
        val key: Color = if (dark) Color(0xFF82B1FF) else Color(0xFF1565C0)        // blue
        val string: Color = if (dark) Color(0xFF9CCC65) else Color(0xFF2E7D32)     // green
        val number: Color = if (dark) Color(0xFFF48FB1) else Color(0xFFAD1457)     // magenta
        val keyword: Color = if (dark) Color(0xFFCE93D8) else Color(0xFF6A1B9A)    // purple
        val comment: Color = if (dark) Color(0xFFB0BEC5) else Color(0xFF616161)    // grey
        val punct: Color = if (dark) Color(0xFF90A4AE) else Color(0xFF455A64)      // blue-grey
        val tag: Color = if (dark) Color(0xFF82B1FF) else Color(0xFF1565C0)        // blue
    }

    private val jsonTokens = Regex(
        """"(?:\\.|[^"\\])*"(?=\s*:)""" +          // keys
            """|"(?:\\.|[^"\\])*"""" +              // strings
            """|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?""" + // numbers
            """|\btrue\b|\bfalse\b|\bnull\b""",       // literals
    )

    fun highlightJson(text: String, dark: Boolean = false): AnnotatedString {
        val p = Palette(dark)
        return buildAnnotatedString {
            var last = 0
            for (m in jsonTokens.findAll(text)) {
                append(text.substring(last, m.range.first))
                val tok = m.value
                val isString = tok.startsWith("\"")
                val followedByColon = text.substring(m.range.last + 1).trimStart().startsWith(":")
                val style = when {
                    isString && followedByColon -> SpanStyle(color = p.key)
                    isString -> SpanStyle(color = p.string)
                    tok == "true" || tok == "false" || tok == "null" -> SpanStyle(color = p.keyword)
                    else -> SpanStyle(color = p.number)
                }
                withStyle(style) { append(tok) }
                last = m.range.last + 1
            }
            append(text.substring(last))
        }
    }

    private val xmlTokens = Regex("""<!--[\s\S]*?-->|<[^>]+>""")

    fun highlightXml(text: String, dark: Boolean = false): AnnotatedString {
        val p = Palette(dark)
        return buildAnnotatedString {
            var last = 0
            for (m in xmlTokens.findAll(text)) {
                append(text.substring(last, m.range.first))
                val tok = m.value
                if (tok.startsWith("<!--")) {
                    withStyle(SpanStyle(color = p.comment)) { append(tok) }
                } else {
                    withStyle(SpanStyle(color = p.tag)) { append(tok) }
                }
                last = m.range.last + 1
            }
            append(text.substring(last))
        }
    }

    private val yamlKey = Regex("""(?m)^(\s*)([\w.\-]+)(\s*:)""")
    private val yamlComment = Regex("""(?m)#.*$""")

    fun highlightYaml(text: String, dark: Boolean = false): AnnotatedString {
        val p = Palette(dark)
        return buildAnnotatedString {
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
                    withStyle(SpanStyle(color = p.key)) { append(key) }
                    withStyle(SpanStyle(color = p.punct)) { append(colon) }
                    append(code.substring(keyMatch.range.last + 1))
                } else {
                    append(code)
                }
                if (comment != null) {
                    withStyle(SpanStyle(color = p.comment)) { append(comment) }
                }
                if (idx < lines.size - 1) append("\n")
            }
        }
    }

    /**
     * Highlights source [code] for the given [language]: line comments,
     * double/single-quoted strings, numbers, and keywords.
     */
    fun highlightCode(code: String, language: CodeLanguage, dark: Boolean = false): AnnotatedString {
        val p = Palette(dark)
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
                        tok.startsWith("\"") || tok.startsWith("'") -> SpanStyle(color = p.string)
                        tok.first().isDigit() -> SpanStyle(color = p.number)
                        keywords.contains(tok) -> SpanStyle(color = p.keyword)
                        else -> null
                    }
                    if (style != null) withStyle(style) { append(tok) } else append(tok)
                    last = m.range.last + 1
                }
                append(codePart.substring(last))
                if (commentPart != null) withStyle(SpanStyle(color = p.comment)) { append(commentPart) }
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
