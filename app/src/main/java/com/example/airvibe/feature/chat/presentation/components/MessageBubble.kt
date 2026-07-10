package com.example.airvibe.feature.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.feature.chat.domain.model.ChatMessage
import com.example.airvibe.feature.chat.domain.model.MessageDirection
import com.example.airvibe.feature.chat.domain.model.MessageKind
import com.example.airvibe.feature.chat.domain.model.MessageStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Burbuja de chat estilo iOS. Se alinea a la derecha para
 * mensajes salientes y a la izquierda para los entrantes.
 *
 * El diseño respeta el lenguaje visual glass de AirVibe: bordes
 * sutiles, gradiente translúcido y radios asimétricos para la
 * "cola" de la burbuja.
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    val isOutgoing = message.direction == MessageDirection.Outgoing
    val isInvite = message.kind == MessageKind.GroupInvite
    val tokens = AirVibeTheme.glass

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 20.dp)
    }

    val bubbleBrush = if (isOutgoing) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
            ),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                tokens.surfaceFillStrong,
                tokens.surfaceFill,
            ),
        )
    }

    val textColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val alignment = if (isOutgoing) Alignment.End else Alignment.Start

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        if (isInvite) {
            InviteBadge(isOutgoing = isOutgoing)
            Spacer(modifier = Modifier.height(4.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(bubbleShape)
                .background(brush = bubbleBrush)
                .border(
                    width = 1.dp,
                    color = if (isOutgoing) {
                        Color.White.copy(alpha = 0.20f)
                    } else {
                        tokens.outerBorder
                    },
                    shape = bubbleShape,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        MessageMetaRow(
            timestampMillis = message.createdAt,
            status = message.status,
            isOutgoing = isOutgoing,
        )
    }
}

@Composable
private fun InviteBadge(isOutgoing: Boolean) {
    val tokens = AirVibeTheme.glass
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tokens.surfaceFill)
            .border(width = 1.dp, color = tokens.outerBorder, shape = RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = if (isOutgoing) "Invitación enviada" else "Invitación a grupo",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MessageMetaRow(
    timestampMillis: Long,
    status: MessageStatus,
    isOutgoing: Boolean,
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val time = remember(timestampMillis) { timeFormatter.format(Date(timestampMillis)) }
    val (icon, tint) = when (status) {
        MessageStatus.Sending -> Icons.Rounded.Schedule to MaterialTheme.colorScheme.outline
        MessageStatus.Sent -> Icons.Rounded.Check to MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.Synced -> Icons.Rounded.Check to MaterialTheme.colorScheme.primary
        MessageStatus.Failed -> Icons.Rounded.WarningAmber to MaterialTheme.colorScheme.error
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isOutgoing) {
            Icon(
                imageVector = icon,
                contentDescription = status.displayName,
                tint = tint,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}
