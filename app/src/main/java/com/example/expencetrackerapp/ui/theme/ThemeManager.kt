package com.example.expencetrackerapp.ui.theme

import android.content.Context
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
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_BACKGROUND = "background"
    private const val KEY_FPS_METER = "fps_meter"

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    var currentBackground by mutableStateOf(AppBackgroundTheme.NEON_CYBERPUNK)
        private set

    var isFpsMeterEnabled by mutableStateOf(false)
        private set

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load Theme Mode
        val savedThemeMode = prefs?.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        themeMode =
                try {
                    ThemeMode.valueOf(savedThemeMode ?: ThemeMode.SYSTEM.name)
                } catch (e: IllegalArgumentException) {
                    ThemeMode.SYSTEM
                }

        // Load Background
        val savedBackground =
                prefs?.getString(KEY_BACKGROUND, AppBackgroundTheme.NEON_CYBERPUNK.name)
        currentBackground =
                try {
                    AppBackgroundTheme.valueOf(
                            savedBackground ?: AppBackgroundTheme.NEON_CYBERPUNK.name
                    )
                } catch (e: IllegalArgumentException) {
                    AppBackgroundTheme.NEON_CYBERPUNK
                }

        // Load FPS Meter setting
        isFpsMeterEnabled = prefs?.getBoolean(KEY_FPS_METER, false) ?: false
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }

    fun updateBackground(background: AppBackgroundTheme) {
        currentBackground = background
        prefs?.edit()?.putString(KEY_BACKGROUND, background.name)?.apply()
    }

    fun updateFpsMeter(enabled: Boolean) {
        isFpsMeterEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_FPS_METER, enabled)?.apply()
    }
}

enum class AppBackgroundTheme(val drawableRes: Int, val title: String) {
    GEOMETRIC(com.example.expencetrackerapp.R.drawable.app_background, "Geometric Glass"),
    GEOMETRIC_ABSTRACT(com.example.expencetrackerapp.R.drawable.bg_geometric_shapes, "Bauhaus"),
    NEON_CYBERPUNK(com.example.expencetrackerapp.R.drawable.bg_neon_cyberpunk, "Neon City"),
    SOFT_WATERCOLOR(com.example.expencetrackerapp.R.drawable.bg_soft_watercolor, "Watercolor"),
    DEEP_SPACE(com.example.expencetrackerapp.R.drawable.bg_deep_space, "Deep Space"),
    PAPER_LAYERS(com.example.expencetrackerapp.R.drawable.bg_paper_layers, "Paper Layers"),
    GRADIENT_MESH(com.example.expencetrackerapp.R.drawable.bg_gradient_mesh, "Gradient Mesh"),
    RETRO_WAVE(com.example.expencetrackerapp.R.drawable.bg_retro_wave, "Retro Wave"),
    DARK_SLATE(com.example.expencetrackerapp.R.drawable.bg_dark_slate, "Dark Slate"),
    PIXEL_PATTERN(com.example.expencetrackerapp.R.drawable.bg_pixel_pattern, "Pixel Pattern"),
    LIQUID_SYMPHONY(0, "Liquid Symphony") // Special case: uses BackgroundPattern composable
}
