package com.offlineviewer.app.data

/**
 * Pure, framework-free list logic for the recent-files store. Keeping the
 * de-duplication / bounding / removal rules here (rather than inline in the
 * [android.content.SharedPreferences] wrapper) makes them unit-testable on the
 * JVM; [RecentFilesPreferences] only has to persist the result.
 */
object RecentFilesLogic {

    /** Default maximum number of remembered entries. */
    const val MAX_ENTRIES: Int = 10

    /**
     * A single remembered file: the SAF [uri] string used to reopen it and the
     * [displayName] shown to the user.
     */
    data class Entry(val uri: String, val displayName: String)

    /**
     * Returns [current] with [entry] promoted to the front, de-duplicated by
     * [Entry.uri] (an existing entry with the same uri is removed first so the
     * freshest display name wins), and capped at [max] items. Blank uris are
     * ignored and return the list unchanged.
     */
    fun add(current: List<Entry>, entry: Entry, max: Int = MAX_ENTRIES): List<Entry> {
        if (entry.uri.isBlank()) return current
        val deduped = current.filterNot { it.uri == entry.uri }
        return (listOf(entry) + deduped).take(max.coerceAtLeast(1))
    }

    /** Returns [current] without any entry whose uri equals [uri]. */
    fun remove(current: List<Entry>, uri: String): List<Entry> =
        current.filterNot { it.uri == uri }
}
