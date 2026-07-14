package com.example.airvibe.feature.chat.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Feature 4 — Sala de Proximidad.
 *
 * Miembro activo de una sala. Cuando el Host recibe un
 * `room_join` añade (o reactiva) la fila; cuando recibe un
 * `room_leave` o el miembro está inactivo por mucho tiempo
 * la marca como `is_active = false`.
 *
 * `nodeId` es el identificador Bluetooth estable del peer
 * (mismo que `radar_nodes.id`).
 */
@Entity(
    tableName = "room_members",
    indices = [
        Index(value = ["room_id"]),
        Index(value = ["is_active"]),
        Index(value = ["node_id"]),
    ],
)
data class RoomMemberEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "room_id")
    val roomId: String,

    @ColumnInfo(name = "node_id")
    val nodeId: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "role")
    val role: String = ROLE_GUEST,

    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Boolean = true,

    @ColumnInfo(name = "joined_at")
    val joinedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_seen_at")
    val lastSeenAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val ROLE_HOST = "Host"
        const val ROLE_GUEST = "Guest"
    }
}
