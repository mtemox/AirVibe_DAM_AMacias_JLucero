package com.example.airvibe.feature.radar.data.device.nearby

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.ui.graphics.Color
import com.example.airvibe.feature.chat.data.device.nearby.NearbyChatMessageGateway
import com.example.airvibe.feature.chat.data.notification.MatchEngine
import com.example.airvibe.feature.radar.domain.model.PresenceStatus
import com.example.airvibe.feature.radar.domain.model.RadarNode
import com.example.airvibe.feature.radar.domain.model.RadarNodeKind
import com.example.airvibe.feature.radar.domain.repository.RadarRepository
import com.example.airvibe.feature.radar.domain.scanner.DiscoveredPeer
import com.example.airvibe.feature.radar.domain.scanner.DistanceLevel
import com.example.airvibe.feature.radar.domain.scanner.RadarScanner
import com.example.airvibe.feature.radar.domain.scanner.ScannerError
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile
import com.example.airvibe.feature.radar.domain.scanner.ScannerState
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Implementación del [RadarScanner] basada en la **Nearby Connections
 * API** de Google Play Services.
 *
 * Estrategia: [Strategy.P2P_STAR] — un host lógico coordina el
 * intercambio de perfiles con los peers cercanos. Es la elección
 * más natural para una app de networking presencial donde todos
 * los nodos quieren hablar entre sí.
 *
 * Flujo:
 *
 *  1. **Advertising**: serializa el [ScannerProfile] propio y lo
 *     publica como nombre de endpoint local.
 *  2. **Discovery**: cuando aparece un endpoint, solicitamos la
 *     conexión; al aceptarse, intercambiamos el payload y derivamos
 *     un [DiscoveredPeer] (con distancia y señal estimadas).
 *  3. Cada peer se **persiste en Room** vía [RadarRepository]; la UI
 *     se entera gracias al `Flow` reactivo del repositorio.
 *  4. Al desconectarse, el peer se elimina de la base de datos.
 *
 * Paso 5: el scanner también enruta los payloads de chat al
 * [NearbyChatMessageGateway] y notifica al [MatchEngine] para
 * las alertas inteligentes.
 */
