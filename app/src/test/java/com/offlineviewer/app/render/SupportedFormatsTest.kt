package com.offlineviewer.app.render

import com.offlineviewer.app.render.SupportedFormats.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for [SupportedFormats]: the home-screen format search filter
 * and category grouping. Keeps the single-source-of-truth catalogue honest.
 */
class SupportedFormatsTest {

    @Test
    fun blankQueryReturnsEveryFormat() {
        assertEquals(SupportedFormats.all, SupportedFormats.filter(""))
        assertEquals(SupportedFormats.all, SupportedFormats.filter("   "))
    }

    @Test
    fun filterMatchesByLabelCaseInsensitively() {
        val result = SupportedFormats.filter("word")
        assertTrue(result.any { it.label == "Word document" })
        assertTrue(result.all { it.label.contains("word", ignoreCase = true) })
    }

    @Test
    fun filterMatchesByExtensionWithOrWithoutDot() {
        val byExt = SupportedFormats.filter("xlsx")
        val byDotExt = SupportedFormats.filter(".xlsx")
        assertEquals(byExt, byDotExt)
        assertTrue(byExt.any { "xlsx" in it.extensions })
    }

    @Test
    fun filterWithNoMatchIsEmpty() {
        assertTrue(SupportedFormats.filter("definitely-not-a-format").isEmpty())
    }

    @Test
    fun everyFormatHasACategory() {
        // All categories referenced exist in the enum (compile-time) and every
        // format is assigned one.
        assertTrue(SupportedFormats.all.all { it.category in Category.entries })
    }

    @Test
    fun groupedSkipsEmptyCategoriesAndPreservesOrder() {
        val grouped = SupportedFormats.grouped()
        // No empty groups.
        assertTrue(grouped.all { it.second.isNotEmpty() })
        // Categories appear in declaration order.
        val order = grouped.map { it.first }
        assertEquals(order, order.sortedBy { Category.entries.indexOf(it) })
        // Every advertised format is present exactly once across the groups.
        val flattened = grouped.flatMap { it.second }
        assertEquals(SupportedFormats.all.toSet(), flattened.toSet())
        assertEquals(SupportedFormats.all.size, flattened.size)
    }

    @Test
    fun groupedRespectsAFilteredSubset() {
        val subset = SupportedFormats.filter("json")
        val grouped = SupportedFormats.grouped(subset)
        assertEquals(subset.toSet(), grouped.flatMap { it.second }.toSet())
    }
}
