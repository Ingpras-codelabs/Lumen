package com.offlineviewer.app.render.logic

/**
 * A dependency-free parser for delimited text (CSV / TSV).
 *
 * Handles the common RFC-4180-ish rules so renderers never crash on real-world
 * files:
 *  - fields separated by [delimiter]
 *  - fields optionally wrapped in double quotes
 *  - quotes inside a quoted field escaped by doubling ("")
 *  - embedded delimiters and newlines allowed inside quoted fields
 *  - both \n and \r\n row terminators
 *  - ragged rows (differing column counts) are preserved as-is; the caller
 *    decides how to pad/display them
 *
 * The parser is pure and framework-free so it is fully unit-testable on the JVM.
 */
object DelimitedParser {

    /** Result of parsing: the raw rows plus the widest row's column count. */
    data class Table(val rows: List<List<String>>) {
        val columnCount: Int get() = rows.maxOfOrNull { it.size } ?: 0
        val rowCount: Int get() = rows.size
    }

    /** Parses [input] using the [delimiter] (',' for CSV, '\t' for TSV). */
    fun parse(input: String, delimiter: Char): Table {
        val rows = ArrayList<List<String>>()
        var field = StringBuilder()
        var row = ArrayList<String>()
        var inQuotes = false
        var i = 0
        var sawAnyContent = false

        fun endField() {
            row.add(field.toString())
            field = StringBuilder()
        }
        fun endRow() {
            endField()
            rows.add(row)
            row = ArrayList()
        }

        while (i < input.length) {
            val c = input[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < input.length && input[i + 1] == '"') {
                        field.append('"')
                        i += 2
                        continue
                    }
                    inQuotes = false
                    i++
                    continue
                }
                field.append(c)
                i++
                continue
            }

            when (c) {
                '"' -> {
                    inQuotes = true
                    sawAnyContent = true
                    i++
                }
                delimiter -> {
                    sawAnyContent = true
                    endField()
                    i++
                }
                '\r' -> {
                    sawAnyContent = true
                    // Treat \r\n as one terminator; a lone \r also terminates.
                    endRow()
                    if (i + 1 < input.length && input[i + 1] == '\n') i += 2 else i++
                }
                '\n' -> {
                    sawAnyContent = true
                    endRow()
                    i++
                }
                else -> {
                    sawAnyContent = true
                    field.append(c)
                    i++
                }
            }
        }

        // Flush the final field/row unless the input ended exactly on a row
        // terminator (in which case we already closed that row).
        if (field.isNotEmpty() || row.isNotEmpty() || sawAnyContent && rows.isEmpty()) {
            endRow()
        }

        return Table(rows)
    }

    /** Picks the delimiter from an extension/mime hint. TSV -> tab, else comma. */
    fun delimiterFor(extension: String?, mimeType: String?): Char {
        val ext = extension?.lowercase()?.removePrefix(".")
        val mime = mimeType?.lowercase()
        return if (ext == "tsv" || mime == "text/tab-separated-values") '\t' else ','
    }
}
