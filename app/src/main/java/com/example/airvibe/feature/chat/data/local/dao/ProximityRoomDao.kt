package com.example.airvibe.feature.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.airvibe.feature.chat.data.local.entity.ProximityRoomEntity
import com.example.airvibe.feature.chat.data.local.entity.RoomMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProximityRoomDao {

    @Query("SELECT * FROM proximity_rooms ORDER BY created_at DESC")
    fun observeRooms(): Flow<List<ProximityRoomEntity>>

    @Query("SELECT * FROM proximity_rooms WHERE id = :id LIMIT 1")
    fun observeRoomById(id: String): Flow<ProximityRoomEntity?>

    @Query("SELECT * FROM proximity_rooms WHERE id = :id LIMIT 1")
    suspend fun getRoom(id: String): ProximityRoomEntity?

    @Upsert
    suspend fun upsertRoom(room: ProximityRoomEntity)

    @Query("UPDATE proximity_rooms SET joined = 1, is_synced = 0 WHERE id = :id")
    suspend fun markJoined(id: String)

    @Query("SELECT * FROM proximity_rooms WHERE is_synced = 0")
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
}
