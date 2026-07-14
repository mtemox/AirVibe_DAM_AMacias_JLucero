package com.example.airvibe.feature.chat.data.mapper

import com.example.airvibe.feature.chat.data.local.entity.ProximityRoomEntity
import com.example.airvibe.feature.chat.data.local.entity.RoomMemberEntity
import com.example.airvibe.feature.chat.data.local.entity.RoomMessageEntity
import com.example.airvibe.feature.chat.domain.model.ProximityRoom
import com.example.airvibe.feature.chat.domain.model.RoomMember
import com.example.airvibe.feature.chat.domain.model.RoomMessage

object RoomMapper {

    fun ProximityRoomEntity.toDomain(): ProximityRoom = ProximityRoom(
        id = id,
        title = title,
        hostNodeId = hostNodeId,
        hostName = hostName,
        createdAt = createdAt,
        joined = joined,
        isHost = isHost,
    )

    fun RoomMessageEntity.toDomain(localUserId: String): RoomMessage = RoomMessage(
        id = id,
        roomId = roomId,
        senderNodeId = senderNodeId,
        senderName = senderName,
        text = text,
        createdAt = createdAt,
        // is_own is set at write-time; fall back to ID comparison for legacy rows
        isOwn = if (isOwn) true else senderNodeId == localUserId,
    )

    fun RoomMemberEntity.toDomain(): RoomMember = RoomMember(
        id = id,
        roomId = roomId,
        nodeId = nodeId,
        displayName = displayName,
        role = role,
        isActive = isActive,
        joinedAt = joinedAt,
        lastSeenAt = lastSeenAt,
    )
}
