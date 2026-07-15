package com.example.airvibe.feature.chat.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.room.Index

@Entity(
    tableName = "proximity_rooms",
    indices = [
        Index(value = ["is_deleted"], name = "index_proximity_rooms_is_deleted")
    ]
)
data class ProximityRoomEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "host_node_id")
    val hostNodeId: String,

    @ColumnInfo(name = "host_name")
    val hostName: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "joined", defaultValue = "0")
    val joined: Boolean = false,

    @ColumnInfo(name = "is_host", defaultValue = "0")
    val isHost: Boolean = false,

    @ColumnInfo(name = "is_synced", defaultValue = "0")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
)
