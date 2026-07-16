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
            val local = savedContactDao.getByIdIncludingDeleted(dto.peerNodeId)
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
            val local = chatDao.getByIdIncludingDeleted(dto.id)
            if (local == null || local.isSynced) {
                chatDao.upsertAll(listOf(dto.toEntity()))
                if (dto.direction == "Incoming" && local == null) {
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
        
        // Solo sincronizar eventos con targetUserId válido (UUID, no device-*)
        val validEntries = pending.filter { isValidUuid(it.targetUserId) }
        if (validEntries.isEmpty()) {
            // Marcar como sincronizados para no reintentar
            profileViewDao.markAsSynced(pending.map { it.id })
            return
        }
        
        val dtos = validEntries.map { it.toRemoteTelemetryDto(ownerId = ownerId) }
        val result = remote.upsert(dtos)
        result.onSuccess { _ ->
            // Marcar todos como sincronizados (válidos e inválidos)
            profileViewDao.markAsSynced(pending.map { it.id })
        }
    }

    /** Valida que el string sea un UUID válido (para targetUserId de telemetría). */
    private fun isValidUuid(value: String): Boolean {
        if (value.startsWith("device-")) return false
        return try {
            java.util.UUID.fromString(value)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    /**
     * Feature 5 — Sincronización Diferida + Analíticas Premium.
     * Descarga la `visibility_daily` del usuario Premium actual
     * y suma los totales de los últimos 7 y 30 días desde Supabase.
     * Si la red falla, devuelve un objeto con `totalPendingSync`
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
        val sevenDaysAgo = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.time
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val fromIso = isoFormat.format(from)
        val toIso = isoFormat.format(now.time)
        val sevenAgoIso = isoFormat.format(sevenDaysAgo)
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
        
        // Sumar totales desde datos remotos de visibility_daily
        val last7Days = byDay.filter { it.dayIso >= sevenAgoIso }
        val views7 = last7Days.sumOf { it.views }
        val taps7 = last7Days.sumOf { it.taps }
        val visitors7 = last7Days.sumOf { it.uniqueVisitors }
        val views30 = byDay.sumOf { it.views }
        val taps30 = byDay.sumOf { it.taps }
        
        // Contar eventos locales pendientes de sincronizar (con UUID válido)
        val pendingEvents = profileViewDao.getPendingSync()
            .count { isValidUuid(it.targetUserId) }
        
        VisibilityStats(
            viewsLast7Days = views7,
            tapsLast7Days = taps7,
            uniqueVisitorsLast7Days = visitors7,
            viewsLast30Days = views30,
            tapsLast30Days = taps30,
            totalPendingSync = pendingEvents,
            byDay = byDay,
        )
    }
}
