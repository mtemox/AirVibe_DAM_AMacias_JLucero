package com.example.airvibe.feature.chat.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "room_messages",
    indices = [Index(value = ["room_id"])],
)
data class RoomMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "room_id")
    val roomId: String,

    @ColumnInfo(name = "sender_node_id")
    val senderNodeId: String,

    @ColumnInfo(name = "sender_name")
    val senderName: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    /**
     * Establecido en tiempo de escritura (al crear o recibir), no en tiempo de lectura.
     * Permite identificar mensajes propios incluso después de reinstalar la app.
     */
    @ColumnInfo(name = "is_own", defaultValue = "0")
    val isOwn: Boolean = false,

    @ColumnInfo(name = "is_synced", defaultValue = "0")
    val isSynced: Boolean = false,
)
