package com.offlineviewer.app.render.logic

/**
 * A minimal, dependency-free reader for Excel OOXML (.xlsx) built on
 * [java.util.zip].
 *
 * Shared strings live in `xl/sharedStrings.xml` as an indexed `<si>` list; each
 * worksheet (`xl/worksheets/sheetN.xml`) holds `<c>` cells whose `r="A1"`
 * reference gives the column/row and whose `t` attribute selects the value
 * source (`t="s"` -> shared-string index, `t="inlineStr"` -> inline `<is>` text,
 * `t="str"` -> formula string, otherwise a numeric/boolean literal). Sheet order
 * follows `xl/workbook.xml`.
 *
 * Parsing is tolerant substring/regex scanning and defensive: a malformed or
 * empty archive yields an empty workbook rather than throwing.
 */
object XlsxParser {

    data class Sheet(val name: String, val rows: List<List<String>>)
    data class Workbook(val sheets: List<Sheet>) {
        val isEmpty: Boolean get() = sheets.none { sheet ->
            sheet.rows.any { row -> row.any { it.isNotEmpty() } }
        }
    }

    fun parse(bytes: ByteArray): Workbook {
        return try {
            val entries = OoxmlZip.readEntries(bytes)
            val shared = parseSharedStrings(entries["xl/sharedStrings.xml"]?.toString(Charsets.UTF_8))
            val order = resolveSheetOrder(entries)
            val sheets = order.mapNotNull { (name, part) ->
                val xml = entries[part]?.toString(Charsets.UTF_8) ?: return@mapNotNull null
                Sheet(name, parseSheet(xml, shared))
            }
            Workbook(sheets)
        } catch (e: Throwable) {
            Workbook(emptyList())
        }
    }

    /** Parses the indexed shared-string table; each `<si>` may hold several `<t>`. */
    private fun parseSharedStrings(xml: String?): List<String> {
        if (xml == null) return emptyList()
        val result = ArrayList<String>()
        val siRegex = Regex("""<si\b[^>]*>([\s\S]*?)</si>""", RegexOption.IGNORE_CASE)
        for (si in siRegex.findAll(xml)) {
            val sb = StringBuilder()
            for (t in Regex("""<t\b[^>]*>([\s\S]*?)</t>""", RegexOption.IGNORE_CASE).findAll(si.groupValues[1])) {
                sb.append(OoxmlZip.decodeEntities(t.groupValues[1]))
            }
            result.add(sb.toString())
        }
        return result
    }

    /**
     * Returns the ordered list of (sheetName, worksheet-part-path). Order comes
     * from `xl/workbook.xml` (+ the relationship ids in `_rels`); when those are
     * missing we fall back to the numeric order of the `sheetN.xml` files.
     */
    private fun resolveSheetOrder(entries: Map<String, ByteArray>): List<Pair<String, String>> {
        val workbook = entries["xl/workbook.xml"]?.toString(Charsets.UTF_8)
        val rels = entries["xl/_rels/workbook.xml.rels"]?.toString(Charsets.UTF_8)

        if (workbook != null) {
            val relMap = parseRels(rels) // rId -> target
            val ordered = ArrayList<Pair<String, String>>()
            val sheetRegex = Regex("""<sheet\b[^>]*/?>""", RegexOption.IGNORE_CASE)
            var fallbackIndex = 0
            for (tag in sheetRegex.findAll(workbook)) {
                fallbackIndex++
                val name = attr(tag.value, "name") ?: "Sheet$fallbackIndex"
                val rid = attr(tag.value, "r:id") ?: attr(tag.value, "id")
                val target = rid?.let { relMap[it] }
                val part = when {
                    target != null -> normalizePart(target)
                    else -> "xl/worksheets/sheet$fallbackIndex.xml"
                }
                if (entries.containsKey(part)) {
                    ordered.add(OoxmlZip.decodeEntities(name) to part)
                }
            }
            if (ordered.isNotEmpty()) return ordered
        }

        // Fallback: every sheetN.xml in numeric order.
        return entries.keys
            .filter { it.matches(Regex("""xl/worksheets/sheet\d+\.xml""", RegexOption.IGNORE_CASE)) }
            .sortedBy { sheetNumber(it) }
            .mapIndexed { i, part -> "Sheet${i + 1}" to part }
    }

