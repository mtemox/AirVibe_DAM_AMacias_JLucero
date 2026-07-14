package com.example.airvibe.feature.chat.data.repository

import com.example.airvibe.feature.chat.data.local.dao.ChatDao
import com.example.airvibe.feature.chat.data.mapper.ChatMessageMapper.toDomain
import com.example.airvibe.feature.chat.data.mapper.ChatMessageMapper.toEntity
import com.example.airvibe.feature.chat.data.mapper.ChatMessageMapper.toSummary
import com.example.airvibe.feature.chat.domain.model.ChatMessage
import com.example.airvibe.feature.chat.domain.model.MessageKind
import com.example.airvibe.feature.chat.domain.model.MessageStatus
import com.example.airvibe.feature.chat.domain.repository.BroadcastResult
import com.example.airvibe.feature.chat.domain.repository.ChatRepository
import com.example.airvibe.feature.chat.domain.repository.ConversationSummary
import com.example.airvibe.feature.chat.domain.repository.ProximityRoomRepository
import com.example.airvibe.feature.chat.domain.scanner.ChatMessageGateway
import com.example.airvibe.feature.radar.domain.repository.RadarRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Implementación **offline-first** del [ChatRepository].
 *
 * Contrato con la arquitectura:
 *
 *  - El único writer de `chat_messages` es esta clase. Tanto los
 *    mensajes que envía el usuario como los que llegan por
 *    Bluetooth pasan por aquí.
 *  - La UI nunca recibe un payload crudo de Nearby: observa los
 *    `Flow` de Room. Esto cumple el requisito de Clean
 *    Architecture del paso 5.
 *  - El envío al peer se delega al [ChatMessageGateway] (capa
 *    de transporte). Si la entrega falla, actualizamos el
 *    `status` del mensaje a [MessageStatus.Failed].
 */
