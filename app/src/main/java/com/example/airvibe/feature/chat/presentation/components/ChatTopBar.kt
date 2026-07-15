package com.example.airvibe.feature.chat.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airvibe.R

@Composable
fun ChatTopBar(
    peerDisplayName: String,
    peerAvatarBase64: String? = null,
    isConnected: Boolean,
    onBack: () -> Unit,
    onMore: () -> Unit = {},
    moreMenu: @Composable () -> Unit = {},
    subtitle: String? = null,
    badgeText: String = "Chat P2P",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(elevation = 2.dp)
            .background(Color.White)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Atrás",
                    tint = Color(0xFF444655)
                )
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E2E2))
                    .border(1.dp, Color(0xFFE2E2E2), CircleShape)
            ) {
                com.example.airvibe.core.designsystem.components.AvatarMonogram(
                    name = peerDisplayName,
                    size = 40.dp,
                    imageModel = peerAvatarBase64,
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = peerDisplayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal, fontSize = 18.sp),
                    color = Color(0xFF1A1C1C)
                )
                Text(
                    text = if (isConnected) "en línea" else "sin conexión",
                    color = Color(0xFF444655),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { /* TODO Video Call */ }) {
                Icon(
                    imageVector = Icons.Rounded.Videocam,
                    contentDescription = "Videollamada",
                    tint = Color(0xFF444655)
                )
            }
            IconButton(onClick = { /* TODO Call */ }) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = "Llamada",
                    tint = Color(0xFF444655)
                )
            }
            Box {
                IconButton(onClick = onMore) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Más",
                        tint = Color(0xFF444655)
                    )
                }
                moreMenu()
            }
        }
    }
}
