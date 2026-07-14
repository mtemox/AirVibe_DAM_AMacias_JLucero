package com.example.airvibe.feature.radar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Feature 5 — Telemetría local de vistas / toques / broadcasts.
 *
 * Cada vez que un peer Premium aparece en el radar de un viewer
 * (o es tocado / recibe un broadcast) se inserta una fila aquí. El
 * `SyncWorker` empuja los `is_synced = 0` a Supabase y los borra
 * tras un periodo de gracia.
 *
 * `targetUserId` es el **perfil Premium** que recibe la
 * métrica. Como todavía no tenemos auth.uid() por peer
 * (sólo device-id), usamos el `device-<UUID>` que ya genera
 * `DeviceIdentityProvider`. Cuando un peer se autentique con
 * Supabase por primera vez, sus filas se "vincularán" a su
 * `auth.uid()`.
 */
@Entity(
    tableName = "profile_views",
    indices = [
        Index(value = ["target_user_id"]),
        Index(value = ["is_synced"]),
        Index(value = ["created_at"]),
    ],
)
data class ProfileViewEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "target_user_id")
    val targetUserId: String,

    @ColumnInfo(name = "source_node_id")
    val sourceNodeId: String,

    @ColumnInfo(name = "kind")
    val kind: String = KIND_VIEW,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_synced", defaultValue = "0")
    val isSynced: Boolean = false,
) {
    companion object {
        const val KIND_VIEW = "View"
        const val KIND_TAP = "Tap"
        const val KIND_BROADCAST = "Broadcast"
    }
}
