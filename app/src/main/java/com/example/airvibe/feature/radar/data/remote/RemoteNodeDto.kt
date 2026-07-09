package com.example.airvibe.feature.radar.data.remote

import com.example.airvibe.core.network.SupabaseConfig
import com.example.airvibe.feature.radar.data.local.entity.NodeEntity
import com.example.airvibe.feature.radar.domain.remote.RemoteNode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO `@Serializable` que Postgrest sabe deserializar
 * automáticamente. Mantenemos un tipo específico de data y lo
 * mapeamos a [RemoteNode] (modelo de dominio) en una función
 * pura, evitando filtrar anotaciones de `kotlinx.serialization`
 * al resto de la app.
 */
@Serializable
internal data class RemoteNodeDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("status") val status: String,
    @SerialName("detail") val detail: String,
    @SerialName("kind") val kind: String,
    @SerialName("presence") val presence: String,
    @SerialName("angle_degrees") val angleDegrees: Double,
    @SerialName("distance_normalized") val distanceNormalized: Double,
    @SerialName("signal_strength") val signalStrength: Double,
    @SerialName("accent_color_argb") val accentColorArgb: Long,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

internal fun NodeEntity.toRemoteDto(): RemoteNodeDto = RemoteNodeDto(
    id = id,
    displayName = displayName,
    status = status,
    detail = detail,
    kind = kind,
    presence = presence,
    angleDegrees = angleDegrees.toDouble(),
    distanceNormalized = distanceNormalized.toDouble(),
    signalStrength = signalStrength.toDouble(),
    accentColorArgb = accentColorArgb.toLong() and 0xFFFFFFFFL,
    tags = tags,
    isFavorite = isFavorite,
)

internal fun RemoteNodeDto.toDomain(): RemoteNode = RemoteNode(
    id = id,
    displayName = displayName,
    status = status,
    detail = detail,
    kind = kind,
    presence = presence,
    angleDegrees = angleDegrees,
    distanceNormalized = distanceNormalized,
    signalStrength = signalStrength,
    accentColorArgb = accentColorArgb,
    tags = tags,
    isFavorite = isFavorite,
    updatedAt = updatedAt?.toEpochMillis() ?: System.currentTimeMillis(),
    createdAt = createdAt?.toEpochMillis() ?: System.currentTimeMillis(),
)

/**
 * Supabase devuelve timestamps como ISO 8601 (`2026-07-09T15:00:00+00:00`).
 * Esta función los convierte a milisegundos para mantener el mismo
 * formato que Room (Long).
 */
private fun String.toEpochMillis(): Long? = runCatching {
    val instant = java.time.Instant.parse(this)
    instant.toEpochMilli()
}.getOrNull()

internal val RADAR_NODES_TABLE: String = SupabaseConfig.RADAR_NODES_TABLE
