package com.example.airvibe.feature.radar.presentation.components.permissions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airvibe.core.permissions.RadarPermissionsState

import androidx.compose.foundation.layout.systemBarsPadding

@Composable
fun PermissionsScreen(
    state: RadarPermissionsState,
    onDismiss: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showError by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize().systemBarsPadding(),
        color = Color(0xFFF9F9F9) // background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFFFF)) // surface-container-lowest
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "AirVibe",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )
            }

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                // Illustration Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(192.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F3F4)) // surface-container-low
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xCCFFFFFF))
                            .border(1.dp, Color(0x33C4C5D7), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Security & Access",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Heading Section
                Text(
                    text = "App Permissions",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To provide the best offline experience, AirVibe requires the following access:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Permissions List
                val bluetoothGranted = state.permissions.any { p -> 
                    p.name.contains("Bluetooth", ignoreCase = true) 
                } && !state.missing.any { p -> 
                    p.name.contains("Bluetooth", ignoreCase = true) 
                }

                PermissionToggleCard(
                    title = "Bluetooth",
                    description = "Used to create the local mesh network with nearby peers, allowing you to communicate without cellular data.",
                    icon = Icons.Rounded.Bluetooth,
                    iconTint = MaterialTheme.colorScheme.primaryContainer,
                    iconBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                    isGranted = bluetoothGranted
                )

                val locationGranted = state.permissions.any { p -> 
                    p.name.contains("Location", ignoreCase = true) 
                } && !state.missing.any { p -> 
                    p.name.contains("Location", ignoreCase = true) 
                }

                PermissionToggleCard(
                    title = "Location",
                    description = "Needed to scan for nearby devices and optimize node connections within your specific vicinity.",
                    icon = Icons.Rounded.LocationOn,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    iconBg = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    isGranted = locationGranted
                )

                val notifGranted = state.permissions.any { p -> 
                    p.name.contains("NOTIFICATIONS", ignoreCase = true) 
                } && !state.missing.any { p -> 
                    p.name.contains("NOTIFICATIONS", ignoreCase = true) 
                }

                PermissionToggleCard(
                    title = "Notifications",
                    description = "Alerts you when a peer is nearby or when you receive offline messages through the mesh network.",
                    icon = Icons.Rounded.Notifications,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    iconBg = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    isGranted = notifGranted
                )

                // Mock Contacts
                PermissionToggleCard(
                    title = "Contacts",
                    description = "Synchronizes your existing contact list to identify which friends are already on the AirVibe network.",
                    icon = Icons.Rounded.Contacts,
                    iconTint = MaterialTheme.colorScheme.outline,
                    iconBg = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    isGranted = false,
                    isButtonMock = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Footer Hint
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your data is encrypted and stored locally.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Bottom Action Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xE6FFFFFF))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (showError && !state.allGranted) {
                        Text(
                            text = "Please grant all required permissions to continue.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                    Button(
                        onClick = { 
                            if (state.allGranted) {
                                onDismiss()
                            } else {
                                showError = true
                                state.requestAll()
                            }
                        },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}
}

@Composable
private fun PermissionToggleCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    isGranted: Boolean,
    isButtonMock: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFFFFF)) // surface-container-lowest
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Toggle / Button
        if (isButtonMock) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE8E8E8)) // surface-container-high
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Grant",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            CustomToggleSwitch(
                isActive = isGranted
            )
        }
    }
}

@Composable
private fun CustomToggleSwitch(
    isActive: Boolean
) {
    val bgColor by animateColorAsState(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color(0xFFE2E2E2), label="bg")
    val translationX by animateFloatAsState(if (isActive) 20f else 2f, label="x")

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    this.translationX = translationX.dp.toPx()
                    this.translationY = 2.dp.toPx()
                }
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
