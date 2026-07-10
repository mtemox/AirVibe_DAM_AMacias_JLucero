package com.example.airvibe.feature.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.airvibe.feature.chat.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO de la tabla `chat_messages`. Sigue el patrón reactivo del
 * proyecto: lecturas como [Flow] para que la UI se entere de
 * cualquier cambio sin polling, escrituras como `suspend`.
 */
@Dao
interface ChatDao {

    /** Mensajes de una conversación, ordenados por timestamp. */
    @Query("SELECT * FROM chat_messages WHERE node_id = :nodeId ORDER BY created_at ASC")
    fun observeByNode(nodeId: String): Flow<List<ChatMessageEntity>>

    /**
     * Resumen de la bandeja de entrada: por cada peer con al
     * menos un mensaje, devuelve el último mensaje y conteo de
     * no-leídos.
     *
     * La query se resuelve con un `GROUP BY` + `MAX(created_at)`.
     * Para no leer N veces la tabla de nodos, hacemos un JOIN
     * con `radar_nodes` cuando exista. Si no hay match (peer
     * aún no persistido), el JOIN produce NULL y caemos al
     * `nodeId` como `displayName` por defecto.
     */
    @Query(
        """
        SELECT
            m.node_id                AS nodeId,
            COALESCE(n.display_name, m.node_id) AS displayName,
            m.text                   AS lastMessage,
            m.created_at             AS lastTimestamp,
            m.kind                   AS kind,
            m.direction              AS direction,
            m.is_synced              AS isSynced
        FROM chat_messages m
        INNER JOIN (
            SELECT node_id, MAX(created_at) AS max_ts
            FROM chat_messages
            GROUP BY node_id
        ) latest
            ON latest.node_id = m.node_id AND latest.max_ts = m.created_at
        LEFT JOIN radar_nodes n
            ON n.id = m.node_id
        ORDER BY m.created_at DESC
        """,
    )
    fun observeConversationSummaries(): Flow<List<ConversationSummaryRow>>

    /** Conteo total de mensajes pendientes de sincronizar. */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE is_synced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    /** Mensajes aún no sincronizados. */
    @Query("SELECT * FROM chat_messages WHERE is_synced = 0")
    suspend fun getUnsynced(): List<ChatMessageEntity>

    /** Búsqueda puntual por id. */
    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Update
    suspend fun update(message: ChatMessageEntity)

    @Upsert
    suspend fun upsertAll(messages: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET status = :status, is_synced = 0 WHERE id = :id")
    suspend fun setStatus(id: String, status: String)

    @Query("UPDATE chat_messages SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    /** Elimina todo el historial de un peer. */
    @Query("DELETE FROM chat_messages WHERE node_id = :nodeId")
    suspend fun clearByNode(nodeId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clear()
}

/**
 * Fila intermedia de la query de resúmenes. Se mantiene en
 * `data/local` porque es un detalle de SQL que no debe filtrarse
 * al dominio; el mapper lo convierte a `ConversationSummary`.
 */
data class ConversationSummaryRow(
    val nodeId: String,
    val displayName: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val kind: String,
    val direction: String,
    val isSynced: Boolean,
)
