package com.example.airvibe.feature.chat.data.repository

import com.example.airvibe.feature.chat.data.local.dao.ProximityRoomDao
import com.example.airvibe.feature.chat.data.local.entity.ProximityRoomEntity
import com.example.airvibe.feature.chat.data.local.entity.RoomMemberEntity
import com.example.airvibe.feature.chat.data.local.entity.RoomMessageEntity
import com.example.airvibe.feature.chat.data.mapper.RoomMapper.toDomain
import com.example.airvibe.feature.chat.domain.model.ProximityRoom
import com.example.airvibe.feature.chat.domain.model.RoomMember
import com.example.airvibe.feature.chat.domain.model.RoomMessage
import com.example.airvibe.feature.chat.domain.repository.ProximityRoomRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProximityRoomRepositoryImpl(
    private val roomDao: ProximityRoomDao,
    private val localUserIdProvider: () -> String,
    private val localDisplayNameProvider: () -> String,
    private val onDataChanged: () -> Unit = {},
) : ProximityRoomRepository {

    override fun observeRoom(roomId: String): Flow<ProximityRoom?> =
        roomDao.observeRoomById(roomId).map { it?.toDomain() }

    override fun observeMessages(roomId: String): Flow<List<RoomMessage>> =
        roomDao.observeMessages(roomId).map { rows ->
            val localId = localUserIdProvider()
            rows.map { it.toDomain(localId) }
        }

    override fun observeActiveRooms(): Flow<List<ProximityRoom>> =
        roomDao.observeRooms().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getRoom(roomId: String): ProximityRoom? =
        roomDao.getRoom(roomId)?.toDomain()

    override suspend fun createHostRoom(
        title: String,
        hostNodeId: String,
        hostName: String,
    ): ProximityRoom {
        val room = ProximityRoomEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            hostNodeId = hostNodeId,
            hostName = hostName,
            createdAt = System.currentTimeMillis(),
            joined = true,
            isHost = true,
        )
        roomDao.upsertRoom(room)
        notifyChanged()
        return room.toDomain()
    }

    override suspend fun receiveInvite(
        roomId: String,
        title: String,
        hostNodeId: String,
        hostName: String,
        createdAt: Long,
    ): ProximityRoom {
        val existing = roomDao.getRoom(roomId)
        val room = ProximityRoomEntity(
            id = roomId,
            title = title.trim(),
            hostNodeId = hostNodeId,
            hostName = hostName,
            createdAt = createdAt,
            joined = existing?.joined ?: false,
            isHost = false,
        )
        roomDao.upsertRoom(room)
        notifyChanged()
        return room.toDomain()
    }

    override suspend fun joinRoom(roomId: String) {
        roomDao.markJoined(roomId)
        val localId = localUserIdProvider()
        val localName = localDisplayNameProvider()
        roomDao.upsertMember(
            RoomMemberEntity(
                roomId = roomId,
                nodeId = localId,
                displayName = localName,
                role = RoomMemberEntity.ROLE_GUEST,
                isActive = true,
            ),
        )
        notifyChanged()
    }

    override suspend fun registerLocalGuest(roomId: String) {
        val localId = localUserIdProvider()
        val localName = localDisplayNameProvider()
        roomDao.upsertMember(
            RoomMemberEntity(
                roomId = roomId,
                nodeId = localId,
                displayName = localName,
                role = RoomMemberEntity.ROLE_GUEST,
                isActive = true,
            ),
        )
        notifyChanged()
    }

    override suspend fun registerGuest(
        roomId: String,
        nodeId: String,
        displayName: String,
        role: String,
    ) {
        roomDao.upsertMember(
            RoomMemberEntity(
                roomId = roomId,
                nodeId = nodeId,
                displayName = displayName,
                role = role,
                isActive = true,
            ),
        )
        notifyChanged()
    }

    override suspend fun markMemberLeft(roomId: String, nodeId: String) {
        roomDao.markMemberInactive(roomId, nodeId)
        notifyChanged()
    }

    override suspend fun refreshMemberPresence(
        roomId: String,
        nodeId: String,
        displayName: String?,
    ) {
        if (displayName != null) {
            roomDao.updateMemberDisplayName(roomId, nodeId, displayName)
        } else {
            roomDao.markMemberActive(roomId, nodeId)
        }
        notifyChanged()
    }

    override fun observeActiveMembers(roomId: String): Flow<List<RoomMember>> =
        roomDao.observeActiveMembers(roomId).map { rows -> rows.map { it.toDomain() } }

    override fun observeAllMembers(roomId: String): Flow<List<RoomMember>> =
        roomDao.observeAllMembers(roomId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun insertOutgoingMessage(roomId: String, text: String): RoomMessage {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty())
        val now = System.currentTimeMillis()
        val messageId = UUID.randomUUID().toString()
        val senderId = localUserIdProvider()
        val senderName = localDisplayNameProvider()
        val entity = RoomMessageEntity(
            id = messageId,
            roomId = roomId,
            senderNodeId = senderId,
            senderName = senderName,
            text = trimmed,
            createdAt = now,
            isOwn = true,
        )
        roomDao.insertMessage(entity)
        notifyChanged()
        return entity.toDomain(senderId)
    }

    override suspend fun persistIncomingMessage(
        roomId: String,
        senderNodeId: String,
        senderName: String,
        text: String,
        createdAt: Long,
        messageId: String,
    ): RoomMessage? {
        if (roomDao.messageExists(messageId)) return null
        val entity = RoomMessageEntity(
            id = messageId,
            roomId = roomId,
            senderNodeId = senderNodeId,
            senderName = senderName.ifBlank { "Usuario cercano" },
            text = text,
            createdAt = createdAt,
            isOwn = false,
        )
        roomDao.insertMessage(entity)
        notifyChanged()
        return entity.toDomain(localUserIdProvider())
    }

    private fun notifyChanged() {
        runCatching { onDataChanged() }
    }
}
