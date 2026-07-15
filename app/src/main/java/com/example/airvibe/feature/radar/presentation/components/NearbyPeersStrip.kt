package com.example.airvibe.feature.radar.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.feature.radar.domain.model.RadarNode

private val PremiumGold = Color(0xFFFFD700)
private val PremiumGoldDark = Color(0xFFB8860B)

/**
 * Lista horizontal de dispositivos cercanos. Sirve como respaldo
 * cuando las burbujas del radar son difíciles de ver o tocar.
 */
@Composable
fun NearbyPeersStrip(
    nodes: List<RadarNode>,
    onNodeClick: (RadarNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (nodes.isEmpty()) return

    val tokens = AirVibeTheme.glass
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        nodes.forEach { node ->
            val meters = proximityMeters(node.distanceNormalized)
            val borderColor = if (node.isPremium) PremiumGold else node.accentColor.copy(alpha = 0.65f)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (node.isPremium) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    tokens.surfaceFillStrong,
                                    PremiumGold.copy(alpha = 0.08f),
                                ),
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(tokens.surfaceFillStrong, tokens.surfaceFillStrong),
                            )
                        },
                    )
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .clickable { onNodeClick(node) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Indicador Premium (pequeña estrella/icono dorado)
                if (node.isPremium) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(PremiumGold, PremiumGoldDark),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WorkspacePremium,
                            contentDescription = "Premium",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }
                Text(
                    text = node.displayName,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "~${meters} m",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (node.isPremium) PremiumGoldDark else node.accentColor,
                )
            }
        }
    }
}
