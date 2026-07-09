package com.example.airvibe.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.modifiers.glassBlur
import com.example.airvibe.core.designsystem.modifiers.glassHighlightBorder
import com.example.airvibe.core.designsystem.modifiers.glassShadow
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.core.designsystem.theme.LocalGlassTokens

/**
 * Superficie con efecto liquid glass: relleno translúcido,
 * highlight superior y sombra difusa.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    tint: Color = Color.Unspecified,
    useBlur: Boolean = true,
    content: @Composable () -> Unit,
) {
    val tokens = AirVibeTheme.glass
    val resolvedTint = if (tint == Color.Unspecified) tokens.surfaceFill else tint
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .glassShadow(
                color = tokens.shadowColor,
                cornerRadius = cornerRadius,
            )
            .glassBlur(radius = 20.dp, shape = shape, enabled = useBlur)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tokens.highlightTop.copy(alpha = 0.45f),
                        resolvedTint,
                        tokens.surfaceFillSubtle,
                    ),
                ),
            )
            .glassHighlightBorder(
                color = tokens.outerBorder,
                cornerRadius = cornerRadius,
            )
            .drawBehind {
                // Borde interno brillante en el borde superior
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            tokens.innerBorder.copy(alpha = 0.55f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = size.height * 0.5f,
                    ),
                )
            }
            .padding(contentPadding),
    ) {
        content()
    }
}
