package com.offlineviewer.app.render.logic

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * A minimal, dependency-free EPUB reader built on [java.util.zip].
 *
 * It reads the container.xml to locate the OPF package document, parses the
 * manifest + spine to determine the ordered list of chapter resources, and
 * exposes their decoded XHTML. All parsing is defensive: a malformed archive
 * yields an empty chapter list rather than throwing, so the renderer can show a
 * clear error state.
 *
 * The XML parsing here is tolerant substring/regex scanning (not a validating
 * parser) which keeps it pure and robust.
 */
object EpubParser {

    data class Chapter(val href: String, val html: String)
    data class Book(val chapters: List<Chapter>)

    private val CONTAINER = "META-INF/container.xml"

    fun parse(bytes: ByteArray): Book {
        return try {
            val entries = readAllEntries(bytes)
            val opfPath = findOpfPath(entries[CONTAINER]) ?: return Book(emptyList())
            val opf = entries[opfPath]?.toString(Charsets.UTF_8) ?: return Book(emptyList())
            val basePath = opfPath.substringBeforeLast('/', "")

            val manifest = parseManifest(opf) // id -> href
            val spine = parseSpine(opf)        // ordered idrefs

            val chapters = spine.mapNotNull { idref ->
                val href = manifest[idref] ?: return@mapNotNull null
                val full = resolve(basePath, href)
                val raw = entries[full] ?: entries[href] ?: return@mapNotNull null
                Chapter(href, raw.toString(Charsets.UTF_8))
            }
            Book(chapters)
        } catch (e: Throwable) {
            Book(emptyList())
        }
    }

    private fun readAllEntries(bytes: ByteArray): Map<String, ByteArray> {
        val out = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    out[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return out
    }

    private fun findOpfPath(container: ByteArray?): String? {
        val xml = container?.toString(Charsets.UTF_8) ?: return null
        val m = Regex("""full-path\s*=\s*["']([^"']+)["']""").find(xml)
        return m?.groupValues?.get(1)
    }

    private fun parseManifest(opf: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        val itemRegex = Regex("""<item\b[^>]*>""", RegexOption.IGNORE_CASE)
        for (tag in itemRegex.findAll(opf)) {
            val id = attr(tag.value, "id") ?: continue
            val href = attr(tag.value, "href") ?: continue
            map[id] = decodeEntities(href)
        }
        return map
    }

    private fun parseSpine(opf: String): List<String> {
        val spineBlock = Regex("""<spine\b[^>]*>([\s\S]*?)</spine>""", RegexOption.IGNORE_CASE)
            .find(opf)?.groupValues?.get(1) ?: return emptyList()
        return Regex("""<itemref\b[^>]*>""", RegexOption.IGNORE_CASE)
            .findAll(spineBlock)
            .mapNotNull { attr(it.value, "idref") }
            .toList()
    }

    private fun attr(tag: String, name: String): String? =
        Regex("""\b$name\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
            .find(tag)?.groupValues?.get(1)

    private fun resolve(base: String, href: String): String {
        if (base.isEmpty()) return href
        // Handle simple ../ and ./ segments.
        val parts = ArrayDeque<String>()
        base.split('/').forEach { if (it.isNotEmpty()) parts.addLast(it) }
        for (seg in href.split('/')) {
            when (seg) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeLast()
                else -> parts.addLast(seg)
            }
        }
        return parts.joinToString("/")
    }

    private fun decodeEntities(s: String): String =
        s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
}
