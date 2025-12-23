package com.example.expencetrackerapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================================
// 🎨 DARK COLOR SCHEME - Deep, Rich, Premium
// ============================================================================
private val DarkColorScheme =
        darkColorScheme(
                primary = Primary,
                onPrimary = Color.White,
                primaryContainer = PrimaryDark,
                onPrimaryContainer = PrimaryLight,
                secondary = Secondary,
                onSecondary = Color.White,
                secondaryContainer = SecondaryDark,
                onSecondaryContainer = SecondaryLight,
                tertiary = Accent,
                onTertiary = Color.White,
                tertiaryContainer = AccentDark,
                onTertiaryContainer = AccentLight,
                background = DarkBackground,
                onBackground = TextPrimary,
                surface = DarkSurface,
                onSurface = TextPrimary,
                surfaceVariant = DarkSurfaceVariant,
                onSurfaceVariant = TextSecondary,
                surfaceTint = Primary.copy(alpha = 0.1f),
                error = Error,
                onError = Color.White,
                errorContainer = Error.copy(alpha = 0.2f),
                onErrorContainer = Error,
                outline = DarkSurfaceVariant,
                outlineVariant = TextTertiary,
                scrim = Color.Black.copy(alpha = 0.5f),
                inverseSurface = TextPrimary,
                inverseOnSurface = DarkBackground,
                inversePrimary = PrimaryDark
        )

// ============================================================================
// ☀️ LIGHT COLOR SCHEME - Clean, Bright, Modern
// ============================================================================
private val LightColorScheme =
        lightColorScheme(
                primary = Primary,
                onPrimary = Color.White,
                primaryContainer = PrimaryLight.copy(alpha = 0.2f),
                onPrimaryContainer = PrimaryDark,
                secondary = Secondary,
                onSecondary = Color.White,
                secondaryContainer = SecondaryLight.copy(alpha = 0.2f),
                onSecondaryContainer = SecondaryDark,
                tertiary = Accent,
                onTertiary = Color.White,
                tertiaryContainer = AccentLight.copy(alpha = 0.2f),
                onTertiaryContainer = AccentDark,
                background = LightBackground,
                onBackground = TextPrimaryLight,
                surface = LightSurface,
                onSurface = TextPrimaryLight,
                surfaceVariant = LightSurfaceVariant,
                onSurfaceVariant = TextSecondaryLight,
                surfaceTint = Primary.copy(alpha = 0.05f),
                error = Error,
                onError = Color.White,
                errorContainer = Error.copy(alpha = 0.1f),
                onErrorContainer = Error,
                outline = LightSurfaceVariant,
                outlineVariant = TextTertiaryLight,
                scrim = Color.Black.copy(alpha = 0.3f),
                inverseSurface = TextPrimaryLight,
                inverseOnSurface = LightBackground,
                inversePrimary = PrimaryLight
        )

@Composable
fun ExpenceTrackerAppTheme(
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        // Dynamic color is available on Android 12+
        dynamicColor: Boolean = false, // Disabled to use our custom theme
        content: @Composable () -> Unit
) {
    // Determine if dark theme should be used
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme =
            when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemInDarkTheme
            }

    val colorScheme =
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)
                }
                darkTheme -> DarkColorScheme
                else -> LightColorScheme
            }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status bar transparent for immersive experience
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            // Set status bar icons to dark/light based on theme
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
