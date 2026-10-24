package com.offlineviewer.app.render.logic

/**
 * A minimal, dependency-free RTF -> styled-text converter.
 *
 * It understands the most common control words (bold/italic/underline,
 * paragraph/line breaks, tabs, unicode escapes and hex escapes) and ignores
 * everything else gracefully, so it never crashes on real documents. The output
 * is a list of [Run]s (text + style flags) which the renderer maps onto an
 * AnnotatedString. The conversion itself is pure and unit-testable.
 */
object RtfConverter {

    data class Run(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
    )

    private data class Style(
        var bold: Boolean = false,
        var italic: Boolean = false,
        var underline: Boolean = false,
    ) {
        fun copyOf() = Style(bold, italic, underline)
    }

    /** Returns true when [input] looks like an RTF document. */
    fun looksLikeRtf(input: String): Boolean = input.trimStart().startsWith("{\\rtf")

    /** Converts [input] RTF into styled runs. Non-RTF input yields a single plain run. */
    fun convert(input: String): List<Run> {
        if (!looksLikeRtf(input)) return listOf(Run(input))

        val runs = ArrayList<Run>()
        val styleStack = ArrayDeque<Style>()
        var style = Style()
        val current = StringBuilder()
        var i = 0
        val n = input.length

        fun flush() {
            if (current.isNotEmpty()) {
                runs.add(Run(current.toString(), style.bold, style.italic, style.underline))
                current.setLength(0)
            }
        }

        while (i < n) {
            when (val c = input[i]) {
                '{' -> {
                    flush()
                    styleStack.addLast(style.copyOf())
                    i++
                }
                '}' -> {
                    flush()
                    style = styleStack.removeLastOrNull() ?: Style()
                    i++
                }
                '\\' -> {
                    i++
                    if (i >= n) break
                    val ctrl = input[i]
                    when {
                        // Escaped literal characters.
                        ctrl == '\\' || ctrl == '{' || ctrl == '}' -> {
                            current.append(ctrl); i++
                        }
                        ctrl == '~' -> { current.append('\u00A0'); i++ } // non-breaking space
                        ctrl == '\n' || ctrl == '\r' -> { current.append('\n'); i++ }
                        ctrl == '\'' -> {
                            // \'hh hex-encoded byte -> char
                            val hex = input.substring(i + 1, (i + 3).coerceAtMost(n))
                            val code = hex.toIntOrNull(16)
                            if (code != null) current.append(code.toChar())
                            i += 3
                        }
                        ctrl.isLetter() -> {
                            // Parse control word + optional numeric parameter.
                            val start = i
                            while (i < n && input[i].isLetter()) i++
                            val word = input.substring(start, i)
                            var param: Int? = null
                            if (i < n && (input[i] == '-' || input[i].isDigit())) {
                                val ps = i
                                if (input[i] == '-') i++
                                while (i < n && input[i].isDigit()) i++
                                param = input.substring(ps, i).toIntOrNull()
                            }
                            // A single optional trailing space is consumed as a delimiter.
                            if (i < n && input[i] == ' ') i++
                            applyControlWord(word, param, current, ::flush) { style }
                        }
                        else -> i++ // unknown escape, skip
                    }
                }
                '\n', '\r' -> i++ // raw line breaks in RTF are not significant
                else -> { current.append(c); i++ }
            }
        }
        flush()
        // Drop the leading font/metadata run that is pure whitespace if present.
        return runs.filter { it.text.isNotEmpty() }
            .ifEmpty { listOf(Run("")) }
    }

    private inline fun applyControlWord(
        word: String,
        param: Int?,
        current: StringBuilder,
        flush: () -> Unit,
        styleRef: () -> Style,
    ) {
        when (word) {
            "b" -> { flush(); styleRef().bold = param != 0 }
            "i" -> { flush(); styleRef().italic = param != 0 }
            "ul" -> { flush(); styleRef().underline = param != 0 }
            "ulnone" -> { flush(); styleRef().underline = false }
            "par", "line" -> current.append('\n')
            "tab" -> current.append('\t')
            "u" -> {
                // \uN unicode, N may be negative (signed 16-bit).
                if (param != null) {
                    val code = if (param < 0) param + 65536 else param
                    current.append(code.toChar())
                }
            }
            // Ignore all other control words (fonts, colors, metadata groups).
            else -> Unit
        }
    }

    /** Flattens runs to plain text (used for fallback / tests). */
    fun toPlainText(runs: List<Run>): String = runs.joinToString("") { it.text }
}
