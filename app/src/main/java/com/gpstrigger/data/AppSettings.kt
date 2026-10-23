package com.gpstrigger.data

import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** App-wide preferences. */
object AppSettings {
    private const val KEY_THEME = "theme"
    private lateinit var prefs: SharedPreferences

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        _themeMode.value = prefs.getString(KEY_THEME, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Also tell the system, so the splash screen and window background match the choice.
            context.getSystemService(UiModeManager::class.java).setApplicationNightMode(
                when (mode) {
                    ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                    ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                    ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
                }
            )
        }
    }
}
