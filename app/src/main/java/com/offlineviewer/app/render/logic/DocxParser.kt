package com.offlineviewer.app.render.logic

/**
 * A minimal, dependency-free reader for Word OOXML (.docx) built on
 * [java.util.zip]. The document body lives in `word/document.xml`; this parser
 * walks it in document order, joining the `w:t` text runs inside each paragraph
 * (`w:p`) and detecting simple tables (`w:tbl` / `w:tr` / `w:tc`).
 *
 * Parsing is tolerant substring/regex scanning (not a validating parser) and is
 * defensive: a malformed or empty archive yields an empty [Document] rather
 * than throwing, so the renderer can show a clear error state.
 */
object DocxParser {

    /** One ordered block of the document: either a paragraph or a table. */
    sealed interface Block

    /** A run of paragraph text (may be empty for a blank line). */
    data class Paragraph(val text: String) : Block

    /** A simple table: rows of cells, each cell already flattened to text. */
    data class Table(val rows: List<List<String>>) : Block

    data class Document(val blocks: List<Block>) {
        /** True when nothing renderable was extracted. */
        val isEmpty: Boolean get() = blocks.none {
            (it is Paragraph && it.text.isNotEmpty()) ||
                (it is Table && it.rows.any { row -> row.any { cell -> cell.isNotEmpty() } })
        }
    }

    private const val DOCUMENT_PART = "word/document.xml"

    fun parse(bytes: ByteArray): Document {
        return try {
            val entries = OoxmlZip.readEntries(bytes)
            val xml = entries[DOCUMENT_PART]?.toString(Charsets.UTF_8)
                ?: return Document(emptyList())
            val body = Regex("""<w:body\b[^>]*>([\s\S]*?)</w:body>""", RegexOption.IGNORE_CASE)
                .find(xml)?.groupValues?.get(1) ?: xml
            Document(parseBlocks(body))
        } catch (e: Throwable) {
            Document(emptyList())
        }
    }

    /**
     * Walks [body] in order, emitting a [Table] for each top-level `w:tbl` and a
     * [Paragraph] for each top-level `w:p`. Paragraphs nested inside a table are
     * consumed by the table, so only top-level paragraphs become blocks.
     */
    private fun parseBlocks(body: String): List<Block> {
        val blocks = ArrayList<Block>()
        val element = Regex("""<w:(tbl|p)\b[^>]*>([\s\S]*?)</w:\1>""", RegexOption.IGNORE_CASE)
        var lastEnd = 0
        for (m in element.findAll(body)) {
            // Skip matches that are nested inside an already-consumed element.
            if (m.range.first < lastEnd) continue
            when (m.groupValues[1].lowercase()) {
                "tbl" -> blocks.add(parseTable(m.groupValues[2]))
                "p" -> blocks.add(Paragraph(paragraphText(m.value)))
            }
            lastEnd = m.range.last + 1
        }
        return blocks
    }

    private fun parseTable(tableInner: String): Table {
        val rows = ArrayList<List<String>>()
        val rowRegex = Regex("""<w:tr\b[^>]*>([\s\S]*?)</w:tr>""", RegexOption.IGNORE_CASE)
        for (rowMatch in rowRegex.findAll(tableInner)) {
            val cells = ArrayList<String>()
            val cellRegex = Regex("""<w:tc\b[^>]*>([\s\S]*?)</w:tc>""", RegexOption.IGNORE_CASE)
            for (cellMatch in cellRegex.findAll(rowMatch.groupValues[1])) {
                // A cell may hold several paragraphs; join them with a newline.
                val paragraphs = Regex("""<w:p\b[^>]*>[\s\S]*?</w:p>""", RegexOption.IGNORE_CASE)
                    .findAll(cellMatch.groupValues[1])
                    .map { paragraphText(it.value) }
                    .toList()
                val cellText = if (paragraphs.isEmpty()) runText(cellMatch.groupValues[1])
                else paragraphs.joinToString("\n")
                cells.add(cellText)
            }
            rows.add(cells)
        }
        return Table(rows)
    }

    /** Joins every `w:t` run inside a single paragraph, honouring tabs/breaks. */
    private fun paragraphText(paragraphXml: String): String = runText(paragraphXml)

    private fun runText(xml: String): String {
        val sb = StringBuilder()
        // Scan runs and break/tab markers in document order.
        val token = Regex(
            """<w:t\b[^>]*>([\s\S]*?)</w:t>|<w:tab\b[^>]*/?>|<w:br\b[^>]*/?>|<w:cr\b[^>]*/?>""",
            RegexOption.IGNORE_CASE,
        )
        for (m in token.findAll(xml)) {
            val textGroup = m.groupValues[1]
            when {
                m.value.startsWith("<w:tab", ignoreCase = true) -> sb.append('\t')
                m.value.startsWith("<w:br", ignoreCase = true) -> sb.append('\n')
                m.value.startsWith("<w:cr", ignoreCase = true) -> sb.append('\n')
                else -> sb.append(OoxmlZip.decodeEntities(textGroup))
            }
        }
        return sb.toString()
    }
}
