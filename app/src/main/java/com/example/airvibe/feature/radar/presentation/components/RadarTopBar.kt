package com.example.airvibe.feature.radar.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.components.AvatarMonogram
import com.example.airvibe.core.designsystem.components.GlassCard
import com.example.airvibe.core.designsystem.components.StatusDot
import com.example.airvibe.core.designsystem.theme.AirVibeTheme

@Composable
fun RadarTopBar(
    userName: String,
    activeCount: Int,
    isScanning: Boolean,
    modifier: Modifier = Modifier,
    discoveredPeers: Int = 0,
    chatCount: Int = 0,
    isServiceActive: Boolean = false,
    onSignOut: (() -> Unit)? = null,
    onOpenChats: (() -> Unit)? = null,
    onOpenFriends: (() -> Unit)? = null,
    onOpenRooms: (() -> Unit)? = null,
    onEditProfile: (() -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AvatarMonogram(
                            name = userName,
                            size = 38.dp,
                            modifier = if (onEditProfile != null) {
                                Modifier.clickable(onClick = onEditProfile)
                            } else {
                                Modifier
                            },
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .padding(1.dp),
                        ) {
                            StatusDot(
                                color = if (isScanning) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                pulse = isScanning,
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Hola, ${userName.substringBefore(' ')}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val statusText = when {
                            isServiceActive -> "Radar en background · escaneando"
                            isScanning -> "Escaneando entorno"
                            else -> "Escaneo en pausa"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "$activeCount cerca",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(
                            imageVector = Icons.Rounded.PersonOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }

                    val tokens = AirVibeTheme.glass
                    Box {
                        BadgedBox(
                            badge = {
                                if (chatCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ) { Text(chatCount.toString()) }
                                }
                            },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(tokens.surfaceFill)
                                    .border(
                                        width = 1.dp,
                                        color = tokens.outerBorder,
                                        shape = CircleShape,
                                    )
                                    .clickable { menuExpanded = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = "Menú",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            if (onOpenRooms != null) {
                                DropdownMenuItem(
                                    text = { Text("Salas cercanas") },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenRooms()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Groups, contentDescription = null)
                                    },
                                )
                            }
                            if (onOpenFriends != null) {
                                DropdownMenuItem(
                                    text = { Text("Mis amigos") },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenFriends()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.PersonOutline, contentDescription = null)
                                    },
                                )
                            }
                            if (onOpenChats != null) {
                                DropdownMenuItem(
                                    text = { Text("Chats") },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenChats()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null)
                                    },
                                )
                            }
                            if (onSignOut != null) {
                                DropdownMenuItem(
                                    text = { Text("Cerrar sesión") },
                                    onClick = {
                                        menuExpanded = false
                                        onSignOut()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Logout, contentDescription = null)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
