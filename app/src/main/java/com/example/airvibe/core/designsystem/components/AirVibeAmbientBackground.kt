package com.example.airvibe.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.theme.AirVibeTheme

/**
 * Fondo ambient estilo Apple Vision Pro / iOS 18.
 * Construye un degradado suave entre tonos del tema con
 * un toque sutilmente más cálido en la parte superior.
 */
@Composable
fun AirVibeAmbientBackground(
    modifier: Modifier = Modifier,
    topTint: Color = AirVibeTheme.glass.tint,
    bottomTint: Color = Color.Transparent,
    topAccent: Color = Color(0xFFEEF2FF),
    bottomAccent: Color = Color(0xFFF5F3FF),
    cornerRadius: Dp = 0.dp,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        lerp(topAccent, topTint, 0.3f),
                        bottomAccent,
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
            )
    ) {
        // Degradado inferior para oscurecer sutilmente
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, bottomTint),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                    ),
                )
        )
    }
}
