package com.example.airvibe.feature.radar.presentation.components.permissions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.airvibe.R
import com.example.airvibe.core.designsystem.components.GlassCard
import com.example.airvibe.core.designsystem.components.LiquidGlassButton
import com.example.airvibe.core.designsystem.components.LiquidGlassVariant
import com.example.airvibe.core.designsystem.components.StatusDot
import com.example.airvibe.core.designsystem.modifiers.glassBlur
import com.example.airvibe.core.designsystem.modifiers.glassShadow
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.core.permissions.RadarPermission
import com.example.airvibe.core.permissions.RadarPermissionsState

/**
 * Modal full-screen con efecto glass que solicita los permisos
 * necesarios para activar el radar. Se muestra por encima de la
 * UI principal con un scrim sutil y contenido centrado.
 *
 * El modal es agnóstico al estado: decide su copy en función de
 * [RadarPermissionsState] (todos concedidos → ocultar; faltan →
 * mostrar CTA). Esto permite a la UI simplemente renderizarlo y
 * dejar que reaccione a los cambios de permisos.
 */
@Composable
fun PermissionsModal(
    state: RadarPermissionsState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = !state.allGranted,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 }),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC0F172A),
                            Color(0xD9070B13),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PermissionsCard(state = state, onDismiss = onDismiss)
            }
        }
    }
}

@Composable
private fun PermissionsCard(
    state: RadarPermissionsState,
    onDismiss: () -> Unit,
) {
    val tokens = AirVibeTheme.glass
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassShadow(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                cornerRadius = 32.dp,
            )
            .glassBlur(radius = 28.dp, shape = RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tokens.surfaceFillStrong,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = tokens.outerBorder,
                shape = RoundedCornerShape(32.dp),
            )
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .glassBlur(radius = 24.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.25f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Radar,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp),
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.permissions_modal_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.permissions_modal_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(18.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.permissions.forEach { permission ->
                    PermissionRow(
                        permission = permission,
                        isGranted = state.missing.none { it == permission },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LiquidGlassButton(
                text = stringResource(R.string.permissions_modal_action_later),
                onClick = onDismiss,
                variant = LiquidGlassVariant.Secondary,
                modifier = Modifier.weight(1f),
            )
            LiquidGlassButton(
                text = stringResource(
                    if (state.missing.isNotEmpty() && state.shouldShowRationale) {
                        R.string.permissions_modal_action_settings
                    } else {
                        R.string.permissions_modal_action_grant
                    },
                ),
                onClick = {
                    if (state.shouldShowRationale && state.missing.isNotEmpty()) {
                        state.openAppSettings()
                    } else {
                        state.requestAll()
                    }
                },
                variant = LiquidGlassVariant.Primary,
                modifier = Modifier.weight(1.4f),
            )
        }
    }
}

@Composable
private fun PermissionRow(
    permission: RadarPermission,
    isGranted: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = permission.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(permission.titleRes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(permission.rationaleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusDot(
            color = if (isGranted) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.outline
            },
            size = 10.dp,
            pulse = !isGranted,
        )
    }
}

@Composable
private fun RadarPermission.icon(): ImageVector = when (this) {
    RadarPermission.BLUETOOTH_SCAN,
    RadarPermission.BLUETOOTH_ADVERTISE,
    RadarPermission.BLUETOOTH_CONNECT,
    -> Icons.Rounded.Bluetooth
    RadarPermission.ACCESS_FINE_LOCATION -> Icons.Rounded.LocationOn
    RadarPermission.NEARBY_WIFI_DEVICES -> Icons.Rounded.Wifi
    RadarPermission.POST_NOTIFICATIONS -> Icons.Rounded.NotificationsActive
}
