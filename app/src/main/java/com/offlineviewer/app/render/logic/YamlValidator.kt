package com.offlineviewer.app.render.logic

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml

/**
 * Validates / normalizes YAML using SnakeYAML. Pure and testable on the JVM
 * (SnakeYAML is a plain Java library with no Android dependency).
 *
 * The renderer shows the original text (preserving comments and formatting the
 * user wrote) when YAML is valid, and falls back to a raw-text notice when it
 * is not. [validate] exposes the parse-or-fallback decision that the unit tests
 * assert on.
 */
object YamlValidator {

    sealed interface Result {
        /** Parsed cleanly. */
        data object Valid : Result
        data class Invalid(val reason: String) : Result
    }

    fun validate(input: String): Result {
        if (input.isBlank()) return Result.Invalid("Empty document")
        return try {
            val options = LoaderOptions().apply {
                // Keep memory bounded for pathological inputs.
                isAllowDuplicateKeys = true
                maxAliasesForCollections = 100
            }
            val yaml = Yaml(options)
            // loadAll handles multi-document streams; iterate to force parsing.
            var any = false
            for (doc in yaml.loadAll(input)) {
                any = true
            }
            if (any) Result.Valid else Result.Invalid("No YAML documents found")
        } catch (e: Exception) {
            Result.Invalid(e.message?.lineSequence()?.firstOrNull() ?: "Malformed YAML")
        }
    }

    /** Convenience predicate used by tests and renderers. */
    fun isValid(input: String): Boolean = validate(input) is Result.Valid
}
