package com.example.airvibe.feature.main.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airvibe.R

import androidx.compose.foundation.layout.systemBarsPadding

@Composable
fun DrawerContent(
    modifier: Modifier = Modifier,
    onSignOutClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .systemBarsPadding()
                .padding(bottom = 24.dp, top = 8.dp)
        ) {
            // Header Profile
            val profile by com.example.airvibe.core.di.ServiceLocator.scannerProfileRepository.observe()
                .collectAsStateWithLifecycle(initialValue = null)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile?.avatarBase64 != null) {
                        com.example.airvibe.core.designsystem.components.AvatarMonogram(
                            name = profile?.displayName ?: "?",
                            size = 48.dp,
                            imageModel = profile?.avatarBase64
                        )
                    } else if (profile != null) {
                        com.example.airvibe.core.designsystem.components.AvatarMonogram(
                            name = profile?.displayName ?: "?",
                            size = 48.dp
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_background), // Fallback image
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = profile?.displayName ?: "Usuario",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Miembro de AirVibe",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Links
            DrawerItem(
                icon = Icons.Rounded.Home,
                label = "Inicio",
                isActive = true
            )
            DrawerItem(
                icon = Icons.Rounded.History,
                label = "Historial",
                isActive = false
            )
            DrawerItem(
                icon = Icons.Rounded.Notifications,
                label = "Notificaciones",
                isActive = false
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            DrawerItem(
                icon = Icons.Rounded.SupportAgent,
                label = "Soporte",
                isActive = false
            )

            Spacer(modifier = Modifier.weight(1f))

            // Sign Out
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(CircleShape)
                    .clickable { onSignOutClick() }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Cerrar sesión",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // App Version
            Text(
                text = "v1.0.2",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit = {}
) {
    val containerColor = if (isActive) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal),
            color = contentColor
        )
    }
}
