package com.example.airvibe.feature.radar.data.sync

import com.example.airvibe.feature.chat.data.local.dao.ChatDao
import com.example.airvibe.feature.chat.data.remote.SupabaseChatMessageDataSource
import com.example.airvibe.feature.chat.data.remote.SupabaseProximityRoomDataSource
import com.example.airvibe.feature.chat.data.remote.SupabaseRoomMessageDataSource
import com.example.airvibe.feature.chat.data.remote.toEntity
import com.example.airvibe.feature.chat.data.remote.toRemoteDto
import com.example.airvibe.feature.chat.data.local.dao.ProximityRoomDao
import com.example.airvibe.feature.radar.data.local.dao.ProfileViewDao
import com.example.airvibe.feature.radar.data.local.dao.SavedContactDao
import com.example.airvibe.feature.radar.data.remote.SupabaseSavedContactDataSource
import com.example.airvibe.feature.radar.data.remote.SupabaseTelemetryDataSource
import com.example.airvibe.feature.radar.data.remote.RemoteVisibilityDayDto
import com.example.airvibe.feature.radar.data.remote.toEntity
import com.example.airvibe.feature.radar.data.remote.toRemoteDto
import com.example.airvibe.feature.radar.data.remote.toRemoteDto as toRemoteTelemetryDto
import com.example.airvibe.feature.radar.data.remote.toEntity as toTelemetryEntity
import com.example.airvibe.feature.radar.domain.model.VisibilityDay
import com.example.airvibe.feature.radar.domain.model.VisibilityStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Sincroniza datos offline-first con Supabase:
 * - Amigos guardados ([saved_contacts])
 * - Salas cercanas ([proximity_rooms])
 * - Mensajes de sala ([room_messages])
 * - Mensajes 1-a-1 ([chat_messages])            ← Feature 5: los empuja
 * - Telemetría Premium ([profile_views])        ← Feature 5: nuevo
 *
 * Cada usuario respalda su propia copia en la nube
 * (owner_id = auth.uid()).
 */
class CloudSyncService(
    private val savedContactDao: SavedContactDao,
    private val roomDao: ProximityRoomDao,
    private val chatDao: ChatDao,
    private val profileViewDao: ProfileViewDao,
    private val savedContactRemote: SupabaseSavedContactDataSource,
    private val roomRemote: SupabaseProximityRoomDataSource,
    private val roomMessageRemote: SupabaseRoomMessageDataSource,
    private val chatMessageRemote: SupabaseChatMessageDataSource,
    private val telemetryRemote: SupabaseTelemetryDataSource? = null,
    private val chatNotifications: com.example.airvibe.feature.chat.data.notification.ChatMessageNotificationManager? = null,
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
        pushProfileViews(ownerId)
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
            val local = roomDao.getRoomIncludingDeleted(dto.id)
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
        val deletions = savedContactDao.getPendingDeletions()
        deletions.forEach { entity ->
            val result = savedContactRemote.delete(entity.nodeId, ownerId)
            if (result.isSuccess) {
                savedContactDao.hardDelete(entity.nodeId)
            }
        }

        val pending = savedContactDao.getPendingSync()
        if (pending.isEmpty()) return
        val dtos = pending.map { it.toRemoteDto(ownerId) }
        val synced = savedContactRemote.upsert(dtos).getOrNull() ?: return
        savedContactDao.markAsSynced(synced)
    }

    private suspend fun pushRooms(ownerId: String) {
        val deletions = roomDao.getPendingDeletions()
        deletions.forEach { entity ->
            val result = roomRemote.delete(entity.id, ownerId)
            if (result.isSuccess) {
                roomDao.hardDeleteRoom(entity.id)
            }
        }

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
                if (dto.direction == "Incoming") {
                    val profile = savedContactDao.getById(dto.peerNodeId)
                    val senderName = profile?.displayName ?: "Contacto"
                    chatNotifications?.postDirectMessage(dto.peerNodeId, senderName, dto.content)
                }
            }
        }
    }

    private suspend fun pushChatMessages(ownerId: String) {
        val deletions = chatDao.getPendingDeletions()
        val deletedNodeIds = deletions.map { it.nodeId }.distinct()
        deletedNodeIds.forEach { nodeId ->
            val result = chatMessageRemote.deleteByNode(nodeId, ownerId)
            if (result.isSuccess) {
                chatDao.hardClearByNode(nodeId)
            }
        }

        val pending = chatDao.getUnsynced()
        if (pending.isEmpty()) return
        val dtos = pending.map { it.toRemoteDto(ownerId) }
        val synced = chatMessageRemote.upsert(dtos).getOrNull() ?: return
        chatDao.markAsSynced(synced)
    }

    // --------- Feature 5: Telemetría ---------

    private suspend fun pushProfileViews(ownerId: String) {
        val remote = telemetryRemote ?: return
        val pending = profileViewDao.getPendingSync(200)
        if (pending.isEmpty()) return
        val dtos = pending.map { it.toRemoteTelemetryDto() }
        val result = remote.upsert(dtos)
        result.onSuccess { _ ->
            profileViewDao.markAsSynced(pending.map { it.id })
        }
    }

    /**
     * Feature 5 — Sincronización Diferida + Analíticas Premium.
     * Descarga la `visibility_daily` del usuario Premium actual
     * y la devuelve como un [VisibilityStats] agregado. Si la
     * red falla, devuelve un objeto con `totalPendingSync`
     * útil para mostrar "X vistas en cola, sincroniza cuando
     * tengas Wi-Fi".
     */
    suspend fun pullVisibility(
        ownerId: String,
        daysWindow: Int = 30,
    ): Result<VisibilityStats> = runCatching {
        val remote = telemetryRemote
            ?: return@runCatching VisibilityStats.Empty
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val from = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -daysWindow)
        }.time
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val fromIso = isoFormat.format(from)
        val toIso = isoFormat.format(now.time)
        val remoteRows: List<RemoteVisibilityDayDto> = remote
            .fetchVisibility(ownerId, fromIso, toIso)
            .getOrNull().orEmpty()
        val byDay = remoteRows.map { dto ->
            VisibilityDay(
                dayIso = dto.day,
                views = dto.viewsCount,
                taps = dto.tapsCount,
                broadcasts = dto.broadcastsCount,
                uniqueVisitors = dto.uniqueVisitorsCount,
            )
        }
        val sevenAgo = System.currentTimeMillis() - 7L * 86_400_000
        val thirtyAgo = System.currentTimeMillis() - 30L * 86_400_000
        val views7 = profileViewDao.countViewsSince(ownerId, sevenAgo)
        val taps7 = profileViewDao.countTapsSince(ownerId, sevenAgo)
        val visitors7 = profileViewDao.countUniqueVisitorsSince(ownerId, sevenAgo)
        val views30 = profileViewDao.countViewsSince(ownerId, thirtyAgo)
        val taps30 = profileViewDao.countTapsSince(ownerId, thirtyAgo)
        VisibilityStats(
            viewsLast7Days = views7,
            tapsLast7Days = taps7,
            uniqueVisitorsLast7Days = visitors7,
            viewsLast30Days = views30,
            tapsLast30Days = taps30,
            totalPendingSync = profileViewDao.getPendingSync().size,
            byDay = byDay,
        )
    }
}
