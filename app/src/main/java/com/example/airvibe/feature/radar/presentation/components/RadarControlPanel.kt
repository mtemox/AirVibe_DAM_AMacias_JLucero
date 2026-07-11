package com.example.airvibe.feature.radar.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BroadcastOnPersonal
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.airvibe.R
import com.example.airvibe.core.designsystem.components.GlassCard
import com.example.airvibe.core.designsystem.modifiers.glassBlur
import com.example.airvibe.core.designsystem.theme.AirVibeTheme

/**
 * Panel flotante de acciones rápidas.
 *
 * Botones (izquierda → derecha):
 *  1. **Escaneo** — activa/pausa el radar Bluetooth.
 *  2. **Broadcast** — invita a todos los peers cercanos.
 *  3. **Filtros** — alertas por profesión o etiqueta.
 */
@Composable
fun RadarControlPanel(
    onScanToggle: () -> Unit,
    onBroadcast: () -> Unit,
    onCenter: () -> Unit,
    onFilter: () -> Unit,
    isScanning: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlassCard(
            cornerRadius = 36.dp,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ControlIcon(
                    icon = if (isScanning) Icons.Rounded.NearMe else Icons.Rounded.MyLocation,
                    label = if (isScanning) {
                        stringResource(R.string.radar_control_scan_stop)
                    } else {
                        stringResource(R.string.radar_control_scan_start)
                    },
                    onClick = onScanToggle,
                    highlighted = isScanning,
                )
                ControlIcon(
                    icon = Icons.Rounded.BroadcastOnPersonal,
                    label = stringResource(R.string.radar_control_broadcast),
                    onClick = onBroadcast,
                )
                ControlIcon(
                    icon = Icons.Rounded.Tune,
                    label = stringResource(R.string.radar_control_filters),
                    onClick = onFilter,
                )
            }
        }
        Text(
            text = stringResource(R.string.radar_control_scan_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun ControlIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    val tokens = AirVibeTheme.glass

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.semantics { contentDescription = label },
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
                .glassBlur(radius = 18.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    color = if (highlighted) accent.copy(alpha = 0.85f) else tokens.surfaceFillStrong,
                )
                .border(
                    width = 1.dp,
                    color = if (highlighted) accent.copy(alpha = 0.7f) else tokens.outerBorder,
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) scheme.onPrimary else scheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
