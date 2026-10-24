package com.offlineviewer.app.data

import android.content.Context
import android.content.SharedPreferences
import com.offlineviewer.app.data.RecentFilesLogic.Entry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the list of recently opened files locally via [SharedPreferences],
 * mirroring [ThemePreferences]. Fully offline: it touches only on-device
 * storage, requires no permission, and makes no network calls.
 *
 * Only files whose SAF read permission was persisted should be recorded here
 * (see [MainActivity]); otherwise a remembered entry could not be reopened.
 * Entries that can no longer be opened are pruned via [remove].
 *
 * Each entry is stored as one `uri\tname` line in a single newline-separated
 * string, which avoids pulling in any JSON runtime dependency. Tabs and
 * newlines in the display name are collapsed to spaces so the encoding stays
 * unambiguous.
 */
class RecentFilesPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _recent = MutableStateFlow(load())

    /** The remembered files, most-recent-first, observable for Compose. */
    val recent: StateFlow<List<Entry>> = _recent.asStateFlow()

    /**
     * Records [uri] / [displayName] as the most recent file, de-duplicated and
     * bounded by [RecentFilesLogic].
     */
    fun add(uri: String, displayName: String) {
        val entry = Entry(uri, displayName.ifBlank { uri })
        update(RecentFilesLogic.add(_recent.value, entry))
    }

    /** Drops the entry for [uri] (e.g. when reopening it failed). */
    fun remove(uri: String) {
        update(RecentFilesLogic.remove(_recent.value, uri))
    }

    private fun update(list: List<Entry>) {
        _recent.value = list
        prefs.edit().putString(KEY_RECENT, encode(list)).apply()
    }

    private fun load(): List<Entry> = decode(prefs.getString(KEY_RECENT, null))

    private fun encode(list: List<Entry>): String =
        list.joinToString("\n") { "${it.uri}\t${sanitize(it.displayName)}" }

    private fun decode(raw: String?): List<Entry> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split('\n').mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val tab = line.indexOf('\t')
            if (tab <= 0) return@mapNotNull null
            val uri = line.substring(0, tab)
            val name = line.substring(tab + 1)
            if (uri.isBlank()) null else Entry(uri, name.ifBlank { uri })
        }
    }

    /** Collapses the field separators so the single-string encoding is safe. */
    private fun sanitize(name: String): String =
        name.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')

    companion object {
        private const val PREFS_NAME = "offline_viewer_prefs"
        private const val KEY_RECENT = "recent_files"
    }
}
