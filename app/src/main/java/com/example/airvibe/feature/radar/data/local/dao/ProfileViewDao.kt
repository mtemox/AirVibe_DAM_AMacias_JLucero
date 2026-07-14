package com.example.airvibe.feature.radar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.airvibe.feature.radar.data.local.entity.ProfileViewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileViewDao {

    @Query("SELECT * FROM profile_views WHERE is_synced = 0 ORDER BY created_at ASC")
    suspend fun getPendingSync(): List<ProfileViewEntity>

    @Query("SELECT * FROM profile_views WHERE is_synced = 0 ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingSync(limit: Int): List<ProfileViewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(view: ProfileViewEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(views: List<ProfileViewEntity>)

    @Query("UPDATE profile_views SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM profile_views WHERE is_synced = 1 AND created_at < :olderThan")
    suspend fun deleteSyncedOlderThan(olderThan: Long)

    @Query("SELECT COUNT(*) FROM profile_views WHERE target_user_id = :targetUserId AND kind = 'View' AND created_at >= :since")
    suspend fun countViewsSince(targetUserId: String, since: Long): Int

    @Query("SELECT COUNT(*) FROM profile_views WHERE target_user_id = :targetUserId AND kind = 'Tap' AND created_at >= :since")
    suspend fun countTapsSince(targetUserId: String, since: Long): Int

    @Query("SELECT COUNT(DISTINCT source_node_id) FROM profile_views WHERE target_user_id = :targetUserId AND created_at >= :since")
    suspend fun countUniqueVisitorsSince(targetUserId: String, since: Long): Int

    /**
     * Lectura reactiva de los contadores para alimentar el
     * dashboard de Visibilidad del perfil (sin tocar la red).
     */
    @Query("SELECT COUNT(*) FROM profile_views WHERE kind = 'View'")
    fun observeTotalViews(): Flow<Int>

    @Query("SELECT COUNT(*) FROM profile_views WHERE kind = 'Tap'")
    fun observeTotalTaps(): Flow<Int>
}
