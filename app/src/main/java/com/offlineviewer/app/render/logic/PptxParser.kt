package com.offlineviewer.app.render.logic

/**
 * A minimal, dependency-free reader for PowerPoint OOXML (.pptx) built on
 * [java.util.zip]. Each slide is a part `ppt/slides/slideN.xml`; this parser
 * enumerates those in numeric slide order and extracts the `a:t` text runs of
 * each slide into an ordered list of paragraph lines.
 *
 * Parsing is tolerant substring/regex scanning and defensive: a malformed or
 * empty archive yields an empty [Deck] rather than throwing.
 */
object PptxParser {

    /** A single slide's extracted text lines, in reading order. */
    data class Slide(val lines: List<String>)
    data class Deck(val slides: List<Slide>) {
        val isEmpty: Boolean get() = slides.none { slide -> slide.lines.any { it.isNotEmpty() } }
    }

    private val SLIDE_PART = Regex("""ppt/slides/slide(\d+)\.xml""", RegexOption.IGNORE_CASE)

    fun parse(bytes: ByteArray): Deck {
        return try {
            val entries = OoxmlZip.readEntries(bytes)
            val slideParts = entries.keys
                .mapNotNull { name -> SLIDE_PART.matchEntire(name)?.let { it to name } }
                .sortedBy { it.first.groupValues[1].toIntOrNull() ?: Int.MAX_VALUE }
                .map { it.second }
            val slides = slideParts.mapNotNull { part ->
                val xml = entries[part]?.toString(Charsets.UTF_8) ?: return@mapNotNull null
                Slide(extractLines(xml))
            }
            Deck(slides)
        } catch (e: Throwable) {
            Deck(emptyList())
        }
    }

    /**
     * One line per `a:p` paragraph, joining its `a:t` runs. Falls back to all
     * `a:t` runs on the slide when no paragraph structure is present.
     */
    private fun extractLines(xml: String): List<String> {
        val lines = ArrayList<String>()
        val paragraphs = Regex("""<a:p\b[^>]*>([\s\S]*?)</a:p>""", RegexOption.IGNORE_CASE)
            .findAll(xml).toList()
        val source = if (paragraphs.isNotEmpty()) paragraphs.map { it.groupValues[1] } else listOf(xml)
        for (block in source) {
            val sb = StringBuilder()
            for (t in Regex("""<a:t\b[^>]*>([\s\S]*?)</a:t>""", RegexOption.IGNORE_CASE).findAll(block)) {
                sb.append(OoxmlZip.decodeEntities(t.groupValues[1]))
            }
            val text = sb.toString()
            if (text.isNotEmpty()) lines.add(text)
        }
        return lines
    }
}
