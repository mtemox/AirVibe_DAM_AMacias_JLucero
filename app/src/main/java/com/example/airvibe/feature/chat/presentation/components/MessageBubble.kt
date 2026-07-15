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
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
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
import androidx.compose.ui.unit.sp
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.feature.chat.domain.model.ChatMessage
import com.example.airvibe.feature.chat.domain.model.MessageDirection
import com.example.airvibe.feature.chat.domain.model.MessageKind
import com.example.airvibe.feature.chat.domain.model.MessageStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Burbuja de chat estilo WhatsApp. Se alinea a la derecha para
 * mensajes salientes y a la izquierda para los entrantes.
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    isAboveSameSender: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isOutgoing = message.direction == MessageDirection.Outgoing
    val isInvite = message.kind == MessageKind.GroupInvite

    val topCorner = if (isAboveSameSender) 8.dp else 0.dp
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 8.dp, topEnd = topCorner, bottomStart = 8.dp, bottomEnd = 8.dp)
    } else {
        RoundedCornerShape(topStart = topCorner, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    }

    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bubbleColor = if (isOutgoing) {
        if (isDark) Color(0xFF005C4B) else Color(0xFFDCF8C6)
    } else {
        if (isDark) androidx.compose.material3.MaterialTheme.colorScheme.surface else Color.White
    }

    val textColor = if (isDark) Color(0xFFE9EDEF) else Color(0xFF1A1C1C)
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
                .background(bubbleColor)
                .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 4.dp),
        ) {
            // Utilizamos un padding end mayor para asegurar que el texto
            // nunca se superponga con la hora.
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.padding(end = if (isOutgoing) 48.dp else 36.dp, bottom = 8.dp)
            )

            MessageMetaBox(
                timestampMillis = message.createdAt,
                status = message.status,
                isOutgoing = isOutgoing,
                isRead = message.isRead,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
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
private fun MessageMetaBox(
    timestampMillis: Long,
    status: MessageStatus,
    isOutgoing: Boolean,
    isRead: Boolean,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val time = remember(timestampMillis) { timeFormatter.format(Date(timestampMillis)) }
    
    // Icono y color para el estado
    val (icon, tint) = when {
        status == MessageStatus.Sending -> Icons.Rounded.Schedule to Color(0xFF888888)
        status == MessageStatus.Failed -> Icons.Rounded.WarningAmber to MaterialTheme.colorScheme.error
        // Si fue leído (isRead == true), mostrar checks azules, sino, grises.
        status == MessageStatus.Synced || status == MessageStatus.Sent -> {
            // Nota: En WhatsApp, Sent es 1 check, Synced/Delivered es 2 checks, Read es 2 checks azules.
            // Para simplificar según lo solicitado, usaremos un icono de doble check.
            // Idealmente deberíamos importar Icons.Rounded.DoneAll, pero como tal vez no esté,
            // si no está, usamos Done. Pero aquí asumimos que podemos referenciar DoneAll de Material3.
            androidx.compose.material.icons.Icons.Rounded.DoneAll to if (isRead) Color(0xFF34B7F1) else Color(0xFF888888)
        }
        else -> Icons.Rounded.Check to Color(0xFF888888)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
        val timeColor = if (isDark) Color(0x99E9EDEF) else Color(0x991A1C1C)
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = timeColor,
        )
        if (isOutgoing) {
            Icon(
                imageVector = icon,
                contentDescription = status.displayName,
                tint = tint,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
