package com.offlineviewer.app.data

import com.offlineviewer.app.data.RecentFilesLogic.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for [RecentFilesLogic]: the de-duplication / bounding / removal
 * rules behind the recent-files store. No Android framework is involved.
 */
class RecentFilesLogicTest {

    private fun entry(uri: String, name: String = uri) = Entry(uri, name)

    @Test
    fun addPrependsMostRecent() {
        val result = RecentFilesLogic.add(listOf(entry("a"), entry("b")), entry("c"))
        assertEquals(listOf("c", "a", "b"), result.map { it.uri })
    }

    @Test
    fun addDeduplicatesByUriKeepingFreshest() {
        val current = listOf(entry("a", "old name"), entry("b"))
        val result = RecentFilesLogic.add(current, entry("a", "new name"))
        assertEquals(listOf("a", "b"), result.map { it.uri })
        assertEquals("new name", result.first().displayName)
    }

    @Test
    fun addEnforcesMaxBound() {
        var list = emptyList<Entry>()
        for (i in 1..15) {
            list = RecentFilesLogic.add(list, entry("uri$i"), max = 10)
        }
        assertEquals(10, list.size)
        // Most recent first; oldest five dropped.
        assertEquals("uri15", list.first().uri)
        assertEquals("uri6", list.last().uri)
    }

    @Test
    fun addIgnoresBlankUri() {
        val current = listOf(entry("a"))
        assertEquals(current, RecentFilesLogic.add(current, entry("   ", "x")))
    }

    @Test
    fun removeDropsMatchingUri() {
        val current = listOf(entry("a"), entry("b"), entry("c"))
        assertEquals(listOf("a", "c"), RecentFilesLogic.remove(current, "b").map { it.uri })
    }

    @Test
    fun removeOfMissingUriIsNoOp() {
        val current = listOf(entry("a"))
        assertEquals(current, RecentFilesLogic.remove(current, "zzz"))
    }

    @Test
    fun maxOfZeroIsClampedToOne() {
        val result = RecentFilesLogic.add(emptyList(), entry("a"), max = 0)
        assertTrue(result.size == 1)
    }
}
