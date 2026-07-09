package com.example.airvibe.feature.radar.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.theme.AirVibeTheme

/**
 * Canvas con anillos concéntricos, líneas radiales y un barrido
 * continuo que evoca la animación clásica de un radar.
 *
 * El barrido se renderiza con un gradiente cónico para conseguir el
 * efecto "trail" característico de un radar real.
 */
@Composable
fun RadarSweep(
    modifier: Modifier = Modifier,
    rings: Int = 4,
    spokes: Int = 8,
    sweepColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = AirVibeTheme.glass.outerBorder,
    sweepDurationMillis: Int = 4200,
) {
    val transition = rememberInfiniteTransition(label = "radarSweep")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = sweepDurationMillis, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweepRotation",
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = (minOf(size.width, size.height) / 2f) * 0.92f

            // Anillos concéntricos
            for (i in 1..rings) {
                drawCircle(
                    color = gridColor,
                    radius = maxRadius * (i / rings.toFloat()),
                    center = center,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f),
                    ),
                )
            }

            // Radios
            for (i in 0 until spokes) {
                val angle = (Math.PI * 2 * i) / spokes
                val endX = center.x + (kotlin.math.cos(angle) * maxRadius).toFloat()
                val endY = center.y + (kotlin.math.sin(angle) * maxRadius).toFloat()
                drawLine(
                    color = gridColor,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f),
                )
            }

            // Sweep con gradiente cónico
            rotate(degrees = rotation, pivot = center) {
                val sweepBrush = Brush.sweepGradient(
                    colors = listOf(
                        sweepColor.copy(alpha = 0f),
                        sweepColor.copy(alpha = 0.18f),
                        sweepColor.copy(alpha = 0.0f),
                    ),
                    center = center,
                )
                drawCircle(
                    brush = sweepBrush,
                    radius = maxRadius,
                    center = center,
                )
            }

            // Núcleo del radar
            drawCircle(
                color = sweepColor.copy(alpha = 0.35f),
                radius = maxRadius * 0.04f,
                center = center,
            )
            drawCircle(
                color = sweepColor,
                radius = maxRadius * 0.018f,
                center = center,
            )
        }
    }
}
