package com.example.airvibe.feature.radar.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.airvibe.feature.radar.data.local.entity.SavedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedContactDao {

    @Query("SELECT * FROM saved_contacts WHERE is_deleted = 0 ORDER BY display_name ASC")
    fun observeAll(): Flow<List<SavedContactEntity>>

    @Query("SELECT * FROM saved_contacts WHERE node_id = :nodeId AND is_deleted = 0 LIMIT 1")
    suspend fun getById(nodeId: String): SavedContactEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM saved_contacts WHERE node_id = :nodeId AND is_deleted = 0)")
    suspend fun exists(nodeId: String): Boolean

    @Upsert
    suspend fun upsert(contact: SavedContactEntity)

    @Query("UPDATE saved_contacts SET is_deleted = 1, is_synced = 0, updated_at = :timestamp WHERE node_id = :nodeId")
    suspend fun softDelete(nodeId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM saved_contacts WHERE node_id = :nodeId")
    suspend fun hardDelete(nodeId: String)

    @Query("SELECT * FROM saved_contacts WHERE is_synced = 0 AND is_deleted = 0 ORDER BY updated_at ASC")
    suspend fun getPendingSync(): List<SavedContactEntity>

    @Query("SELECT * FROM saved_contacts WHERE is_synced = 0 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<SavedContactEntity>

    @Query("UPDATE saved_contacts SET is_synced = 1, updated_at = :timestamp WHERE node_id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, timestamp: Long = System.currentTimeMillis())
}
