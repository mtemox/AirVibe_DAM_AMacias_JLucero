package com.example.airvibe.feature.radar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.airvibe.feature.radar.data.local.entity.HandshakeRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HandshakeRequestDao {

    /** Stream reactivo con las solicitudes **entrantes pendientes**. */
    @Query(
        """
        SELECT * FROM handshake_requests
        WHERE direction = 'Incoming' AND status = 'Pending'
        ORDER BY created_at DESC
        """,
    )
    fun observeIncomingPending(): Flow<List<HandshakeRequestEntity>>

    /** Stream reactivo de TODAS las solicitudes del usuario. */
    @Query("SELECT * FROM handshake_requests ORDER BY created_at DESC")
    fun observeAll(): Flow<List<HandshakeRequestEntity>>

    /** Búsqueda por `handshakeId` (UUID emitido por el peer). */
    @Query("SELECT * FROM handshake_requests WHERE handshake_id = :handshakeId LIMIT 1")
    suspend fun getByHandshakeId(handshakeId: String): HandshakeRequestEntity?

    @Query("SELECT * FROM handshake_requests WHERE peer_node_id = :peerNodeId ORDER BY created_at DESC")
    suspend fun getForPeer(peerNodeId: String): List<HandshakeRequestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(request: HandshakeRequestEntity): Long

    @Update
    suspend fun update(request: HandshakeRequestEntity)

    @Query(
        """
        UPDATE handshake_requests
        SET status = :status, responded_at = :respondedAt
        WHERE handshake_id = :handshakeId
        """,
    )
    suspend fun updateStatus(handshakeId: String, status: String, respondedAt: Long)

    @Query("DELETE FROM handshake_requests WHERE handshake_id = :handshakeId")
    suspend fun deleteByHandshakeId(handshakeId: String)

    @Query("DELETE FROM handshake_requests WHERE handshake_id = :handshakeId AND status = :expectedStatus")
    suspend fun deleteIfStatus(handshakeId: String, expectedStatus: String)
}
