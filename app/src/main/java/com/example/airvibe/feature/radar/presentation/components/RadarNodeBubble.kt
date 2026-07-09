package com.example.airvibe.feature.radar.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.components.AvatarMonogram
import com.example.airvibe.core.designsystem.modifiers.glassBlur
import com.example.airvibe.core.designsystem.modifiers.glow
import com.example.airvibe.feature.radar.domain.model.PresenceStatus
import com.example.airvibe.feature.radar.domain.model.RadarNode
import kotlin.math.cos
import kotlin.math.sin

/**
 * Burbuja flotante que representa un nodo del radar. Se posiciona
 * mediante coordenadas polares (ángulo + distancia normalizada) sobre
 * el [Box] padre. Incluye un halo pulsante cuando la presencia está activa.
 */
@Composable
fun RadarNodeBubble(
    node: RadarNode,
    canvasSizePx: Float,
    onClick: (RadarNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isActive = node.presence != PresenceStatus.Away
    val density = LocalDensity.current

    val transition = rememberInfiniteTransition(label = "pulse-${node.id}")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (1400 / (0.6f + node.signalStrength)).toInt()),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseAlpha",
    )

    val interactionSource = remember { MutableInteractionSource() }

    val (xPx, yPx) = polarToOffset(
        angleDegrees = node.angleDegrees,
        distanceNormalized = node.distanceNormalized,
        canvasSizePx = canvasSizePx,
    )

    val bubbleSize: Dp = (44 + node.signalStrength * 14).dp
    val ringSize: Dp = bubbleSize + 18.dp

    Box(
        modifier = modifier
            .size(ringSize)
            .offset(
                x = with(density) { xPx.toDp() } - ringSize / 2,
                y = with(density) { yPx.toDp() } - ringSize / 2,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Halo / glow externo
        Box(
            modifier = Modifier
                .size(bubbleSize * 2.2f)
                .glow(
                    color = node.accentColor.copy(alpha = 0.55f),
                    radius = bubbleSize * 1.2f,
                ),
        )

        // Anillo pulsante
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(ringSize)
                    .scale(pulse)
                    .clip(CircleShape)
                    .border(
                        width = 1.4.dp,
                        color = node.accentColor.copy(alpha = 0.5f),
                        shape = CircleShape,
                    ),
            )
        }

        // Burbuja principal con efecto glass
        Box(
            modifier = Modifier
                .size(bubbleSize)
                .shadow(elevation = 14.dp, shape = CircleShape, clip = false)
                .glassBlur(radius = 18.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.75f),
                            Color.White.copy(alpha = 0.35f),
                        ),
                    ),
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            node.accentColor.copy(alpha = 0.95f),
                            node.accentColor.copy(alpha = 0.55f),
                        ),
                    ),
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = { onClick(node) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            AvatarMonogram(
                name = node.displayName,
                size = bubbleSize * 0.66f,
                accentBrush = Brush.linearGradient(
                    colors = listOf(node.accentColor, node.accentColor.copy(alpha = 0.7f)),
                ),
            )
        }
    }
}

private fun polarToOffset(
    angleDegrees: Float,
    distanceNormalized: Float,
    canvasSizePx: Float,
): Pair<Float, Float> {
    val radius = (canvasSizePx / 2f) * distanceNormalized
    val radians = Math.toRadians(angleDegrees.toDouble() - 90.0)
    val x = (cos(radians) * radius).toFloat() + canvasSizePx / 2f
    val y = (sin(radians) * radius).toFloat() + canvasSizePx / 2f
    return x to y
}
