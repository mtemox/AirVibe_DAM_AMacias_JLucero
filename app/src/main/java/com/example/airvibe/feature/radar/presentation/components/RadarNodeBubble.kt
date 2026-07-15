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
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.offset
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
    isSelected: Boolean = false,
    selectedProfile: com.example.airvibe.feature.radar.domain.model.PersonProfile? = null,
    onConnect: () -> Unit = {},
    onDismissPreview: () -> Unit = {}
) {
    val isActive = node.presence != PresenceStatus.Away


    val interactionSource = remember { MutableInteractionSource() }
    val bubbleSize: Dp = (44 + node.signalStrength * 14).dp
    val ringSize: Dp = bubbleSize + 18.dp
    val meters = proximityMeters(node.distanceNormalized)
    val showDistance = !node.id.startsWith("pending-")
    val token = node.intentToken()
    val emergencyPulse = node.presence == PresenceStatus.Emergency

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
                    .size(bubbleSize)
                    .shadow(elevation = 14.dp, shape = CircleShape, clip = false)
                    .glassBlur(radius = 18.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                token.accent.copy(alpha = 0.35f),
                            ),
                        ),
                    )
                    .border(
                        width = 2.dp,
                        color = token.accent,
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
                        colors = listOf(token.accent, token.accent.copy(alpha = 0.7f)),
                    ),
                    imageModel = node.avatarBase64
                )
            }

            // Pequeño chip glassmórfico en la esquina superior derecha
            // que indica visualmente la intención del peer (Networking,
            // Servicio, Grupo, Emergencia).
            if (!node.id.startsWith("pending-")) {
                val chipSize = (bubbleSize.value * 0.42f).coerceAtLeast(20f).dp
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(chipSize)
                        .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                        .glassBlur(radius = 12.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(
                            color = if (emergencyPulse) {
                                token.accent.copy(alpha = 0.95f)
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            },
                        )
                        .border(
                            width = 1.5.dp,
                            color = token.accent,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = token.icon,
                        contentDescription = token.label,
                        tint = if (emergencyPulse) Color.White else token.accent,
                        modifier = Modifier.size(chipSize * 0.62f),
                    )
                }
            }

            // Anillo pulsante para emergencias
            if (emergencyPulse) {
                val infinite = rememberInfiniteTransition(label = "emergencyPulse")
                val pulseScale by infinite.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1100, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "emergencyPulseScale",
                )
                val pulseAlpha by infinite.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1100, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "emergencyPulseAlpha",
                )
                Box(
                    modifier = Modifier
                        .size(ringSize)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = token.accent.copy(alpha = pulseAlpha),
                            shape = CircleShape,
                        ),
                )
            }
            
            // --- INLINE PREVIEW POPUP ---
            if (isSelected) {
                val density = LocalDensity.current
                val popupOffset = remember(density, ringSize) {
                    with(density) { IntOffset(ringSize.roundToPx() + 4.dp.roundToPx(), (-24).dp.roundToPx()) }
                }
                Popup(
                    alignment = Alignment.TopStart,
                    offset = popupOffset,
                    properties = PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true),
                    onDismissRequest = onDismissPreview
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(min = 230.dp, max = 280.dp)
                            .shadow(12.dp, RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AvatarMonogram(
                                    name = node.displayName,
                                    size = 48.dp,
                                    accentBrush = Brush.linearGradient(
                                        colors = listOf(token.accent, token.accent.copy(alpha = 0.7f)),
                                    ),
                                    imageModel = node.avatarBase64
                                )
                                Column {
                                    Text(
                                        text = node.displayName,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    if (node.headline.isNotBlank()) {
                                        Text(
                                            text = node.headline,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = if (node.isPremium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    if (node.bio.isNotBlank()) {
                                        Text(
                                            text = node.bio,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (node.isPremium && node.premiumCatalog?.isNotBlank() == true) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.ChatBubble, // Re-using chat bubble icon for simplicity or star if available
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Catálogo: ${node.premiumCatalog}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                                color = MaterialTheme.colorScheme.tertiary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.SignalCellularAlt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "~${meters}m • ${if(node.signalStrength > 0.6) "Fuerte" else "Débil"}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = onConnect,
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = "Mensaje",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Mensaje", style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp))
                                }
                            }
                        }
                        
                        // Tail triangle on the left side
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (-6).dp)
                                .size(12.dp)
                                .rotate(45f)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        )
                        
                        // Cover the inner line of the triangle
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = 0.dp)
                                .size(width = 6.dp, height = 16.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }
                }
            }
        }

        if (showDistance) {
            Text(
                text = "~${meters} m",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = token.accent,
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
