package com.example.airvibe.feature.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.airvibe.feature.chat.data.local.entity.ProximityRoomEntity
import com.example.airvibe.feature.chat.data.local.entity.RoomMemberEntity
import com.example.airvibe.feature.chat.data.local.entity.RoomMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProximityRoomDao {

    @Query("SELECT * FROM proximity_rooms WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun observeRooms(): Flow<List<ProximityRoomEntity>>

    @Query("SELECT * FROM proximity_rooms WHERE id = :id AND is_deleted = 0 LIMIT 1")
    fun observeRoomById(id: String): Flow<ProximityRoomEntity?>

    @Query("SELECT * FROM proximity_rooms WHERE id = :id AND is_deleted = 0 LIMIT 1")
    suspend fun getRoom(id: String): ProximityRoomEntity?

    @Query("SELECT * FROM proximity_rooms WHERE id = :id LIMIT 1")
    suspend fun getRoomIncludingDeleted(id: String): ProximityRoomEntity?

    @Upsert
    suspend fun upsertRoom(room: ProximityRoomEntity)

    @Query("UPDATE proximity_rooms SET is_deleted = 1, is_synced = 0 WHERE id = :id")
    suspend fun deleteRoom(id: String)
    
    @Query("DELETE FROM proximity_rooms WHERE id = :id")
    suspend fun hardDeleteRoom(id: String)
    
    @Query("SELECT * FROM proximity_rooms WHERE is_deleted = 1 AND is_synced = 0")
    suspend fun getPendingDeletions(): List<ProximityRoomEntity>

    @Query("DELETE FROM room_messages WHERE room_id = :roomId")
    suspend fun deleteMessages(roomId: String)

    @Query("DELETE FROM room_members WHERE room_id = :roomId")
    suspend fun deleteMembers(roomId: String)

    @Query("UPDATE proximity_rooms SET joined = 1, is_synced = 0 WHERE id = :id")
    suspend fun markJoined(id: String)

    @Query("SELECT * FROM proximity_rooms WHERE is_synced = 0 AND is_deleted = 0")
    suspend fun getPendingRooms(): List<ProximityRoomEntity>

    @Query("UPDATE proximity_rooms SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markRoomsSynced(ids: List<String>)

    @Query("SELECT * FROM room_messages WHERE is_synced = 0 ORDER BY created_at ASC")
    suspend fun getPendingMessages(): List<RoomMessageEntity>

    @Query("UPDATE room_messages SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markMessagesSynced(ids: List<String>)

    @Upsert
    suspend fun upsertMessage(message: RoomMessageEntity)

    @Query("SELECT * FROM room_messages WHERE room_id = :roomId ORDER BY created_at ASC, id ASC")
    fun observeMessages(roomId: String): Flow<List<RoomMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: RoomMessageEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM room_messages WHERE id = :id)")
    suspend fun messageExists(id: String): Boolean

    // -------- Feature 4: room members --------

    @Query("SELECT * FROM room_members WHERE room_id = :roomId AND is_active = 1 ORDER BY joined_at ASC")
    fun observeActiveMembers(roomId: String): Flow<List<RoomMemberEntity>>

    @Query("SELECT * FROM room_members WHERE room_id = :roomId ORDER BY joined_at ASC")
    fun observeAllMembers(roomId: String): Flow<List<RoomMemberEntity>>

    @Query("SELECT * FROM room_members WHERE room_id = :roomId AND node_id = :nodeId LIMIT 1")
    suspend fun getMember(roomId: String, nodeId: String): RoomMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: RoomMemberEntity): Long

    @Query("UPDATE room_members SET is_active = 0, last_seen_at = :ts WHERE room_id = :roomId AND node_id = :nodeId")
    suspend fun markMemberInactive(roomId: String, nodeId: String, ts: Long = System.currentTimeMillis())

    @Query("UPDATE room_members SET is_active = 1, last_seen_at = :ts WHERE room_id = :roomId AND node_id = :nodeId")
    suspend fun markMemberActive(roomId: String, nodeId: String, ts: Long = System.currentTimeMillis())

    @Query("UPDATE room_members SET display_name = :displayName, last_seen_at = :ts WHERE room_id = :roomId AND node_id = :nodeId")
    suspend fun updateMemberDisplayName(roomId: String, nodeId: String, displayName: String, ts: Long = System.currentTimeMillis())
}
