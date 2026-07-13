package com.example.airvibe.core.designsystem.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat

/**
 * Tema principal de AirVibe. Diseñado siguiendo la línea visual de shadcn/ui + Apple
 * con foco en tipografía limpia, neutros suaves y acentos vibrantes.
 *
 * Por defecto se utiliza el tema claro, independientemente del modo del sistema,
 * para conservar una identidad visual consistente. Si se desea respetar el modo
 * del sistema, basta con pasar `darkTheme = isSystemInDarkTheme()` al llamar al theme.
 */
@Composable
fun AirVibeTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) AirVibeDarkColors else AirVibeLightColors
    val glassTokens = if (darkTheme) DarkGlassTokens else LightGlassTokens

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !darkTheme
            insets.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalGlassTokens provides glassTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AirVibeTypography,
            shapes = AirVibeShapes,
            content = content,
        )
    }
}

internal val AirVibeLightColors = lightColorScheme(
    primary = Color(0xFF1f4bdb),
    onPrimary = Color(0xFFffffff),
    primaryContainer = Color(0xFF4166f5),
    onPrimaryContainer = Color(0xFFfcf9ff),

    secondary = Color(0xFF8c5000),
    onSecondary = Color(0xFFffffff),
    secondaryContainer = Color(0xFFffa84e),
    onSecondaryContainer = Color(0xFF703f00),

    tertiary = Color(0xFF06637d),
    onTertiary = Color(0xFFffffff),
    tertiaryContainer = Color(0xFF317c96),
    onTertiaryContainer = Color(0xFFf6fbff),

    background = Color(0xFFffffff), // As requested in login_white_theme
    onBackground = Color(0xFF1a1c1c),

    surface = Color(0xFFf9f9f9),
    onSurface = Color(0xFF1a1c1c),
    surfaceVariant = Color(0xFFe2e2e2),
    onSurfaceVariant = Color(0xFF444655),
    surfaceTint = Color(0xFF254fdf),

    outline = Color(0xFF747686),
    outlineVariant = Color(0xFFc4c5d7),
    error = Color(0xFFba1a1a),
    onError = Color(0xFFffffff),
)

internal val AirVibeDarkColors = darkColorScheme(
    primary = Color(0xFFb8c3ff),
    onPrimary = Color(0xFF001355),
    primaryContainer = Color(0xFF0036bc),
    onPrimaryContainer = Color(0xFFdde1ff),

    secondary = Color(0xFFffb873),
    onSecondary = Color(0xFF2d1600),
    secondaryContainer = Color(0xFF6a3b00),
    onSecondaryContainer = Color(0xFFffdcbf),

    tertiary = Color(0xFF8ad0ed),
    onTertiary = Color(0xFF001f29),
    tertiaryContainer = Color(0xFF004d62),
    onTertiaryContainer = Color(0xFFbaeaff),

    background = Color(0xFF1a1c1c),
    onBackground = Color(0xFFf9f9f9),

    surface = Color(0xFF2f3131),
    onSurface = Color(0xFFf0f1f1),
    surfaceVariant = Color(0xFF444655),
    onSurfaceVariant = Color(0xFFc4c5d7),
    surfaceTint = Color(0xFFb8c3ff),

    outline = Color(0xFF8e90a0),
    outlineVariant = Color(0xFF444655),
    error = Color(0xFFffdad6),
    onError = Color(0xFF93000a),
)
