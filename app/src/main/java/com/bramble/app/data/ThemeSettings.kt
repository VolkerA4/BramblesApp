package com.bramble.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The user's dark mode choice. null = never chosen, so the app follows the phone's setting.
 * Held in Compose state so flipping the switch in Settings recolors the app right away.
 */
object ThemeSettings {
    private const val PREFS = "settings"
    private const val KEY_DARK_MODE = "dark_mode"

    var darkMode by mutableStateOf<Boolean?>(null)
        private set

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        darkMode = if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false) else null
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        darkMode = enabled
    }
}
