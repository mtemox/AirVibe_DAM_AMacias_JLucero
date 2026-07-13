package com.example.airvibe.feature.radar.data.sync

import com.example.airvibe.feature.chat.data.local.dao.ChatDao
import com.example.airvibe.feature.chat.data.remote.SupabaseChatMessageDataSource
import com.example.airvibe.feature.chat.data.remote.SupabaseProximityRoomDataSource
import com.example.airvibe.feature.chat.data.remote.SupabaseRoomMessageDataSource
import com.example.airvibe.feature.chat.data.remote.toEntity
import com.example.airvibe.feature.chat.data.remote.toRemoteDto
import com.example.airvibe.feature.chat.data.local.dao.ProximityRoomDao
import com.example.airvibe.feature.radar.data.local.dao.SavedContactDao
import com.example.airvibe.feature.radar.data.remote.SupabaseSavedContactDataSource
import com.example.airvibe.feature.radar.data.remote.toEntity
import com.example.airvibe.feature.radar.data.remote.toRemoteDto

/**
 * Sincroniza datos offline-first con Supabase:
 * - Amigos guardados ([saved_contacts])
 * - Salas cercanas ([proximity_rooms])
 * - Mensajes de sala ([room_messages])
 * - Mensajes 1-a-1 ([chat_messages])
 *
 * Cada usuario respalda su propia copia en la nube (owner_id = auth.uid()).
 */
class CloudSyncService(
    private val savedContactDao: SavedContactDao,
    private val roomDao: ProximityRoomDao,
    private val chatDao: ChatDao,
    private val savedContactRemote: SupabaseSavedContactDataSource,
    private val roomRemote: SupabaseProximityRoomDataSource,
    private val roomMessageRemote: SupabaseRoomMessageDataSource,
    private val chatMessageRemote: SupabaseChatMessageDataSource,
) {

    /** Descarga datos de la nube e inserta lo que falta localmente. */
    suspend fun restoreFromRemote(ownerId: String): Result<Unit> = runCatching {
        restoreContacts(ownerId)
        restoreRooms(ownerId)
        restoreRoomMessages(ownerId)
        restoreChatMessages(ownerId)
    }

    /** Sube registros locales con is_synced = 0. */
    suspend fun pushPending(ownerId: String): Result<Unit> = runCatching {
        pushContacts(ownerId)
        pushRooms(ownerId)
        pushRoomMessages(ownerId)
        pushChatMessages(ownerId)
    }

    private suspend fun restoreContacts(ownerId: String) {
        val remote = savedContactRemote.fetchAll(ownerId).getOrNull() ?: return
        remote.forEach { dto ->
            val local = savedContactDao.getById(dto.peerNodeId)
            if (local == null || local.isSynced) {
                savedContactDao.upsert(dto.toEntity())
            }
        }
    }

    private suspend fun restoreRooms(ownerId: String) {
        val remote = roomRemote.fetchAll(ownerId).getOrNull() ?: return
        remote.forEach { dto ->
            val local = roomDao.getRoom(dto.id)
            if (local == null || local.isSynced) {
                roomDao.upsertRoom(dto.toEntity())
            }
        }
    }

    private suspend fun restoreRoomMessages(ownerId: String) {
        val remote = roomMessageRemote.fetchAll(ownerId).getOrNull() ?: return
        remote.forEach { dto ->
            if (!roomDao.messageExists(dto.id)) {
                roomDao.upsertMessage(dto.toEntity())
            }
        }
    }

    private suspend fun pushContacts(ownerId: String) {
        val pending = savedContactDao.getPendingSync()
        if (pending.isEmpty()) return
        val dtos = pending.map { it.toRemoteDto(ownerId) }
        val synced = savedContactRemote.upsert(dtos).getOrNull() ?: return
        savedContactDao.markAsSynced(synced)
    }

    private suspend fun pushRooms(ownerId: String) {
        val pending = roomDao.getPendingRooms()
        if (pending.isEmpty()) return
        val dtos = pending.map { it.toRemoteDto(ownerId) }
        val synced = roomRemote.upsert(dtos).getOrNull() ?: return
        roomDao.markRoomsSynced(synced)
    }

    private suspend fun pushRoomMessages(ownerId: String) {
        val pending = roomDao.getPendingMessages()
        if (pending.isEmpty()) return
        val dtos = pending.map { it.toRemoteDto(ownerId) }
        val synced = roomMessageRemote.upsert(dtos).getOrNull() ?: return
        roomDao.markMessagesSynced(synced)
    }

    private suspend fun restoreChatMessages(ownerId: String) {
        val remote = chatMessageRemote.fetchAll(ownerId).getOrNull() ?: return
        remote.forEach { dto ->
            if (chatDao.getById(dto.id) == null) {
                chatDao.upsertAll(listOf(dto.toEntity()))
            }
        }
    }

    private suspend fun pushChatMessages(ownerId: String) {
        val pending = chatDao.getUnsynced()
        if (pending.isEmpty()) return
        val dtos = pending.map { it.toRemoteDto(ownerId) }
        val synced = chatMessageRemote.upsert(dtos).getOrNull() ?: return
        chatDao.markAsSynced(synced)
    }
}
