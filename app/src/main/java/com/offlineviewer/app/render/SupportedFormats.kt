package com.offlineviewer.app.render

/**
 * Central, framework-free catalogue of the file formats the app advertises and
 * understands. Kept in one place so the renderers, the MIME/extension
 * resolution logic, the home-screen format list, and the unit tests all agree
 * on the same curated set. The AndroidManifest intent-filters mirror this set.
 */
object SupportedFormats {

    /** A single advertised format grouping. */
    data class Format(
        val label: String,
        val extensions: List<String>,
        val mimeTypes: List<String>,
    )

    val all: List<Format> = listOf(
        Format("SVG image", listOf("svg"), listOf("image/svg+xml")),
        Format("Markdown", listOf("md", "markdown"), listOf("text/markdown", "text/x-markdown")),
        Format("CSV", listOf("csv"), listOf("text/csv")),
        Format("TSV", listOf("tsv"), listOf("text/tab-separated-values")),
        Format("JSON", listOf("json"), listOf("application/json", "text/json")),
        Format("XML", listOf("xml"), listOf("application/xml", "text/xml")),
        Format("YAML", listOf("yaml", "yml"), listOf("text/yaml", "application/x-yaml", "application/yaml")),
        Format("Plain text / log", listOf("txt", "log"), listOf("text/plain")),
        Format("HTML", listOf("html", "htm"), listOf("text/html")),
        Format("Rich Text (RTF)", listOf("rtf"), listOf("application/rtf", "text/rtf")),
        Format("EPUB e-book", listOf("epub"), listOf("application/epub+zip")),
        Format(
            "Source code",
            listOf("kt", "java", "js", "ts", "py", "c", "cpp", "h", "cs", "go", "rb", "rs", "sh", "sql", "css"),
            listOf("text/x-java", "text/x-kotlin", "text/x-python", "application/javascript"),
        ),
    )

    /** Union of every advertised extension (lower-cased, no dot). */
    val allExtensions: Set<String> = all.flatMap { it.extensions }.toSet()

    /** Union of every advertised MIME type (lower-cased). */
    val allMimeTypes: Set<String> = all.flatMap { it.mimeTypes }.toSet()

    /**
     * Extensions that are safe to always decode as UTF-8 text. Binary-ish
     * container formats (epub) are excluded so they are read as raw bytes.
     */
    val textExtensions: Set<String> = setOf(
        "svg", "md", "markdown", "csv", "tsv", "json", "xml",
        "yaml", "yml", "txt", "log", "html", "htm", "rtf",
        "kt", "java", "js", "ts", "py", "c", "cpp", "h", "cs",
        "go", "rb", "rs", "sh", "sql", "css",
    )
}
