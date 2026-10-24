package com.offlineviewer.app.data

import android.content.Context
import android.content.SharedPreferences
import com.offlineviewer.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the in-app theme choice locally via [SharedPreferences]. This is
 * fully offline: it touches only on-device storage, requires no permission,
 * and makes no network calls.
 */
class ThemePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(load())
    /** The currently selected theme, observable for Compose. */
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private fun load(): ThemeMode = ThemeMode.fromName(prefs.getString(KEY_THEME_MODE, null))

    /** Persist and publish a new [mode]. */
    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    companion object {
        private const val PREFS_NAME = "offline_viewer_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
