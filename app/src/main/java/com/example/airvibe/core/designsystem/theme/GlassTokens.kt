package com.example.airvibe.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tokens semánticos para superficies con efecto "liquid glass".
 *
 * Estos tokens encapsulan los valores que cambian entre tema claro y oscuro
 * para que los componentes no tengan que conocer la paleta de origen.
 */
@Immutable
data class GlassTokens(
    val surfaceFill: Color,
    val surfaceFillStrong: Color,
    val surfaceFillSubtle: Color,
    val highlightTop: Color,
    val highlightBottom: Color,
    val innerBorder: Color,
    val outerBorder: Color,
    val shadowColor: Color,
    val tint: Color,
    val blurRadius: Dp = 24.dp,
)

internal val LightGlassTokens = GlassTokens(
    surfaceFill = Color(0x66FFFFFF),
    surfaceFillStrong = Color(0x99FFFFFF),
    surfaceFillSubtle = Color(0x33FFFFFF),
    highlightTop = Color(0xFFFFFFFF),
    highlightBottom = Color(0x14FFFFFF),
    innerBorder = Color(0xFFFFFFFF),
    outerBorder = Color(0x1A0F172A),
    shadowColor = Color(0x140F172A),
    tint = Color(0x08000000),
)

internal val DarkGlassTokens = GlassTokens(
    surfaceFill = Color(0x14FFFFFF),
    surfaceFillStrong = Color(0x29FFFFFF),
    surfaceFillSubtle = Color(0x0AFFFFFF),
    highlightTop = Color(0x3DFFFFFF),
    highlightBottom = Color(0x0FFFFFFF),
    innerBorder = Color(0x33FFFFFF),
    outerBorder = Color(0x0FFFFFFF),
    shadowColor = Color(0x66000000),
    tint = Color(0x10FFFFFF),
)

val LocalGlassTokens = compositionLocalOf { LightGlassTokens }

object AirVibeTheme {
    val glass: GlassTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassTokens.current
}
