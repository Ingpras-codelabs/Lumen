package com.offlineviewer.app.render.logic

/**
 * Detects a source-code "language" from a file extension and supplies the
 * keyword set used by the (regex-based) syntax highlighter. Pure and testable.
 */
enum class CodeLanguage(
    val displayName: String,
    val keywords: Set<String>,
    val lineComment: String?,
) {
    KOTLIN("Kotlin", setOf("fun", "val", "var", "class", "object", "interface", "when", "is", "in", "return", "if", "else", "for", "while", "import", "package", "private", "public", "internal", "override", "suspend", "data", "sealed", "companion", "null", "true", "false"), "//"),
    JAVA("Java", setOf("public", "private", "protected", "class", "interface", "void", "int", "long", "double", "float", "boolean", "return", "if", "else", "for", "while", "new", "static", "final", "import", "package", "extends", "implements", "null", "true", "false", "this"), "//"),
    JAVASCRIPT("JavaScript", setOf("function", "var", "let", "const", "return", "if", "else", "for", "while", "class", "new", "import", "export", "from", "async", "await", "null", "undefined", "true", "false", "this"), "//"),
    TYPESCRIPT("TypeScript", setOf("function", "var", "let", "const", "return", "if", "else", "for", "while", "class", "interface", "type", "new", "import", "export", "from", "async", "await", "null", "undefined", "true", "false", "this", "number", "string", "boolean"), "//"),
    PYTHON("Python", setOf("def", "class", "return", "if", "elif", "else", "for", "while", "import", "from", "as", "with", "try", "except", "finally", "lambda", "None", "True", "False", "and", "or", "not", "in", "is", "pass", "yield"), "#"),
    C("C", setOf("int", "char", "float", "double", "void", "return", "if", "else", "for", "while", "struct", "typedef", "const", "static", "sizeof", "include", "define", "NULL"), "//"),
    CPP("C++", setOf("int", "char", "float", "double", "void", "return", "if", "else", "for", "while", "class", "struct", "template", "typename", "namespace", "const", "static", "public", "private", "protected", "new", "delete", "nullptr", "true", "false", "auto"), "//"),
    CSHARP("C#", setOf("using", "namespace", "class", "public", "private", "protected", "void", "int", "string", "bool", "var", "return", "if", "else", "for", "while", "foreach", "new", "static", "null", "true", "false", "this"), "//"),
    GO("Go", setOf("func", "var", "const", "package", "import", "return", "if", "else", "for", "range", "struct", "interface", "type", "go", "chan", "defer", "nil", "true", "false", "map"), "//"),
    RUST("Rust", setOf("fn", "let", "mut", "const", "struct", "enum", "impl", "trait", "return", "if", "else", "for", "while", "loop", "match", "use", "mod", "pub", "self", "true", "false", "None", "Some"), "//"),
    RUBY("Ruby", setOf("def", "class", "module", "return", "if", "elsif", "else", "unless", "while", "do", "end", "require", "yield", "nil", "true", "false", "self", "attr_accessor"), "#"),
    PHP("PHP", setOf("function", "class", "public", "private", "protected", "return", "if", "else", "elseif", "for", "foreach", "while", "echo", "new", "static", "null", "true", "false", "use", "namespace"), "//"),
    SWIFT("Swift", setOf("func", "let", "var", "class", "struct", "enum", "return", "if", "else", "for", "while", "guard", "import", "public", "private", "static", "nil", "true", "false", "self"), "//"),
    SHELL("Shell", setOf("if", "then", "else", "elif", "fi", "for", "do", "done", "while", "case", "esac", "function", "echo", "export", "return", "local"), "#"),
    SQL("SQL", setOf("select", "from", "where", "insert", "into", "values", "update", "set", "delete", "create", "table", "drop", "alter", "join", "left", "right", "inner", "outer", "on", "group", "by", "order", "having", "and", "or", "not", "null", "as"), "--"),
    CSS("CSS", setOf("color", "background", "margin", "padding", "border", "display", "position", "font", "width", "height", "flex", "grid"), null),
    TOML("TOML", setOf("true", "false"), "#"),
    INI("INI", emptySet(), ";"),
    PROPERTIES("Properties", emptySet(), "#"),
    PLAIN("Code", emptySet(), null);

    companion object {
        private val BY_EXTENSION: Map<String, CodeLanguage> = mapOf(
            "kt" to KOTLIN, "kts" to KOTLIN,
            "java" to JAVA,
            "js" to JAVASCRIPT, "mjs" to JAVASCRIPT, "jsx" to JAVASCRIPT,
            "ts" to TYPESCRIPT, "tsx" to TYPESCRIPT,
            "py" to PYTHON,
            "c" to C, "h" to C,
            "cpp" to CPP, "cc" to CPP, "cxx" to CPP, "hpp" to CPP,
            "cs" to CSHARP,
            "go" to GO,
            "rs" to RUST,
            "rb" to RUBY,
            "php" to PHP,
            "swift" to SWIFT,
            "sh" to SHELL, "bash" to SHELL,
            "sql" to SQL,
            "css" to CSS,
            "toml" to TOML,
            "ini" to INI, "cfg" to INI,
            "properties" to PROPERTIES,
        )

        /** All source-code extensions this renderer claims. */
        val allExtensions: Set<String> get() = BY_EXTENSION.keys

        /** Returns the detected language for [extension], or [PLAIN] when unknown. */
        fun fromExtension(extension: String?): CodeLanguage {
            val ext = extension?.trim()?.lowercase()?.removePrefix(".") ?: return PLAIN
            return BY_EXTENSION[ext] ?: PLAIN
        }
    }
}
