package com.offlineviewer.app.render.logic

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * Shared helpers for the OOXML (Office Open XML) parsers. The .docx/.xlsx/.pptx
 * formats are ZIP archives of XML parts, so they are read with [java.util.zip]
 * exactly like [EpubParser] does. The scanning is tolerant substring/regex work
 * (not a validating parser) which keeps it pure, dependency-free and robust:
 * a malformed archive yields an empty entry map rather than throwing.
 */
internal object OoxmlZip {

    /** Reads every (non-directory) entry of a ZIP archive into a name -> bytes map. */
    fun readEntries(bytes: ByteArray): Map<String, ByteArray> {
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

    /** Decodes the common XML entities (and numeric references) used in OOXML text. */
    fun decodeEntities(input: String): String {
        if (input.indexOf('&') < 0) return input
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '&') {
                val semi = input.indexOf(';', i + 1)
                if (semi in (i + 1)..(i + 10)) {
                    val entity = input.substring(i + 1, semi)
                    val replacement = when {
                        entity == "amp" -> "&"
                        entity == "lt" -> "<"
                        entity == "gt" -> ">"
                        entity == "quot" -> "\""
                        entity == "apos" -> "'"
                        entity.startsWith("#x") || entity.startsWith("#X") ->
                            entity.substring(2).toIntOrNull(16)?.let { cp ->
                                runCatching { String(Character.toChars(cp)) }.getOrNull()
                            }
                        entity.startsWith("#") ->
                            entity.substring(1).toIntOrNull()?.let { cp ->
                                runCatching { String(Character.toChars(cp)) }.getOrNull()
                            }
                        else -> null
                    }
                    if (replacement != null) {
                        sb.append(replacement)
                        i = semi + 1
                        continue
                    }
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }
}
