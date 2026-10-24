package com.offlineviewer.app.render.logic

/**
 * A small, dependency-free XML pretty-printer / indenter.
 *
 * It is intentionally a lightweight structural re-indenter rather than a full
 * validating parser: it tokenises tags and text, then re-emits them with
 * consistent indentation. This keeps it robust (it never throws on slightly
 * malformed markup) and pure/testable on the JVM. The renderer still offers a
 * raw-text fallback when [format] reports the input could not be structured.
 */
object XmlFormatter {

    sealed interface Result {
        data class Formatted(val text: String) : Result
        data class Invalid(val reason: String) : Result
    }

    private const val INDENT = "  "

    fun format(input: String): Result {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Result.Invalid("Empty document")
        if (!trimmed.startsWith("<")) return Result.Invalid("Does not start with a tag")

        return try {
            val sb = StringBuilder()
            var depth = 0
            var i = 0
            var sawTag = false
            val n = trimmed.length

            while (i < n) {
                if (trimmed[i] == '<') {
                    val close = trimmed.indexOf('>', i)
                    if (close < 0) {
                        // Unterminated tag -> treat as not structurable.
                        return Result.Invalid("Unterminated tag")
                    }
                    val tag = trimmed.substring(i, close + 1)
                    sawTag = true
                    val isClosing = tag.startsWith("</")
                    val isSelfClosing = tag.endsWith("/>") ||
                        tag.startsWith("<?") || tag.startsWith("<!")

                    if (isClosing) depth = (depth - 1).coerceAtLeast(0)

                    if (sb.isNotEmpty()) sb.append('\n')
                    sb.append(INDENT.repeat(depth)).append(tag.trim())

                    if (!isClosing && !isSelfClosing) depth++
                    i = close + 1
                } else {
                    val next = trimmed.indexOf('<', i)
                    val end = if (next < 0) n else next
                    val text = trimmed.substring(i, end).trim()
                    if (text.isNotEmpty()) {
                        if (sb.isNotEmpty()) sb.append('\n')
                        sb.append(INDENT.repeat(depth)).append(text)
                    }
                    i = end
                }
            }

            if (!sawTag) Result.Invalid("No XML tags found")
            else Result.Formatted(sb.toString())
        } catch (e: Exception) {
            Result.Invalid(e.message ?: "Malformed XML")
        }
    }
}
