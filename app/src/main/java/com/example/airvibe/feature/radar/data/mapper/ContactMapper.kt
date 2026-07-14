package com.example.airvibe.feature.radar.data.mapper

import androidx.compose.ui.graphics.Color
import com.example.airvibe.feature.radar.data.local.entity.NodeEntity
import com.example.airvibe.feature.radar.data.local.entity.SavedContactEntity
import com.example.airvibe.feature.radar.domain.model.PersonProfile
import com.example.airvibe.feature.radar.domain.model.PresenceStatus

object ContactMapper {

    fun PersonProfile.toEntity(
        addedByPeer: Boolean = false,
        isSynced: Boolean = false,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis(),
        accentColorArgb: Int = 0xFF6366F1.toInt(),
    ): SavedContactEntity = SavedContactEntity(
        nodeId = id,
        displayName = displayName,
        headline = headline,
        bio = bio,
        status = status,
        presence = presence.name,
        tags = tags,
        accentColorArgb = accentColorArgb,
        isPremium = isPremium,
        premiumCatalog = premiumCatalog,
        addedByPeer = addedByPeer,
        isSynced = isSynced,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun NodeEntity.toSavedContact(addedByPeer: Boolean = false): SavedContactEntity = SavedContactEntity(
        nodeId = id,
        displayName = displayName,
        headline = headline,
        bio = bio,
        status = status,
        presence = presence,
        tags = tags,
        accentColorArgb = accentColorArgb,
        isPremium = isPremium,
        premiumCatalog = premiumCatalog,
        addedByPeer = addedByPeer,
        isSynced = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    fun SavedContactEntity.toProfile(): PersonProfile = PersonProfile(
        id = nodeId,
        displayName = displayName,
        headline = headline,
        bio = bio,
        status = status,
        presence = runCatching { PresenceStatus.valueOf(presence) }
            .getOrDefault(PresenceStatus.Away),
        tags = tags,
        distanceMeters = 0,
        isFavorite = true,
        accentHue = accentHueFrom(accentColorArgb),
        isPremium = isPremium,
        premiumCatalog = premiumCatalog,
    )

    fun accentArgbFromProfile(profile: PersonProfile): Int =
        android.graphics.Color.HSVToColor(floatArrayOf(profile.accentHue, 0.55f, 0.95f))

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
}
