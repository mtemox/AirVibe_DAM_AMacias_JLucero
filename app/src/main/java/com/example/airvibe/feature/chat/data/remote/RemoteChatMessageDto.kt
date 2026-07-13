package com.example.airvibe.feature.chat.data.remote

import com.example.airvibe.core.network.toEpochMillisOr
import com.example.airvibe.core.network.toIsoTimestamp
import com.example.airvibe.feature.chat.data.local.entity.ChatMessageEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para sincronizar mensajes 1-a-1 con Supabase (offline-first).
 *
 * Cada usuario respalda su propia copia del historial; la fila se
 * ata a [ownerId] (= `auth.uid()`) para que la RLS restrinja el
 * acceso. La identidad del otro participante viaja como
 * [peerNodeId] (TEXT, Bluetooth `device-<UUID>`) y la dirección
 * del mensaje ([direction]) distingue si fue enviado o recibido.
 */
@Serializable
data class RemoteChatMessageDto(
    @SerialName("id") val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("peer_node_id") val peerNodeId: String,
    @SerialName("direction") val direction: String,
    @SerialName("content") val content: String,
    @SerialName("kind") val kind: String,
    @SerialName("status") val status: String,
    @SerialName("created_at") val createdAt: String? = null,
)

fun ChatMessageEntity.toRemoteDto(ownerId: String): RemoteChatMessageDto = RemoteChatMessageDto(
    id = id,
    ownerId = ownerId,
    peerNodeId = nodeId,
    direction = direction,
    content = text,
    kind = kind,
    status = status,
    createdAt = createdAt.toIsoTimestamp(),
)

fun RemoteChatMessageDto.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    nodeId = peerNodeId,
    text = content,
    direction = direction,
    status = status,
    kind = kind,
    createdAt = createdAt.toEpochMillisOr(),
    isSynced = true,
)
