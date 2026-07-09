package com.example.airvibe.feature.radar.data.device.nearby

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.ui.graphics.Color
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
 */
class NearbyRadarScanner(
    private val context: Context,
    private val repository: RadarRepository,
) : RadarScanner {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _state = MutableStateFlow<ScannerState>(ScannerState.Idle)
    override val state: StateFlow<ScannerState> = _state.asStateFlow()

    private val connectionsClient: ConnectionsClient by lazy {
        Nearby.getConnectionsClient(context)
    }

    private var currentProfile: ScannerProfile? = null
    private var serviceId: String = DEFAULT_SERVICE_ID

    private val discoveredEndpointIds = mutableSetOf<String>()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            val bytes = payload.asBytes() ?: return
            val profile = NearbyPayloadCodec.decode(bytes) ?: return
            handleDiscoveredProfile(endpointId, profile, distanceLevel = DistanceLevel.UNKNOWN)
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
                    scope.launch { repository.removeNode(endpointId) }
                    updateDiscoveredCount()
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            discoveredEndpointIds.remove(endpointId)
            scope.launch { repository.removeNode(endpointId) }
            updateDiscoveredCount()
        }
    }

    private val endpointCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (info.serviceId != serviceId) return
            discoveredEndpointIds += endpointId
            connectionsClient.requestConnection(LOCAL_ENDPOINT_NAME, endpointId, connectionCallback)
                .addOnFailureListener { Log.w(TAG, "requestConnection failed: ${it.message}") }
        }

        override fun onEndpointLost(endpointId: String) {
            discoveredEndpointIds.remove(endpointId)
            scope.launch { repository.removeNode(endpointId) }
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
        serviceId = computeServiceId(profile.id)

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
            discoveredEndpointIds.clear()
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
        // En pasos futuros podemos reenviar el payload a todos
        // los endpoints conectados. Por ahora basta con guardar
        // la referencia para los próximos `sendPayload`.
    }

    private fun stopInternal() {
        runCatching { connectionsClient.stopAdvertising() }
        runCatching { connectionsClient.stopDiscovery() }
        runCatching { connectionsClient.stopAllEndpoints() }
        discoveredEndpointIds.clear()
        scope.launch { repository.clearDiscoveredNodes() }
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
        val signal = DiscoveredPeer.signalFor(distanceLevel)
        val peer = DiscoveredPeer(
            endpointId = endpointId,
            profile = profile,
            distanceLevel = distanceLevel,
            signalStrength = signal,
        )
        val accent = colorFor(endpointId)
        val node = peer.toRadarNode(accentColor = accent)
        scope.launch { repository.upsertNode(node) }
        updateDiscoveredCount()
    }

    private fun updateDiscoveredCount() {
        val current = _state.value
        if (current is ScannerState.Active) {
            _state.value = current.copy(discovered = discoveredEndpointIds.size)
        }
    }

    private fun mapError(t: Throwable): ScannerError = when {
        t.message?.contains("permission", ignoreCase = true) == true ->
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

    private fun computeServiceId(profileId: String): String {
        // Nearby Connections exige un serviceId de máximo 32 bytes
        // y solo letras minúsculas, dígitos, '_' o '.'.
        val sanitized = profileId.lowercase()
            .filter { it.isLetterOrDigit() || it == '_' || it == '.' }
            .take(24)
        return if (sanitized.isNotEmpty()) "$sanitized.airvibe" else DEFAULT_SERVICE_ID
    }

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
