package com.example.airvibe.feature.radar.domain.repository

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
}
