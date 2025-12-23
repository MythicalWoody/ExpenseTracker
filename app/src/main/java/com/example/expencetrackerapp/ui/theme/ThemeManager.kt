package com.example.expencetrackerapp.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Theme mode options */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/** Observable theme state for the application */
object ThemeState {
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
    }
}
