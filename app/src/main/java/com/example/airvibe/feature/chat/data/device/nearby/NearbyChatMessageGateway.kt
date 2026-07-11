package com.example.airvibe.feature.chat.data.device.nearby

import android.content.Context
import android.util.Log
import com.example.airvibe.feature.chat.data.repository.ChatRepositoryImpl
import com.example.airvibe.feature.chat.domain.model.MessageKind
import com.example.airvibe.feature.chat.domain.scanner.ChatMessageGateway
import com.example.airvibe.feature.radar.data.device.nearby.NearbyPayloadCodec
import com.example.airvibe.feature.radar.domain.repository.RadarRepository
import com.example.airvibe.feature.radar.domain.model.PresenceStatus
import com.example.airvibe.feature.radar.domain.model.RadarNode
import com.example.airvibe.feature.radar.domain.model.RadarNodeKind
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

/**
 * Implementación del [ChatMessageGateway] basada en la API de
 * **Nearby Connections**.
 *
 * Responsabilidades:
 *
 *  - Mantener el mapping `nodeId → endpointId` (alimentado por
 *    el scanner cuando recibe un perfil).
 *  - Codificar los mensajes a bytes y enviarlos por
 *    [ConnectionsClient.sendPayload].
 *  - Decodificar los payloads entrantes y delegar la
 *    persistencia al [ChatRepositoryImpl] (Single Source of
 *    Truth).
 *
 * El gateway **no** mantiene estado de UI: la presentación
 * consume `Flow`s de Room y reacciona automáticamente a las
 * inserciones que hacemos aquí.
 */
class NearbyChatMessageGateway(
    private val context: Context,
    private val chatRepository: ChatRepositoryImpl,
    private val localUserIdProvider: () -> String,
    private val connectedEndpointsProvider: () -> Set<String>,
    private val radarRepository: RadarRepository,
    private val onPeerBound: ((nodeId: String, endpointId: String) -> Unit)? = null,
) : ChatMessageGateway {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    /** Mapeo `nodeId → endpointId` activo durante la sesión. */
    private val nodeToEndpoint = mutableMapOf<String, String>()
    private val endpointToNode = mutableMapOf<String, String>()

    private val connectionsClient: ConnectionsClient by lazy {
        Nearby.getConnectionsClient(context)
    }

    /**
     * Vincula un endpoint recién descubierto con el id estable
     * del perfil. Lo llama el scanner tras decodificar un payload
     * `v1|profile|…`.
     */
    suspend fun bindEndpoint(nodeId: String, endpointId: String) = mutex.withLock {
        // Si el endpoint ya estaba mapeado a otro nodo, lo limpiamos.
        val previousNode = endpointToNode[endpointId]
        if (previousNode != null && previousNode != nodeId) {
            nodeToEndpoint.remove(previousNode)
        }
        nodeToEndpoint[nodeId] = endpointId
        endpointToNode[endpointId] = nodeId
    }

    /** Limpia el mapping de un endpoint que se desconectó. */
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
        val bytes = NearbyChatPayloadCodec.encode(
            kind = NearbyChatPayloadKind.Chat,
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

    override suspend fun broadcast(text: String): Int {
        val endpoints = connectedEndpointsProvider()
        if (endpoints.isEmpty()) return 0

        val bytes = NearbyChatPayloadCodec.encode(
            kind = NearbyChatPayloadKind.GroupInvite,
            messageId = UUID.randomUUID().toString(),
            senderNodeId = localUserIdProvider(),
            text = text,
            createdAtMillis = System.currentTimeMillis(),
        )

        val payload = Payload.fromBytes(bytes)
        val result = runCatching { connectionsClient.sendPayload(endpoints.toList(), payload) }
        if (result.isFailure) {
            Log.w(TAG, "broadcast: sendPayload failed: ${result.exceptionOrNull()?.message}")
        }

        // Por cada peer con el que tengamos mapping, dejamos
        // constancia en Room. Si el peer no está mapeado (porque
        // su perfil no llegó todavía) no podemos asociarlo a un
        // chat, pero el broadcast físico sí ocurre.
        val peers = mutex.withLock { nodeToEndpoint.keys.toList() }
        peers.forEach { peerNodeId ->
            scope.launch {
                chatRepository.persistOutgoingInvite(peerNodeId, text)
            }
        }
        return peers.size.coerceAtMost(endpoints.size)
    }

    override fun onIncomingPayload(endpointId: String, bytes: ByteArray): Boolean {
        if (!NearbyChatPayloadCodec.looksLikeChatPayload(bytes)) return false
        val decoded = NearbyChatPayloadCodec.decode(bytes) ?: return false

        val senderNodeId = decoded.senderNodeId
        val kind = when (decoded.kind) {
            NearbyChatPayloadKind.Chat -> MessageKind.Text
            NearbyChatPayloadKind.GroupInvite -> MessageKind.GroupInvite
        }

        // Vinculamos el endpoint ↔ nodo para futuros envíos.
        scope.launch {
            bindEndpoint(senderNodeId, endpointId)
            ensureRadarNode(senderNodeId, endpointId)
        }

        scope.launch {
            chatRepository.persistIncoming(
                peerNodeId = senderNodeId,
                text = decoded.text,
                kind = kind,
                createdAt = decoded.createdAtMillis,
            )
        }
        return true
    }

    /**
     * Helper usado por tests o por el scanner para confirmar si
     * un peer está conectado. Mantenido público para no romper
     * el encapsulamiento desde otros archivos del paquete.
     */
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
    }
}
