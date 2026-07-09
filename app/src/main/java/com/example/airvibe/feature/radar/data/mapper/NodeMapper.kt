package com.example.airvibe.feature.radar.data.mapper

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.airvibe.feature.radar.data.local.entity.NodeEntity
import com.example.airvibe.feature.radar.domain.model.PersonProfile
import com.example.airvibe.feature.radar.domain.model.PresenceStatus
import com.example.airvibe.feature.radar.domain.model.RadarNode
import com.example.airvibe.feature.radar.domain.model.RadarNodeKind
import com.example.airvibe.feature.radar.domain.remote.RemoteNode

/**
 * Conversores entre la capa de persistencia (Room) y los modelos de
 * dominio. Mantener esta transformación aquí permite que la capa de
 * presentación nunca vea un `NodeEntity` y que el dominio no conozca
 * detalles de SQLite o Room.
 */
object NodeMapper {

    fun NodeEntity.toDomain(): RadarNode = RadarNode(
        id = id,
        displayName = displayName,
        status = status,
        detail = detail,
        kind = kind.toNodeKind(),
        presence = presence.toPresenceStatus(),
        angleDegrees = angleDegrees,
        distanceNormalized = distanceNormalized,
        signalStrength = signalStrength,
        accentColor = Color(accentColorArgb),
        tags = tags,
    )

    fun RadarNode.toEntity(
        isFavorite: Boolean = false,
        isSynced: Boolean = false,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis(),
    ): NodeEntity = NodeEntity(
        id = id,
        displayName = displayName,
        status = status,
        detail = detail,
        kind = kind.name,
        presence = presence.name,
        angleDegrees = angleDegrees,
        distanceNormalized = distanceNormalized,
        signalStrength = signalStrength,
        accentColorArgb = accentColor.toArgbSafe(),
        tags = tags,
        isFavorite = isFavorite,
        isSynced = isSynced,
        updatedAt = updatedAt,
        createdAt = createdAt,
    )

    fun NodeEntity.toProfile(distanceMeters: Int? = null): PersonProfile = PersonProfile(
        id = id,
        displayName = displayName,
        headline = status,
        bio = detail,
        status = status,
        presence = presence.toPresenceStatus(),
        tags = tags,
        distanceMeters = distanceMeters ?: distanceMetersFor(distanceNormalized),
        isFavorite = isFavorite,
        accentHue = accentHueFrom(accentColorArgb),
    )

    private fun String.toNodeKind(): RadarNodeKind =
        runCatching { RadarNodeKind.valueOf(this) }.getOrDefault(RadarNodeKind.Person)

    private fun String.toPresenceStatus(): PresenceStatus =
        runCatching { PresenceStatus.valueOf(this) }.getOrDefault(PresenceStatus.Online)

    private fun distanceMetersFor(distanceNormalized: Float): Int {
        val clamped = distanceNormalized.coerceIn(0f, 1f)
        return ((1f - clamped) * 12f + 1.5f).toInt()
    }

    private fun accentHueFrom(argb: Int): Float {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        if (delta == 0f) return 0f
        val hue = when (max) {
            r -> ((g - b) / delta) % 6f
            g -> ((b - r) / delta) + 2f
            else -> ((r - g) / delta) + 4f
        }
        return ((hue * 60f) + 360f) % 360f
    }

    private fun Color.toArgbSafe(): Int = toArgb()
}

/**
 * Conversión Entity → modelo de la capa remota (Supabase).
 * Mantenida en `data` para que el dominio no conozca el contrato
 * de la nube.
 */
fun NodeEntity.toRemoteNode(): RemoteNode = RemoteNode(
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
    updatedAt = updatedAt,
    createdAt = createdAt,
)

/** Conversión inversa: remoto → entidad local (para pull-to-refresh). */
fun RemoteNode.toEntity(): NodeEntity = NodeEntity(
    id = id,
    displayName = displayName,
    status = status,
    detail = detail,
    kind = kind,
    presence = presence,
    angleDegrees = angleDegrees.toFloat(),
    distanceNormalized = distanceNormalized.toFloat(),
    signalStrength = signalStrength.toFloat(),
    accentColorArgb = accentColorArgb.toInt(),
    tags = tags,
    isFavorite = isFavorite,
    isSynced = true, // proviene de la nube
    updatedAt = updatedAt,
    createdAt = createdAt,
)
