package com.example.airvibe.feature.radar.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.feature.radar.domain.model.RadarNode

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
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(tokens.surfaceFillStrong)
                    .border(1.dp, node.accentColor.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                    .clickable { onNodeClick(node) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                    color = node.accentColor,
                )
            }
        }
    }
}
