package com.example.airvibe.feature.chat.domain.repository

import com.example.airvibe.feature.chat.domain.model.ProximityRoom
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
    suspend fun insertOutgoingMessage(roomId: String, text: String): RoomMessage
    suspend fun persistIncomingMessage(
        roomId: String,
        senderNodeId: String,
        senderName: String,
        text: String,
        createdAt: Long,
        messageId: String,
    ): RoomMessage?
}
