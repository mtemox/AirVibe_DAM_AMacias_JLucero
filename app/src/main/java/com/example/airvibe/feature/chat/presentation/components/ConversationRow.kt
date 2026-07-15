package com.example.airvibe.feature.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.components.AvatarMonogram
import com.example.airvibe.core.designsystem.components.GlassPill
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.feature.chat.domain.repository.ConversationSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fila de la bandeja de entrada con la última actividad de un
 * peer. Toca la fila para abrir la conversación.
 */
@Composable
fun ConversationRow(
    summary: ConversationSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = AirVibeTheme.glass
    val shape = RoundedCornerShape(22.dp)
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timestamp = remember(summary.lastTimestamp) {
        timeFormatter.format(Date(summary.lastTimestamp))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(shape)
            .background(tokens.surfaceFill)
            .border(width = 1.dp, color = tokens.outerBorder, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tokens.surfaceFillStrong),
                contentAlignment = Alignment.Center,
            ) {
                if (summary.isGroupInvite) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    AvatarMonogram(
                        name = summary.displayName,
                        size = 40.dp,
                        imageModel = summary.avatarBase64
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = summary.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (summary.isGroupInvite) {
                        GlassPill(
                            text = "Invitación",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = summary.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (summary.unreadCount > 0) {
                    Spacer(modifier = Modifier.size(4.dp))
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = summary.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}
