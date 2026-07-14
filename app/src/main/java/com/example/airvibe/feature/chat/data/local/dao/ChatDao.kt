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
    @Query("SELECT * FROM chat_messages WHERE node_id = :nodeId AND is_deleted = 0 ORDER BY created_at ASC")
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
            latest.node_id AS nodeId,
            COALESCE(n.display_name, latest.node_id) AS displayName,
            m.text AS lastMessage,
            m.created_at AS lastTimestamp,
            m.kind AS kind,
            m.direction AS direction,
            m.is_synced AS isSynced,
            latest.unread_count AS unreadCount
        FROM (
            SELECT 
                node_id, 
                MAX(created_at) AS max_ts, 
                SUM(CASE WHEN is_read = 0 AND direction = 'Incoming' THEN 1 ELSE 0 END) AS unread_count
            FROM chat_messages
            WHERE is_deleted = 0
            GROUP BY node_id
        ) latest
        INNER JOIN chat_messages m ON latest.node_id = m.node_id AND latest.max_ts = m.created_at AND m.is_deleted = 0
        LEFT JOIN radar_nodes n ON n.id = latest.node_id
        ORDER BY m.created_at DESC
        """,
    )
    fun observeConversationSummaries(): Flow<List<ConversationSummaryRow>>

    /** Conversaciones cuya última actividad fue un mensaje entrante. */
    @Query(
        """
        SELECT COUNT(*) FROM chat_messages m
        INNER JOIN (
            SELECT node_id, MAX(created_at) AS max_ts
            FROM chat_messages
            WHERE is_deleted = 0
            GROUP BY node_id
        ) latest
            ON latest.node_id = m.node_id AND latest.max_ts = m.created_at
        WHERE m.direction = 'Incoming' AND m.is_deleted = 0
        """,
    )
    fun observeUnreadConversationCount(): Flow<Int>

    /** Conteo total de mensajes pendientes de sincronizar. */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE is_synced = 0 AND is_deleted = 0")
    fun observeUnsyncedCount(): Flow<Int>

    /** Mensajes aún no sincronizados. */
    @Query("SELECT * FROM chat_messages WHERE is_synced = 0 AND is_deleted = 0")
    suspend fun getUnsynced(): List<ChatMessageEntity>

    /** Búsqueda puntual por id. */
    @Query("SELECT * FROM chat_messages WHERE id = :id AND is_deleted = 0 LIMIT 1")
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

    /** Marca todos los mensajes de una conversación como leídos. */
    @Query("UPDATE chat_messages SET is_read = 1 WHERE node_id = :nodeId AND is_read = 0")
    suspend fun markConversationAsRead(nodeId: String)

    /** Elimina todo el historial de un peer. */
    @Query("UPDATE chat_messages SET is_deleted = 1, is_synced = 0 WHERE node_id = :nodeId")
    suspend fun softClearByNode(nodeId: String)

    @Query("DELETE FROM chat_messages WHERE node_id = :nodeId")
    suspend fun hardClearByNode(nodeId: String)

    @Query("SELECT * FROM chat_messages WHERE is_synced = 0 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<ChatMessageEntity>

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
    val unreadCount: Int,
)
