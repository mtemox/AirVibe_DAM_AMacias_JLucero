package com.example.airvibe.feature.radar.data.remote

import com.example.airvibe.feature.radar.data.local.dao.RadarDao
import com.example.airvibe.feature.radar.domain.remote.RemoteNode
import com.example.airvibe.feature.radar.domain.remote.RemoteNodeDataSource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

/**
 * Implementación del [RemoteNodeDataSource] usando el SDK
 * oficial de Supabase (Postgrest).
 *
 * Estrategia: `upsert` con `onConflict = id` para que el cliente
 * pueda reenviar nodos que ya existen sin generar duplicados.
 * El backend debe tener RLS para que un usuario solo pueda
 * insertar/actualizar filas con `owner_id = auth.uid()`.
 */
class SupabaseNodeDataSource(
    private val supabase: SupabaseClient,
    @Suppress("unused") private val radarDao: RadarDao,
) : RemoteNodeDataSource {

    private val table = RADAR_NODES_TABLE

    override suspend fun upsert(nodes: List<RemoteNode>, ownerId: String): Result<List<String>> = runCatching {
        if (nodes.isEmpty()) return@runCatching emptyList()
        val dtos = nodes.map { it.toDto(ownerId) }
        supabase.postgrest.from(table).upsert(dtos) {
            onConflict = "id"
        }
        nodes.map { it.id }
    }.recoverCatching { throwable ->
        throw RemoteException(
            message = "No se pudieron sincronizar los nodos: ${throwable.message}",
            cause = throwable,
        )
    }

    override suspend fun fetchAll(ownerId: String): Result<List<RemoteNode>> = runCatching {
        supabase.postgrest
            .from(table)
            .select(columns = Columns.ALL) {
                filter { eq("owner_id", ownerId) }
            }
            .decodeList<RemoteNodeDto>()
            .map { it.toDomain() }
    }.recoverCatching { throwable ->
        throw RemoteException(
            message = "No se pudo descargar el radar: ${throwable.message}",
            cause = throwable,
        )
    }

    override suspend fun delete(nodeId: String, ownerId: String): Result<Unit> = runCatching {
        supabase.postgrest.from(table).delete {
            filter {
                eq("id", nodeId)
                eq("owner_id", ownerId)
            }
        }
        Unit
    }.recoverCatching { throwable ->
        throw RemoteException(
            message = "No se pudo eliminar el nodo: ${throwable.message}",
            cause = throwable,
        )
    }

    private fun RemoteNode.toDto(ownerId: String): RemoteNodeDto = RemoteNodeDto(
        id = id,
        ownerId = ownerId,
        displayName = displayName,
        status = status,
        detail = detail,
        kind = kind,
        presence = presence,
        angleDegrees = angleDegrees,
        distanceNormalized = distanceNormalized,
        signalStrength = signalStrength,
        accentColorArgb = accentColorArgb,
        tags = tags,
        isFavorite = isFavorite,
    )

    /**
     * Variante del listado ordenada por última actualización. Útil
     * para los tests o futuras implementaciones de "pull-to-refresh".
     */
    @Suppress("unused")
    suspend fun fetchAllOrdered(ownerId: String): Result<List<RemoteNode>> = runCatching {
        supabase.postgrest
            .from(table)
            .select(columns = Columns.ALL) {
                filter { eq("owner_id", ownerId) }
                order(column = "updated_at", order = Order.DESCENDING)
            }
            .decodeList<RemoteNodeDto>()
            .map { it.toDomain() }
    }
}

/**
 * Excepción tipada de la capa remota. Permite a la UI mostrar
 * un mensaje sin filtrar tipos de Postgrest.
 */
class RemoteException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
