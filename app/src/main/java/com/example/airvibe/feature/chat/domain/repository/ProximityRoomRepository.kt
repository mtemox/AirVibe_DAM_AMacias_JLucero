package com.example.airvibe.feature.chat.domain.repository

import com.example.airvibe.feature.chat.domain.model.ProximityRoom
import com.example.airvibe.feature.chat.domain.model.RoomMember
import com.example.airvibe.feature.chat.domain.model.RoomMessage
import kotlinx.coroutines.flow.Flow

interface ProximityRoomRepository {
    fun observeRoom(roomId: String): Flow<ProximityRoom?>
    fun observeMessages(roomId: String): Flow<List<RoomMessage>>
    fun observeActiveRooms(): Flow<List<ProximityRoom>>

    suspend fun getRoom(roomId: String): ProximityRoom?

    suspend fun createHostRoom(title: String, hostNodeId: String, hostName: String): ProximityRoom
    suspend fun receiveInvite(
        roomId: String,
        title: String,
        hostNodeId: String,
        hostName: String,
        createdAt: Long,
    ): ProximityRoom

    suspend fun joinRoom(roomId: String)
    suspend fun deleteRoomLocally(roomId: String)
    suspend fun insertOutgoingMessage(roomId: String, text: String): RoomMessage
    suspend fun persistIncomingMessage(
        roomId: String,
        senderNodeId: String,
        senderName: String,
        text: String,
        createdAt: Long,
        messageId: String,
    ): RoomMessage?

    // -------- Feature 4: members --------

    fun observeActiveMembers(roomId: String): Flow<List<RoomMember>>
    fun observeAllMembers(roomId: String): Flow<List<RoomMember>>

    /**
     * Registra al usuario local como Guest de la sala [roomId]
     * y lo marca activo.
     */
    suspend fun registerLocalGuest(roomId: String)

    /**
     * El Host agrega a [nodeId] como Guest. Se persiste con
     * `isActive = true` y `lastSeenAt = now`.
     */
    suspend fun registerGuest(
        roomId: String,
        nodeId: String,
        displayName: String,
        role: String = com.example.airvibe.feature.chat.data.local.entity.RoomMemberEntity.ROLE_GUEST,
    )

    suspend fun markMemberLeft(roomId: String, nodeId: String)
    suspend fun refreshMemberPresence(roomId: String, nodeId: String, displayName: String? = null)
}
