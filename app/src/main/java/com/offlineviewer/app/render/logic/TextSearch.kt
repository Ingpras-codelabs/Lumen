package com.offlineviewer.app.render.logic

/**
 * Pure, framework-free in-document text search used by the text-based
 * renderers (plain text / source code / structured data). It finds every
 * occurrence of a literal query within a body of text and reports the match
 * offsets, so the Compose layer only has to highlight the ranges and scroll to
 * the active one. Keeping the matching here makes it unit-testable on the JVM.
 */
object TextSearch {

    /** A half-open match range `[start, end)` into the searched text. */
    data class Match(val start: Int, val end: Int)

    /**
     * Returns the offsets of every non-overlapping occurrence of [query] in
     * [text], in order from first to last.
     *
     * - An empty [query] (or empty [text]) yields no matches, so an empty query
     *   never "highlights everything".
     * - Matching is literal (the query is not treated as a regex) and is
     *   case-insensitive by default; pass [ignoreCase] = false for an exact
     *   case-sensitive search.
     * - Overlapping matches are not reported twice: after a match the scan
     *   resumes at its end, so searching "aa" in "aaaa" yields two matches.
     */
    fun findMatches(text: String, query: String, ignoreCase: Boolean = true): List<Match> {
        if (text.isEmpty() || query.isEmpty()) return emptyList()
        val matches = ArrayList<Match>()
        var from = 0
        while (from <= text.length - query.length) {
            val index = text.indexOf(query, startIndex = from, ignoreCase = ignoreCase)
            if (index < 0) break
            val end = index + query.length
            matches.add(Match(index, end))
            from = end
        }
        return matches
    }

    /**
     * Wraps a match index so repeated "find next" cycles stay within bounds.
     * Returns 0 when there are [no][total] matches. Used to advance the active
     * highlight.
     */
    fun wrapIndex(current: Int, total: Int): Int {
        if (total <= 0) return 0
        val mod = current % total
        return if (mod < 0) mod + total else mod
    }
}
