package com.example.airvibe.feature.chat.data.remote

import com.example.airvibe.core.network.toEpochMillisOr
import com.example.airvibe.core.network.toIsoTimestamp
import com.example.airvibe.feature.chat.data.local.entity.RoomMessageEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteRoomMessageDto(
    @SerialName("id") val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("sender_node_id") val senderNodeId: String,
    @SerialName("sender_name") val senderName: String,
    @SerialName("content") val content: String,
    @SerialName("is_own") val isOwn: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

fun RoomMessageEntity.toRemoteDto(ownerId: String): RemoteRoomMessageDto = RemoteRoomMessageDto(
    id = id,
    ownerId = ownerId,
    roomId = roomId,
    senderNodeId = senderNodeId,
    senderName = senderName,
    content = text,
    isOwn = isOwn,
    createdAt = createdAt.toIsoTimestamp(),
)

fun RemoteRoomMessageDto.toEntity(): RoomMessageEntity = RoomMessageEntity(
    id = id,
    roomId = roomId,
    senderNodeId = senderNodeId,
    senderName = senderName,
    text = content,
    createdAt = createdAt.toEpochMillisOr(),
    isOwn = isOwn,
    isSynced = true,
)
