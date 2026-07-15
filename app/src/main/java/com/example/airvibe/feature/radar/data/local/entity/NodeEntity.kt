package com.example.airvibe.feature.radar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Representación local (Room) de un nodo del radar.
 *
 * Toda fila de esta tabla es susceptible de sincronizarse con la nube,
 * por lo que cumple el contrato offline-first:
 *
 *  - [isSynced] indica si la fila ya fue empujada a Supabase.
 *  - [updatedAt] es el último cambio local (timestamp en milisegundos).
 *  - [createdAt] es el momento de inserción, útil para auditoría.
 *
 * El color de acento se almacena como ARGB en [accentColorArgb] para
 * evitar un converter dedicado y conservar eficiencia en lectura.
 *
 * Campos del **Payload extendido (Feature 2)**:
 *  - [headline] profesión / título corto
 *  - [bio] biografía (1–2 frases)
 *  - [isPremium] indica si el peer paga Premium
 *  - [premiumCatalog] portafolio o mini-catálogo (null si no aplica)
 */
@Entity(
    tableName = "radar_nodes",
    indices = [
        Index(value = ["is_synced"]),
        Index(value = ["presence"]),
        Index(value = ["kind"]),
        Index(value = ["is_premium"]),
    ],
)
data class NodeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "detail")
    val detail: String,

    @ColumnInfo(name = "kind")
    val kind: String,

    @ColumnInfo(name = "presence")
    val presence: String,

    @ColumnInfo(name = "angle_degrees")
    val angleDegrees: Float,

    @ColumnInfo(name = "distance_normalized")
    val distanceNormalized: Float,

    @ColumnInfo(name = "signal_strength")
    val signalStrength: Float,

    @ColumnInfo(name = "accent_color_argb")
    val accentColorArgb: Int,

    @ColumnInfo(name = "tags")
    val tags: List<String>,

    @ColumnInfo(name = "headline", defaultValue = "")
    val headline: String = "",

    @ColumnInfo(name = "bio", defaultValue = "")
    val bio: String = "",

    @ColumnInfo(name = "is_premium", defaultValue = "0")
    val isPremium: Boolean = false,

    @ColumnInfo(name = "premium_catalog")
    val premiumCatalog: String? = null,
    
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String? = null,
    
    @ColumnInfo(name = "avatar_base64")
    val avatarBase64: String? = null,

    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "is_synced", defaultValue = "0")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
