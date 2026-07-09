package com.example.airvibe.feature.radar.domain.remote

/**
 * DTO que viaja entre la app y la tabla `radar_nodes` de
 * Supabase. Mantener este contrato en la capa de **dominio**
 * permite cambiar el proveedor (Supabase, Firebase, REST propio)
 * sin tocar el resto de la arquitectura.
 *
 * Las columnas espejan `NodeEntity` (paso 2) para que la
 * sincronización sea 1:1. El `ownerId` lo completa el servidor
 * mediante una policy RLS; el cliente solo lo envía cuando
 * necesita sobrescribir un registro que no le pertenece.
 */
data class RemoteNode(
    val id: String,
    val displayName: String,
    val status: String,
    val detail: String,
    val kind: String,
    val presence: String,
    val angleDegrees: Double,
    val distanceNormalized: Double,
    val signalStrength: Double,
    val accentColorArgb: Long,
    val tags: List<String>,
    val isFavorite: Boolean,
    val updatedAt: Long,
    val createdAt: Long,
)

/**
 * Contrato de la fuente remota. La capa de presentación
 * (WorkManager) consume este interfaz — la implementación vive
 * en `data/remote/SupabaseNodeDataSource`.
 */
interface RemoteNodeDataSource {

    /**
     * Sube un lote de nodos al backend. Devuelve los IDs que
     * se persistieron correctamente para que el `SyncWorker`
     * los marque como sincronizados localmente.
     */
    suspend fun upsert(nodes: List<RemoteNode>): Result<List<String>>

    /** Descarga todos los nodos del usuario autenticado. */
    suspend fun fetchAll(): Result<List<RemoteNode>>

    /** Elimina un nodo en la nube. */
    suspend fun delete(nodeId: String): Result<Unit>
}