class NearbyRadarScanner(
    private val context: Context,
    private val repository: RadarRepository,
    private val chatGateway: NearbyChatMessageGateway? = null,
    private val matchEngine: MatchEngine? = null,
) : RadarScanner {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _state = MutableStateFlow<ScannerState>(ScannerState.Idle)
    override val state: StateFlow<ScannerState> = _state.asStateFlow()

    private val _liveNodes = MutableStateFlow<List<RadarNode>>(emptyList())
    override val liveNodes: StateFlow<List<RadarNode>> = _liveNodes.asStateFlow()

    private val connectionsClient: ConnectionsClient by lazy {
        Nearby.getConnectionsClient(context)
    }

    private var currentProfile: ScannerProfile? = null
    private var serviceId: String = DEFAULT_SERVICE_ID

    private val discoveredEndpointIds = mutableSetOf<String>()
    private val connectedEndpointIds = mutableSetOf<String>()
    private val endpointDistance = mutableMapOf<String, DistanceLevel>()
    private val endpointToNodeId = mutableMapOf<String, String>()
    private val confirmedProfileIds = mutableSetOf<String>()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            val bytes = payload.asBytes() ?: return

            // Primero intentamos decodificar como payload de chat
            // (mensaje o invitación). Si lo es, el gateway se
            // encarga de persistirlo y de enlazar el endpoint con
            // el nodeId del emisor.
            val handledByChat = chatGateway?.onIncomingPayload(endpointId, bytes) ?: false
            if (handledByChat) return

            // Si no era un payload de chat, lo tratamos como
            // perfil del radar (formato v1 original).
            val profile = NearbyPayloadCodec.decode(bytes) ?: return
            val distance = endpointDistance[endpointId]
                ?: ProximityDistanceEstimator.estimate(endpointId)
            handleDiscoveredProfile(endpointId, profile, distanceLevel = distance)
        }

        override fun onPayloadTransferUpdate(
            endpointId: String,
            update: PayloadTransferUpdate,
        ) {
            // No usamos streaming; basta con la entrega de bytes.
        }
    }

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Aceptamos todas las conexiones entrantes. En pasos
            // futuros se podrá validar el authToken aquí.
            connectionsClient.acceptConnection(endpointId, payloadCallback)
                .addOnFailureListener { Log.w(TAG, "acceptConnection failed: ${it.message}") }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    connectedEndpointIds += endpointId
                    val profile = currentProfile
                    if (profile != null) {
                        val payload = Payload.fromBytes(NearbyPayloadCodec.encode(profile))
                        connectionsClient.sendPayload(endpointId, payload)
                            .addOnFailureListener { Log.w(TAG, "sendPayload failed: ${it.message}") }
                    }
                    updateDiscoveredCount()
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED,
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    discoveredEndpointIds.remove(endpointId)
                    connectedEndpointIds.remove(endpointId)
                    scope.launch {
                        removeEndpointNode(endpointId)
                        chatGateway?.unbindEndpoint(endpointId)
                    }
                    updateDiscoveredCount()
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            discoveredEndpointIds.remove(endpointId)
            connectedEndpointIds.remove(endpointId)
            scope.launch {
                removeEndpointNode(endpointId)
                chatGateway?.unbindEndpoint(endpointId)
            }
            updateDiscoveredCount()
        }
    }

    private val endpointCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (info.serviceId != serviceId) return
            if (endpointId in discoveredEndpointIds) return
            discoveredEndpointIds += endpointId
            endpointDistance[endpointId] = ProximityDistanceEstimator.onEndpointFound(endpointId)
            connectionsClient.requestConnection(LOCAL_ENDPOINT_NAME, endpointId, connectionCallback)
                .addOnFailureListener { Log.w(TAG, "requestConnection failed: ${it.message}") }
            updateDiscoveredCount()
        }

        override fun onEndpointLost(endpointId: String) {
            discoveredEndpointIds.remove(endpointId)
            connectedEndpointIds.remove(endpointId)
            endpointDistance.remove(endpointId)
            ProximityDistanceEstimator.onEndpointLost(endpointId)
            scope.launch {
                removeEndpointNode(endpointId)
                chatGateway?.unbindEndpoint(endpointId)
            }
            updateDiscoveredCount()
        }
    }

    @RequiresPermission(allOf = [android.Manifest.permission.BLUETOOTH_ADVERTISE])
    override suspend fun start(profile: ScannerProfile): Boolean = mutex.withLock {
        if (_state.value is ScannerState.Active || _state.value is ScannerState.Starting) {
            return@withLock true
        }
        _state.value = ScannerState.Starting
        currentProfile = profile
        // Todos los dispositivos deben compartir el mismo serviceId
        // para descubrirse; el perfil único viaja en el payload.
        serviceId = DEFAULT_SERVICE_ID

        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()

        return@withLock try {
            connectionsClient.startAdvertising(
                LOCAL_ENDPOINT_NAME,
                serviceId,
                connectionCallback,
                advertisingOptions,
            ).await()

            connectionsClient.startDiscovery(serviceId, endpointCallback, discoveryOptions).await()

            repository.clearDiscoveredNodes()
            repository.removePendingNodes()
            discoveredEndpointIds.clear()
            connectedEndpointIds.clear()
            endpointDistance.clear()
            endpointToNodeId.clear()
            confirmedProfileIds.clear()
            ProximityDistanceEstimator.reset()
            _liveNodes.value = emptyList()
            matchEngine?.resetDedupe()
            _state.value = ScannerState.Active(discovered = 0)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "start() failed: ${t.message}", t)
            _state.value = ScannerState.Error(mapError(t))
            stopInternal()
            false
        }
    }

    @RequiresPermission(allOf = [android.Manifest.permission.BLUETOOTH_ADVERTISE])
    override suspend fun stop() = mutex.withLock {
        stopInternal()
    }

    @RequiresPermission(allOf = [android.Manifest.permission.BLUETOOTH_ADVERTISE])
    override suspend fun updateProfile(profile: ScannerProfile) {
        currentProfile = profile
        val payload = Payload.fromBytes(NearbyPayloadCodec.encode(profile))
        connectedEndpointIds.forEach { endpointId ->
            connectionsClient.sendPayload(endpointId, payload)
                .addOnFailureListener { Log.w(TAG, "updateProfile sendPayload failed: ${it.message}") }
        }
    }

    private fun stopInternal() {
        runCatching { connectionsClient.stopAdvertising() }
        runCatching { connectionsClient.stopDiscovery() }
        runCatching { connectionsClient.stopAllEndpoints() }
        val endpoints = discoveredEndpointIds.toList()
        discoveredEndpointIds.clear()
        connectedEndpointIds.clear()
        endpointToNodeId.clear()
        confirmedProfileIds.clear()
        _liveNodes.value = emptyList()
        scope.launch { repository.clearDiscoveredNodes() }
        scope.launch { repository.removePendingNodes() }
        scope.launch {
            endpoints.forEach { chatGateway?.unbindEndpoint(it) }
        }
        _state.value = ScannerState.Idle
    }

    /**
     * Maneja un perfil recién recibido vía payload. Si el endpoint
     * tiene información de distancia, la aprovechamos para mejorar
     * la ubicación del nodo en el radar.
     */
    private fun handleDiscoveredProfile(
        endpointId: String,
        profile: ScannerProfile,
        distanceLevel: DistanceLevel,
    ) {
        if (profile.id == currentProfile?.id) return

        if (profile.id in confirmedProfileIds) {
            endpointToNodeId[endpointId] = profile.id
            scope.launch {
                repository.removePendingNodes()
                chatGateway?.bindEndpoint(profile.id, endpointId)
            }
            updateDiscoveredCount()
            return
        }

        val signal = DiscoveredPeer.signalFor(distanceLevel)
        val peer = DiscoveredPeer(
            endpointId = endpointId,
            profile = profile,
            distanceLevel = distanceLevel,
            signalStrength = signal,
        )
        val accent = colorFor(endpointId)
        val node = peer.toRadarNode(accentColor = accent)
        confirmedProfileIds += profile.id
        replaceLiveNode(endpointId, node)
        scope.launch {
            purgePendingNodes()
            repository.upsertNode(node)
            endpointToNodeId[endpointId] = profile.id
        }

        // Vinculamos el endpoint con el nodeId estable para que
        // el gateway de chat pueda responder mensajes.
        scope.launch { chatGateway?.bindEndpoint(profile.id, endpointId) }

        // Evaluamos el match contra los criterios activos. Si
        // coincide, el MatchEngine emite al SharedFlow que
        // MatchNotificationManager está escuchando.
        matchEngine?.onPeerDiscovered(profile, signalStrength = signal)

        updateDiscoveredCount()
    }

    fun ensurePeerNode(nodeId: String, endpointId: String) {
        if (nodeId in confirmedProfileIds) {
            endpointToNodeId[endpointId] = nodeId
            return
        }
        val distance = endpointDistance[endpointId] ?: DistanceLevel.UNKNOWN
        val signal = DiscoveredPeer.signalFor(distance)
        val angle = (nodeId.hashCode().toLong() and 0xFFFFFFFFL)
            .let { ((it % 360).toInt()).toFloat() }
            .coerceIn(0f, 359.9f)
        val node = RadarNode(
            id = nodeId,
            displayName = "Usuario cercano",
            status = "Disponible por proximidad",
            detail = "Perfil recibido por conexión local",
            kind = RadarNodeKind.Person,
            presence = PresenceStatus.Online,
            angleDegrees = angle,
            distanceNormalized = distance.normalized,
            signalStrength = signal,
            accentColor = colorFor(endpointId),
        )
        confirmedProfileIds += nodeId
        endpointToNodeId[endpointId] = nodeId
        replaceLiveNode(endpointId, node)
        scope.launch {
            purgePendingNodes()
            repository.upsertNode(node)
        }
    }

    private fun replaceLiveNode(endpointId: String, node: RadarNode) {
        _liveNodes.update { current ->
            (current
                .filterNot { it.id == pendingNodeId(endpointId) || it.id == node.id }
                .filterNot { stalePendingForProfile(it.id, node.id) }) + node
        }
    }

    private fun stalePendingForProfile(existingId: String, profileId: String): Boolean {
        return existingId.startsWith("pending-") && endpointToNodeId.values.contains(profileId)
    }

    private suspend fun purgePendingNodes() {
        _liveNodes.update { current -> current.filterNot { it.id.startsWith("pending-") } }
        repository.removePendingNodes()
    }

    private fun removeLiveNode(endpointId: String) {
        val nodeId = endpointToNodeId[endpointId] ?: pendingNodeId(endpointId)
        confirmedProfileIds.remove(nodeId)
        _liveNodes.update { current ->
            current.filterNot { it.id == nodeId || it.id == pendingNodeId(endpointId) }
        }
    }

    private suspend fun removeEndpointNode(endpointId: String) {
        val nodeId = endpointToNodeId.remove(endpointId)
        if (nodeId != null) {
            confirmedProfileIds.remove(nodeId)
            _liveNodes.update { current -> current.filterNot { it.id == nodeId } }
            repository.removeNode(nodeId)
        }
        repository.removeNode(pendingNodeId(endpointId))
    }

    private fun pendingNodeId(endpointId: String) = "pending-$endpointId"

    private fun updateDiscoveredCount() {
        val current = _state.value
        if (current is ScannerState.Active) {
            _state.value = current.copy(discovered = confirmedProfileIds.size)
        }
    }

    private fun mapError(t: Throwable): ScannerError = when {
        t.message?.contains("permission", ignoreCase = true) == true ||
            t.message?.contains("MISSING_PERMISSION", ignoreCase = true) == true ->
            ScannerError.MissingPermissions
        t.message?.contains("wifi", ignoreCase = true) == true ->
            ScannerError.MissingPermissions
        t.message?.contains("bluetooth", ignoreCase = true) == true ->
            ScannerError.BluetoothUnavailable
        t.message?.contains("location", ignoreCase = true) == true ->
            ScannerError.LocationUnavailable
        else -> ScannerError.Unknown(t.message.orEmpty())
    }

    /**
     * Color estable por endpoint: garantiza que un mismo dispositivo
     * mantenga su acento a lo largo de la sesión.
     */
    private fun colorFor(endpointId: String): Color {
        val palette = listOf(
            Color(0xFF6366F1),
            Color(0xFF06B6D4),
            Color(0xFF10B981),
            Color(0xFFF59E0B),
            Color(0xFFEC4899),
            Color(0xFF8B5CF6),
            Color(0xFF0EA5E9),
        )
        val index = (endpointId.hashCode().toLong() and 0x7FFFFFFF).toInt() % palette.size
        return palette[index]
    }

    /**
     * Devuelve un snapshot inmutable de los endpoints conectados
     * en este momento. El [NearbyChatMessageGateway] lo usa para
     * el broadcast.
     */
    fun connectedEndpoints(): Set<String> = connectedEndpointIds.toSet()

    companion object {
        private const val TAG = "NearbyRadarScanner"
        private const val LOCAL_ENDPOINT_NAME = "AirVibeUser"
        private const val DEFAULT_SERVICE_ID = "com.example.airvibe.radar"

        // P2P_STAR es ideal para una red de contactos donde cada
        // nodo puede hablar con todos los demás. Si en el futuro
        // se prefiere un modelo jerárquico estricto, basta con
        // cambiar a P2P_CLUSTER.
        private val STRATEGY: Strategy = Strategy.P2P_STAR
    }
}
