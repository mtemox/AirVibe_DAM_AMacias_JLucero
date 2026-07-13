package com.example.airvibe.feature.chat.data.device.nearby

import android.content.Context
import android.util.Log
import com.example.airvibe.feature.chat.data.notification.RoomInviteNotificationManager
import com.example.airvibe.feature.chat.data.repository.ChatRepositoryImpl
import com.example.airvibe.feature.chat.data.repository.ProximityRoomRepositoryImpl
import com.example.airvibe.feature.chat.domain.model.MessageKind
import com.example.airvibe.feature.chat.domain.scanner.ChatMessageGateway
import com.example.airvibe.feature.radar.domain.model.PresenceStatus
import com.example.airvibe.feature.radar.domain.model.RadarNode
import com.example.airvibe.feature.radar.domain.model.RadarNodeKind
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile
import com.example.airvibe.feature.radar.domain.repository.RadarRepository
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NearbyChatMessageGateway(
    private val context: Context,
    private val chatRepository: ChatRepositoryImpl,
    private val roomRepository: ProximityRoomRepositoryImpl,
    private val localUserIdProvider: () -> String,
    private val localDisplayNameProvider: () -> String,
    private val localProfileProvider: () -> ScannerProfile,
    private val connectedEndpointsProvider: () -> Set<String>,
    private val radarRepository: RadarRepository,
    private val onPeerBound: ((nodeId: String, endpointId: String) -> Unit)? = null,
) : ChatMessageGateway {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val nodeToEndpoint = mutableMapOf<String, String>()
    private val endpointToNode = mutableMapOf<String, String>()

    private val connectionsClient: ConnectionsClient by lazy {
        Nearby.getConnectionsClient(context)
    }

    suspend fun bindEndpoint(nodeId: String, endpointId: String) = mutex.withLock {
        val previousNode = endpointToNode[endpointId]
        if (previousNode != null && previousNode != nodeId) {
            nodeToEndpoint.remove(previousNode)
        }
        nodeToEndpoint[nodeId] = endpointId
        endpointToNode[endpointId] = nodeId
    }

    suspend fun unbindEndpoint(endpointId: String) = mutex.withLock {
        val nodeId = endpointToNode.remove(endpointId)
        if (nodeId != null) nodeToEndpoint.remove(nodeId)
    }

    override suspend fun sendMessage(targetNodeId: String, text: String): Boolean {
        val endpointId = mutex.withLock { nodeToEndpoint[targetNodeId] }
            ?: run {
                Log.w(TAG, "sendMessage: no endpoint for nodeId=$targetNodeId")
                return false
            }
        val bytes = NearbyChatPayloadCodec.encodeChat(
            messageId = UUID.randomUUID().toString(),
            senderNodeId = localUserIdProvider(),
            text = text,
            createdAtMillis = System.currentTimeMillis(),
        )
        return try {
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
            true
        } catch (t: Throwable) {
            Log.w(TAG, "sendPayload failed: ${t.message}")
            false
        }
    }

    override suspend fun broadcast(text: String): Int =
        broadcastRoomInvite(text, UUID.randomUUID().toString())

    override suspend fun broadcastRoomInvite(text: String, roomId: String): Int {
        val endpoints = connectedEndpointsProvider()
        if (endpoints.isEmpty()) return 0

        val bytes = NearbyChatPayloadCodec.encodeInvite(
            messageId = UUID.randomUUID().toString(),
            senderNodeId = localUserIdProvider(),
            senderName = localProfileProvider().displayName,
            text = text,
            createdAtMillis = System.currentTimeMillis(),
            roomId = roomId,
        )
        val payload = Payload.fromBytes(bytes)
        val result = runCatching { connectionsClient.sendPayload(endpoints.toList(), payload) }
        if (result.isFailure) {
            Log.w(TAG, "broadcastRoomInvite failed: ${result.exceptionOrNull()?.message}")
        }
        return endpoints.size
    }

    override suspend fun sendRoomMessage(roomId: String, text: String, messageId: String): Boolean {
        val localId = localUserIdProvider()
        val endpoints = resolveRoomEndpoints(localId)
        if (endpoints.isEmpty()) return false
        val bytes = NearbyChatPayloadCodec.encodeRoomMessage(
            messageId = messageId,
            senderNodeId = localId,
            senderName = localProfileProvider().displayName,
            roomId = roomId,
            text = text,
            createdAtMillis = System.currentTimeMillis(),
        )
        val payload = Payload.fromBytes(bytes)
        var delivered = 0
        endpoints.forEach { endpointId ->
            val sent = runCatching {
                connectionsClient.sendPayload(endpointId, payload)
            }.isSuccess
            if (sent) delivered++
            Log.d(TAG, "sendRoomMessage to $endpointId: $sent")
        }
        Log.d(TAG, "sendRoomMessage delivered=$delivered/${endpoints.size}")
        return delivered > 0
    }

    private suspend fun resolveRoomEndpoints(localNodeId: String): List<String> = mutex.withLock {
        // endpointToNode.keys are endpointIds (not nodeIds) — use them directly
        val fromBindings = endpointToNode.keys
        val fromScanner = connectedEndpointsProvider()
        (fromBindings + fromScanner).distinct()
    }

    override suspend fun sendFriendAdd(targetNodeId: String): Boolean {
        val endpointId = mutex.withLock { nodeToEndpoint[targetNodeId] }
            ?: return false
        val ownProfile = localProfileProvider()
        val bytes = NearbyChatPayloadCodec.encodeFriendAdd(
            messageId = UUID.randomUUID().toString(),
            senderNodeId = localUserIdProvider(),
            displayName = ownProfile.displayName,
            status = ownProfile.status,
            detail = ownProfile.status,
            tags = ownProfile.tags,
            createdAtMillis = System.currentTimeMillis(),
        )
        return runCatching {
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
        }.isSuccess
    }

    override fun onIncomingPayload(endpointId: String, bytes: ByteArray): Boolean {
        if (!NearbyChatPayloadCodec.looksLikeChatPayload(bytes)) return false
        val decoded = NearbyChatPayloadCodec.decode(bytes) ?: return false
        val senderNodeId = decoded.senderNodeId

        scope.launch {
            bindEndpoint(senderNodeId, endpointId)
            ensureRadarNode(senderNodeId, endpointId)
        }

        when (decoded.kind) {
            NearbyChatPayloadKind.Chat -> {
                scope.launch {
                    chatRepository.persistIncoming(
                        peerNodeId = senderNodeId,
                        text = decoded.text,
                        kind = MessageKind.Text,
                        createdAt = decoded.createdAtMillis,
                    )
                }
            }
            NearbyChatPayloadKind.GroupInvite -> {
                val roomId = decoded.roomId
                if (roomId.isNullOrBlank()) {
                    scope.launch {
                        chatRepository.persistIncoming(
                            peerNodeId = senderNodeId,
                            text = decoded.text,
                            kind = MessageKind.GroupInvite,
                            createdAt = decoded.createdAtMillis,
                        )
                    }
                } else {
                    scope.launch {
                        val hostName = decoded.senderDisplayName?.takeIf { it.isNotBlank() }
                            ?: radarRepository.getProfile(senderNodeId)?.displayName
                            ?: "Usuario cercano"
                        roomRepository.receiveInvite(
                            roomId = roomId,
                            title = decoded.text,
                            hostNodeId = senderNodeId,
                            hostName = hostName,
                            createdAt = decoded.createdAtMillis,
                        )
                        RoomInviteNotificationManager.postInvite(
                            context = context,
                            roomId = roomId,
                            hostName = hostName,
                            roomTitle = decoded.text,
                        )
                    }
                }
            }
            NearbyChatPayloadKind.RoomMessage -> {
                val roomId = decoded.roomId ?: return false
                if (decoded.senderNodeId == localUserIdProvider()) return true
                scope.launch {
                    val senderName = decoded.senderDisplayName?.takeIf { it.isNotBlank() }
                        ?: radarRepository.getProfile(senderNodeId)?.displayName
                        ?: "Usuario cercano"
                    val now = System.currentTimeMillis()
                    val remoteTime = decoded.createdAtMillis
                    val normalizedTime =
                        if (kotlin.math.abs(remoteTime - now) > MAX_CLOCK_SKEW_MS) now else remoteTime
                    roomRepository.persistIncomingMessage(
                        roomId = roomId,
                        senderNodeId = senderNodeId,
                        senderName = senderName,
                        text = decoded.text,
                        createdAt = normalizedTime,
                        messageId = decoded.messageId,
                    )
                }
            }
            NearbyChatPayloadKind.FriendAdd -> {
                scope.launch {
                    val profile = com.example.airvibe.feature.radar.domain.model.PersonProfile(
                        id = senderNodeId,
                        displayName = decoded.senderDisplayName ?: "Usuario cercano",
                        headline = decoded.senderStatus ?: "Disponible",
                        bio = decoded.senderDetail ?: "",
                        status = decoded.senderStatus ?: "Disponible",
                        presence = com.example.airvibe.feature.radar.domain.model.PresenceStatus.Online,
                        tags = decoded.senderTags,
                        distanceMeters = 0,
                        isFavorite = true,
                        accentHue = 0f,
                    )
                    radarRepository.saveContact(profile, addedByPeer = true)
                    com.example.airvibe.core.di.ServiceLocator.requestContactsSync()
                }
            }
        }
        return true
    }

    fun isNodeReachable(nodeId: String): Boolean = nodeToEndpoint.containsKey(nodeId)

    private suspend fun ensureRadarNode(nodeId: String, endpointId: String) {
        if (radarRepository.getProfile(nodeId) != null) {
            onPeerBound?.invoke(nodeId, endpointId)
            return
        }
        val angle = (nodeId.hashCode().toLong() and 0xFFFFFFFFL)
            .let { ((it % 360).toInt()).toFloat() }
            .coerceIn(0f, 359.9f)
        val palette = listOf(
            0xFF6366F1, 0xFF06B6D4, 0xFF10B981, 0xFFF59E0B,
            0xFFEC4899, 0xFF8B5CF6, 0xFF0EA5E9,
        )
        val accent = androidx.compose.ui.graphics.Color(
            palette[(nodeId.hashCode().toLong() and 0x7FFFFFFF).toInt() % palette.size].toInt(),
        )
        val node = RadarNode(
            id = nodeId,
            displayName = "Usuario cercano",
            status = "Disponible por proximidad",
            detail = "Perfil recibido por conexión local",
            kind = RadarNodeKind.Person,
            presence = PresenceStatus.Online,
            angleDegrees = angle,
            distanceNormalized = 0.45f,
            signalStrength = 0.72f,
            accentColor = accent,
        )
        radarRepository.upsertNode(node)
        onPeerBound?.invoke(nodeId, endpointId)
    }

    companion object {
        private const val TAG = "NearbyChatGateway"
        private const val MAX_CLOCK_SKEW_MS = 5_000L
    }
}