    private fun parseRels(rels: String?): Map<String, String> {
        if (rels == null) return emptyMap()
        val map = LinkedHashMap<String, String>()
        for (tag in Regex("""<Relationship\b[^>]*/?>""", RegexOption.IGNORE_CASE).findAll(rels)) {
            val id = attr(tag.value, "Id") ?: continue
            val target = attr(tag.value, "Target") ?: continue
            map[id] = target
        }
        return map
    }

    /** Resolves a workbook-relative relationship target to a full zip-entry path. */
    private fun normalizePart(target: String): String {
        val clean = target.removePrefix("/")
        return if (clean.startsWith("xl/")) clean else "xl/$clean"
    }

    private fun parseSheet(xml: String, shared: List<String>): List<List<String>> {
        val rows = ArrayList<List<String>>()
        val rowRegex = Regex("""<row\b[^>]*>([\s\S]*?)</row>|<row\b[^>]*/>""", RegexOption.IGNORE_CASE)
        for (rowMatch in rowRegex.findAll(xml)) {
            val inner = rowMatch.groupValues[1]
            val byColumn = sortedMapOf<Int, String>()
            var maxCol = -1
            val cellRegex = Regex("""<c\b([^>]*?)(?:/>|>([\s\S]*?)</c>)""", RegexOption.IGNORE_CASE)
            for (cell in cellRegex.findAll(inner)) {
                val cellAttrs = cell.groupValues[1]
                val cellBody = cell.groupValues[2]
                val ref = attr(cellAttrs, "r")
                val colIndex = ref?.let { columnIndex(it) } ?: (maxCol + 1)
                maxCol = maxOf(maxCol, colIndex)
                byColumn[colIndex] = cellValue(cellAttrs, cellBody, shared)
            }
            if (byColumn.isEmpty()) {
                rows.add(emptyList())
                continue
            }
            val width = (byColumn.lastKey()) + 1
            val row = ArrayList<String>(width)
            for (c in 0 until width) row.add(byColumn[c] ?: "")
            rows.add(row)
        }
        return rows
    }

    private fun cellValue(cellAttrs: String, cellBody: String, shared: List<String>): String {
        val type = attr(cellAttrs, "t")
        return when (type?.lowercase()) {
            "s" -> {
                val index = valueOf(cellBody)?.trim()?.toIntOrNull()
                if (index != null && index in shared.indices) shared[index] else ""
            }
            "inlinestr" -> {
                val sb = StringBuilder()
                for (t in Regex("""<t\b[^>]*>([\s\S]*?)</t>""", RegexOption.IGNORE_CASE).findAll(cellBody)) {
                    sb.append(OoxmlZip.decodeEntities(t.groupValues[1]))
                }
                sb.toString()
            }
            else -> OoxmlZip.decodeEntities(valueOf(cellBody) ?: "")
        }
    }

    /** Extracts the `<v>` value text of a cell body, if present. */
    private fun valueOf(cellBody: String): String? =
        Regex("""<v\b[^>]*>([\s\S]*?)</v>""", RegexOption.IGNORE_CASE)
            .find(cellBody)?.groupValues?.get(1)

    /** Converts a cell reference like "AB12" to a 0-based column index. */
    internal fun columnIndex(ref: String): Int {
        var result = 0
        for (ch in ref) {
            val upper = ch.uppercaseChar()
            if (upper in 'A'..'Z') {
                result = result * 26 + (upper - 'A' + 1)
            } else {
                break // reached the row-number portion
            }
        }
        return (result - 1).coerceAtLeast(0)
    }

    private fun sheetNumber(part: String): Int =
        Regex("""sheet(\d+)\.xml""", RegexOption.IGNORE_CASE)
            .find(part)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE

    private fun attr(tag: String, name: String): String? =
        Regex("""\b${Regex.escape(name)}\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
            .find(tag)?.groupValues?.get(1)
}
