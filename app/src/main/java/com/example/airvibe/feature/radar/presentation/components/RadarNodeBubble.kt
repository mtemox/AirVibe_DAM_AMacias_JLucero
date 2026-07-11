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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airvibe.core.designsystem.components.AvatarMonogram
import com.example.airvibe.core.designsystem.modifiers.glassBlur
import com.example.airvibe.core.designsystem.modifiers.glow
import com.example.airvibe.feature.radar.domain.model.PresenceStatus
import com.example.airvibe.feature.radar.domain.model.RadarNode

/**
 * Burbuja flotante que representa un nodo del radar. Se posiciona
 * con [Alignment] relativo al centro del [Box] padre.
 */
@Composable
fun BoxScope.RadarNodeBubble(
    node: RadarNode,
    onClick: (RadarNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isActive = node.presence != PresenceStatus.Away

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
    val bubbleSize: Dp = (44 + node.signalStrength * 14).dp
    val ringSize: Dp = bubbleSize + 18.dp
    val meters = proximityMeters(node.distanceNormalized)
    val showDistance = !node.id.startsWith("pending-")

    Column(
        modifier = modifier.align(radarAlignmentFor(node)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(bubbleSize * 2.2f)
                    .glow(
                        color = node.accentColor.copy(alpha = 0.55f),
                        radius = bubbleSize * 1.2f,
                    ),
            )

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

            Box(
                modifier = Modifier
                    .size(bubbleSize)
                    .shadow(elevation = 14.dp, shape = CircleShape, clip = false)
                    .glassBlur(radius = 18.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.92f),
                                node.accentColor.copy(alpha = 0.35f),
                            ),
                        ),
                    )
                    .border(
                        width = 2.dp,
                        color = node.accentColor,
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

        if (showDistance) {
            Text(
                text = "~${meters} m",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = node.accentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = node.displayName,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
