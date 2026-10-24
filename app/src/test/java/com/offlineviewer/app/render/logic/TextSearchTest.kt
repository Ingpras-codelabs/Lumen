package com.offlineviewer.app.render.logic

import com.offlineviewer.app.render.logic.TextSearch.Match
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for [TextSearch]: the in-document find match logic used by the
 * text-based renderers. No Compose or Android framework is involved.
 */
class TextSearchTest {

    @Test
    fun findsEveryOccurrenceInOrder() {
        val matches = TextSearch.findMatches("the cat sat on the mat", "the")
        assertEquals(listOf(Match(0, 3), Match(15, 18)), matches)
    }

    @Test
    fun isCaseInsensitiveByDefault() {
        val matches = TextSearch.findMatches("Alpha alpha ALPHA", "alpha")
        assertEquals(3, matches.size)
        assertEquals(Match(0, 5), matches[0])
        assertEquals(Match(6, 11), matches[1])
        assertEquals(Match(12, 17), matches[2])
    }

    @Test
    fun caseSensitiveSearchRespectsCase() {
        val matches = TextSearch.findMatches("Alpha alpha ALPHA", "alpha", ignoreCase = false)
        assertEquals(listOf(Match(6, 11)), matches)
    }

    @Test
    fun emptyQueryYieldsNoMatches() {
        assertTrue(TextSearch.findMatches("anything", "").isEmpty())
    }

    @Test
    fun emptyTextYieldsNoMatches() {
        assertTrue(TextSearch.findMatches("", "x").isEmpty())
    }

    @Test
    fun noMatchYieldsEmptyList() {
        assertTrue(TextSearch.findMatches("hello world", "zzz").isEmpty())
    }

    @Test
    fun matchesDoNotOverlap() {
        // "aa" in "aaaa" -> two non-overlapping matches, not three.
        assertEquals(listOf(Match(0, 2), Match(2, 4)), TextSearch.findMatches("aaaa", "aa"))
    }

    @Test
    fun wrapIndexStaysWithinBounds() {
        assertEquals(0, TextSearch.wrapIndex(3, 3))
        assertEquals(1, TextSearch.wrapIndex(4, 3))
        assertEquals(2, TextSearch.wrapIndex(-1, 3))
        assertEquals(0, TextSearch.wrapIndex(5, 0))
    }
}
