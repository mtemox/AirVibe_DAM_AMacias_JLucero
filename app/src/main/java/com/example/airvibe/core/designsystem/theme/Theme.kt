package com.example.airvibe.core.designsystem.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
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
    primary = BrandPalette.Indigo600,
    onPrimary = NeutralPalette.Slate0,
    primaryContainer = BrandPalette.Indigo100,
    onPrimaryContainer = BrandPalette.Indigo800,

    secondary = BrandPalette.Cyan500,
    onSecondary = NeutralPalette.Slate0,
    secondaryContainer = BrandPalette.Cyan300,
    onSecondaryContainer = NeutralPalette.Slate900,

    tertiary = BrandPalette.Emerald500,
    onTertiary = NeutralPalette.Slate0,
    tertiaryContainer = BrandPalette.Emerald400,
    onTertiaryContainer = NeutralPalette.Slate900,

    background = NeutralPalette.Slate50,
    onBackground = NeutralPalette.Slate900,

    surface = NeutralPalette.Slate0,
    onSurface = NeutralPalette.Slate900,
    surfaceVariant = NeutralPalette.Slate100,
    onSurfaceVariant = NeutralPalette.Slate500,
    surfaceTint = BrandPalette.Indigo500,

    outline = NeutralPalette.Slate200,
    outlineVariant = NeutralPalette.Slate150,
    error = BrandPalette.Rose500,
    onError = NeutralPalette.Slate0,
)

internal val AirVibeDarkColors = darkColorScheme(
    primary = BrandPalette.Indigo400,
    onPrimary = NeutralPalette.Slate950,
    primaryContainer = BrandPalette.Indigo800,
    onPrimaryContainer = BrandPalette.Indigo100,

    secondary = BrandPalette.Cyan400,
    onSecondary = NeutralPalette.Slate950,
    secondaryContainer = BrandPalette.Indigo900,
    onSecondaryContainer = BrandPalette.Cyan300,

    tertiary = BrandPalette.Emerald400,
    onTertiary = NeutralPalette.Slate950,
    tertiaryContainer = BrandPalette.Indigo900,
    onTertiaryContainer = BrandPalette.Emerald400,

    background = NeutralPalette.Slate950,
    onBackground = NeutralPalette.Slate50,

    surface = NeutralPalette.Slate900,
    onSurface = NeutralPalette.Slate50,
    surfaceVariant = NeutralPalette.Slate800,
    onSurfaceVariant = NeutralPalette.Slate400,
    surfaceTint = BrandPalette.Indigo400,

    outline = NeutralPalette.Slate800,
    outlineVariant = NeutralPalette.Slate850,
    error = BrandPalette.Rose400,
    onError = NeutralPalette.Slate950,
)
