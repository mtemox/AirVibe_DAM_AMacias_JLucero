package com.example.airvibe.feature.radar.domain.repository

import com.example.airvibe.feature.radar.domain.model.HandshakeRequest
import com.example.airvibe.feature.radar.domain.model.PersonProfile
import com.example.airvibe.feature.radar.domain.model.RadarNode
import kotlinx.coroutines.flow.Flow

/**
 * Contrato que el dominio exige para alimentar la pantalla del radar.
 * La capa de presentación nunca debe conocer la fuente de datos.
 *
 * Esta interfaz es la **única fuente de verdad** de la UI: el
 * scanner escribe aquí cuando descubre un peer, la presentación
 * observa el `Flow` para refrescar la pantalla.
 */
interface RadarRepository {
    /** Flujo en vivo con los nodos actualmente visibles en el radar. */
    fun observeRadarNodes(): Flow<List<RadarNode>>

    /** Recupera el detalle completo de un nodo por identificador. */
    suspend fun getProfile(nodeId: String): PersonProfile?

    /** Marca/desmarca un nodo como favorito. */
    suspend fun toggleFavorite(nodeId: String): Boolean

    /** Añade un nodo a amigos sin quitarlo si ya estaba. */
    suspend fun addFavorite(nodeId: String): Boolean

    /** Persiste un contacto con snapshot de perfil (también vía P2P). */
    suspend fun saveContact(profile: PersonProfile, addedByPeer: Boolean = false): Boolean

    /** Indica si el contacto está guardado de forma persistente. */
    suspend fun isSavedContact(nodeId: String): Boolean

    /** Elimina un contacto (soft-delete para sincronización offline). */
    suspend fun deleteContact(nodeId: String)

    /** Lista reactiva de contactos guardados. */
    fun observeFavorites(): Flow<List<PersonProfile>>

    /**
     * Inserta o actualiza un nodo (típicamente un peer recién
     * descubierto por Bluetooth). Si el nodo es local (generado
     * por el seed) se conservan los flags de sincronización
     * previos. Si es remoto, se marca como `isSynced = false`
     * para que el motor de sincronización (paso 4) lo empuje
     * a Supabase.
     */
    suspend fun upsertNode(node: RadarNode)

    /**
     * Elimina un nodo del radar (por ejemplo, cuando un peer se
     * desconecta). El seed local se preserva.
     */
    suspend fun removeNode(nodeId: String)

    /**
     * Elimina únicamente los nodos remotos (descubiertos por
     * Bluetooth). Se usa cuando el usuario pausa el escaneo o
     * cuando se inicia una nueva sesión de discovery.
     */
    suspend fun clearDiscoveredNodes()

    /** Elimina nodos placeholder creados durante la conexión inicial. */
    suspend fun removePendingNodes()

    // ---- Feature 3: Handshake (Conexión y Networking P2P) ----

    /** Flujo reactivo de solicitudes entrantes pendientes. */
    fun observeIncomingHandshakes(): Flow<List<HandshakeRequest>>

    /** Snapshot de una solicitud por su `handshakeId`. */
    suspend fun getHandshakeById(handshakeId: String): HandshakeRequest?

    /**
     * Persiste o actualiza una solicitud de handshake. Si la fila
     * ya existe (mismo `handshakeId`), sólo se refresca la
     * información de perfil — el estado se respeta para no
     * "des-aceptar" una conexión ya materializada.
     */
    suspend fun upsertHandshakeRequest(request: HandshakeRequest)

    /**
     * Marca la solicitud como `Accepted` o `Rejected` y
     * opcionalmente guarda el contacto (cuando [accept] es
     * `true`). Devuelve el [PersonProfile] resultante en caso
     * de aceptación para que el caller pueda navegar al chat.
     */
    suspend fun respondToHandshake(
        handshakeId: String,
        accept: Boolean,
    ): HandshakeRequest?

    // ---- Feature 5: Telemetría y Visibilidad Premium ----

    /** Registra un evento de telemetría (View / Tap / Broadcast). */
    suspend fun recordProfileEvent(
        targetUserId: String,
        sourceNodeId: String,
        kind: String = com.example.airvibe.feature.radar.data.local.entity.ProfileViewEntity.KIND_VIEW,
    )

    /** Limpia el buffer de telemetría pendiente y la sube a Supabase. */
    suspend fun flushTelemetry(limit: Int = 200): Int

    /** Marca los ids como sincronizados. Llamado por el [SyncWorker]. */
    suspend fun markTelemetrySynced(ids: List<Long>)

    /** Conteo de eventos aún sin sincronizar. */
    fun observePendingTelemetryCount(): kotlinx.coroutines.flow.Flow<Int>

    /**
     * Suma local de vistas / taps en los últimos N días.
     * Se combina con `pullVisibility` para alimentar el dashboard
     * Premium offline-first.
     */
    suspend fun localVisibility(): com.example.airvibe.feature.radar.domain.model.VisibilityStats

    /** Limpia entradas sincronizadas más viejas que [olderThan]. */
    suspend fun pruneSyncedTelemetry(olderThan: Long)
}
