package com.offlineviewer.app.render.logic

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Pretty-prints JSON using the built-in org.json parser. Pure and testable.
 *
 * The org.json classes ship with the Android runtime, but the same API is
 * available on the JVM via the test dependency, so the formatting logic is
 * fully unit-testable.
 */
object JsonFormatter {

    /** Outcome of a formatting attempt. */
    sealed interface Result {
        data class Formatted(val text: String) : Result
        data class Invalid(val reason: String) : Result
    }

    private const val INDENT = 2

    /**
     * Attempts to parse and pretty-print [input]. On any parse error returns
     * [Result.Invalid] so the renderer can fall back to showing the raw text.
     */
    fun format(input: String): Result {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Result.Invalid("Empty document")
        return try {
            val token = JSONTokener(trimmed).nextValue()
            val pretty = when (token) {
                is JSONObject -> token.toString(INDENT)
                is JSONArray -> token.toString(INDENT)
                else -> return Result.Invalid("Not a JSON object or array")
            }
            Result.Formatted(pretty)
        } catch (e: Exception) {
            Result.Invalid(e.message ?: "Malformed JSON")
        }
    }
}