class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val gateway: ChatMessageGateway,
    private val roomRepository: ProximityRoomRepository,
    private val localUserIdProvider: () -> String,
    private val localDisplayNameProvider: () -> String,
    private val radarRepository: RadarRepository? = null,
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeConversation(peerNodeId: String): Flow<List<ChatMessage>> =
        chatDao.observeByNode(peerNodeId).map { rows ->
            rows.map { it.toDomain() }
        }

    override fun observeConversations(): Flow<List<ConversationSummary>> =
        chatDao.observeConversationSummaries().map { rows ->
            rows.map { it.toSummary() }
        }

    override fun observeUnsyncedCount(): Flow<Int> = chatDao.observeUnsyncedCount()

    override fun observeUnreadConversationCount(): Flow<Int> =
        chatDao.observeUnreadConversationCount()

    override suspend fun sendMessage(peerNodeId: String, text: String): ChatMessage {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "sendMessage: text cannot be blank" }

        val now = System.currentTimeMillis()
        val outgoing = ChatMessage(
            id = UUID.randomUUID().toString(),
            nodeId = peerNodeId,
            text = trimmed,
            direction = com.example.airvibe.feature.chat.domain.model.MessageDirection.Outgoing,
            status = MessageStatus.Sending,
            kind = MessageKind.Text,
            createdAt = now,
            isSynced = false,
        )
        chatDao.insert(outgoing.toEntity())

        val delivered = try {
            gateway.sendMessage(peerNodeId, trimmed)
        } catch (t: Throwable) {
            false
        }

        val finalStatus = if (delivered) MessageStatus.Sent else MessageStatus.Failed
        chatDao.setStatus(outgoing.id, finalStatus.name)
        return outgoing.copy(status = finalStatus)
    }

    override suspend fun broadcast(text: String): BroadcastResult {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "broadcast: text cannot be blank" }
        val hostId = localUserIdProvider()
        val hostName = localDisplayNameProvider()
        val room = roomRepository.createHostRoom(
            title = trimmed,
            hostNodeId = hostId,
            hostName = hostName,
        )
        // Feature 4: registrar la sala como un nodo `Group` en el
        // radar. El id es determinista (`room:<roomId>`) para que
        // el `RadarIntentToken` la pinte con el icono `Groups`
        // y el color violeta de las salas.
        try {
            val roomNode = com.example.airvibe.feature.radar.domain.model.RadarNode(
                id = "room:${room.id}",
                displayName = room.title,
                status = "Anfitrión: $hostName",
                detail = "Sala de proximidad — ${room.title}",
                kind = com.example.airvibe.feature.radar.domain.model.RadarNodeKind.Group,
                presence = com.example.airvibe.feature.radar.domain.model.PresenceStatus.Online,
                angleDegrees = (room.id.hashCode().toLong() and 0xFFFFFFFFL)
                    .let { ((it % 360).toInt()).toFloat() }
                    .coerceIn(0f, 359.9f),
                distanceNormalized = 0.18f,
                signalStrength = 0.95f,
                accentColor = androidx.compose.ui.graphics.Color(0xFF8B5CF6),
                tags = listOf("Sala", room.id),
            )
            radarRepository?.upsertNode(roomNode)
        } catch (_: Throwable) {
            // No bloquear el broadcast si la creación del nodo falla.
        }
        val count = try {
            gateway.broadcastRoomInvite(trimmed, room.id)
        } catch (_: Throwable) {
            0
        }
        return BroadcastResult(recipientCount = count, roomId = room.id)
    }

    override suspend fun clearConversation(peerNodeId: String) {
        chatDao.clearByNode(peerNodeId)
    }

    override suspend fun sendRoomMessage(
        roomId: String,
        text: String,
    ): com.example.airvibe.feature.chat.domain.model.RoomMessage {
        val message = roomRepository.insertOutgoingMessage(roomId, text)
        gateway.sendRoomMessage(roomId, message.text, message.id)
        return message
    }

    /**
     * Hook invocado por el [ChatMessageGateway] cuando llega un
     * mensaje entrante. Se persiste como [MessageKind.Text]
     * (o [MessageKind.GroupInvite] si así lo indica el payload)
     * con estado [MessageStatus.Synced] provisional hasta que el
     * worker de Supabase lo replique.
     */
    suspend fun persistIncoming(
        peerNodeId: String,
        text: String,
        kind: MessageKind,
        createdAt: Long = System.currentTimeMillis(),
    ): ChatMessage {
        val incoming = ChatMessage(
            id = UUID.randomUUID().toString(),
            nodeId = peerNodeId,
            text = text,
            direction = com.example.airvibe.feature.chat.domain.model.MessageDirection.Incoming,
            status = MessageStatus.Sent,
            kind = kind,
            createdAt = createdAt,
            isSynced = false,
        )
        chatDao.insert(incoming.toEntity())
        return incoming
    }

    /**
     * Llamado por el [ChatMessageGateway] cuando un broadcast
     * propio se entrega a un peer concreto. Persiste un mensaje
     * saliente tipo [MessageKind.GroupInvite] para que aparezca
     * en el chat con ese peer.
     */
    suspend fun persistOutgoingInvite(peerNodeId: String, text: String): ChatMessage {
        val now = System.currentTimeMillis()
        val outgoing = ChatMessage(
            id = UUID.randomUUID().toString(),
            nodeId = peerNodeId,
            text = text,
            direction = com.example.airvibe.feature.chat.domain.model.MessageDirection.Outgoing,
            status = MessageStatus.Sent,
            kind = MessageKind.GroupInvite,
            createdAt = now,
            isSynced = false,
        )
        chatDao.insert(outgoing.toEntity())
        return outgoing
    }

    /** Marca como sincronizados los mensajes que el worker empuje. */
    suspend fun markAsSynced(ids: List<String>) {
        if (ids.isEmpty()) return
        chatDao.markAsSynced(ids)
    }
}
