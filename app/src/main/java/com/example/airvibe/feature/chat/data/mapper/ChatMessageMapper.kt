package com.example.airvibe.feature.chat.data.mapper

import com.example.airvibe.feature.chat.data.local.dao.ConversationSummaryRow
import com.example.airvibe.feature.chat.data.local.entity.ChatMessageEntity
import com.example.airvibe.feature.chat.domain.model.ChatMessage
import com.example.airvibe.feature.chat.domain.model.MessageDirection
import com.example.airvibe.feature.chat.domain.model.MessageKind
import com.example.airvibe.feature.chat.domain.model.MessageStatus
import com.example.airvibe.feature.chat.domain.repository.ConversationSummary

/**
 * Conversores Entity ↔ Dominio para la feature de chat.
 *
 * Mantener esta transformación en la capa `data` permite que
 * el resto de la app consuma modelos puros sin filtrar detalles
 * de Room ni de SQL.
 */
object ChatMessageMapper {

    fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
        id = id,
        nodeId = nodeId,
        text = text,
        direction = direction.toDirection(),
        status = status.toStatus(),
        kind = kind.toKind(),
        createdAt = createdAt,
        isSynced = isSynced,
    )

    fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
        id = id,
        nodeId = nodeId,
        text = text,
        direction = direction.name,
        status = status.name,
        kind = kind.name,
        createdAt = createdAt,
        isSynced = isSynced,
    )

    fun ConversationSummaryRow.toSummary(): ConversationSummary = ConversationSummary(
        nodeId = nodeId,
        displayName = displayName,
        lastMessage = lastMessage,
        lastTimestamp = lastTimestamp,
        unreadCount = 0, // futuro: integrar flags de "leído"
        isGroupInvite = kind.equals(MessageKind.GroupInvite.name, ignoreCase = true),
    )

    private fun String.toDirection(): MessageDirection =
        runCatching { MessageDirection.valueOf(this) }
            .getOrDefault(MessageDirection.Incoming)

    private fun String.toStatus(): MessageStatus =
        runCatching { MessageStatus.valueOf(this) }
            .getOrDefault(MessageStatus.Sent)

    private fun String.toKind(): MessageKind =
        runCatching { MessageKind.valueOf(this) }
            .getOrDefault(MessageKind.Text)
}
