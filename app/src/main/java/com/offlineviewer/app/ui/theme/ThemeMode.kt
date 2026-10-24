package com.offlineviewer.app.ui.theme

/**
 * The user-selectable theme preference. [SYSTEM] follows the device's
 * light/dark setting; [LIGHT] and [DARK] force a scheme regardless of system.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /** Parse a persisted name back to a [ThemeMode], defaulting to [SYSTEM]. */
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
