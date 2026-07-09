package com.example.airvibe.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Punto de estado (presencia). Si [pulse] es true, aplica un halo
 * animado para indicar actividad en tiempo real.
 */
@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    pulse: Boolean = false,
    ringColor: Color = Color.White,
) {
    val transition = rememberInfiniteTransition(label = "statusDot")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    val pulseScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseScale",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (pulse) {
            Canvas(modifier = Modifier.size(size)) {
                val radius = (this.size.minDimension / 2f) * pulseScale
                drawCircle(
                    color = color.copy(alpha = pulseAlpha),
                    radius = radius,
                    center = Offset(this.size.width / 2f, this.size.height / 2f),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .let {
                    it
                }
        ) {
            Canvas(modifier = Modifier.size(size)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ringColor.copy(alpha = 0.85f),
                            Color.Transparent,
                        ),
                        center = Offset(this.size.width / 2f, this.size.height / 2f),
                        radius = this.size.minDimension,
                    ),
                )
                drawCircle(color = color, radius = this.size.minDimension / 2.4f)
            }
        }
    }
}
