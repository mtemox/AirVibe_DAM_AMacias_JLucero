package com.example.airvibe.feature.radar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Contacto guardado de forma persistente, independiente del radar en vivo.
 * Sobrevive a desconexiones, reinicios de escaneo y borrado de peers.
 *
 * Campos del **Payload extendido (Feature 2)**: [headline], [bio],
 * [isPremium], [premiumCatalog]. Permiten mostrar el catálogo de un
 * peer Premium aunque ya no esté cerca.
 */
@Entity(
    tableName = "saved_contacts",
    indices = [Index(value = ["display_name"])],
)
data class SavedContactEntity(
    @PrimaryKey
    @ColumnInfo(name = "node_id")
    val nodeId: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "headline")
    val headline: String = "",

    @ColumnInfo(name = "bio")
    val bio: String = "",

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "presence")
    val presence: String,

    @ColumnInfo(name = "tags")
    val tags: List<String>,

    @ColumnInfo(name = "accent_color_argb")
    val accentColorArgb: Int,

    @ColumnInfo(name = "is_premium", defaultValue = "0")
    val isPremium: Boolean = false,

    @ColumnInfo(name = "premium_catalog")
    val premiumCatalog: String? = null,
    
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String? = null,
    
    @ColumnInfo(name = "avatar_base64")
    val avatarBase64: String? = null,

    @ColumnInfo(name = "added_by_peer")
    val addedByPeer: Boolean = false,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
)
