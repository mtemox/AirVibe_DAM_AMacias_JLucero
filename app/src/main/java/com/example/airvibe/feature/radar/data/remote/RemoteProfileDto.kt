package com.example.airvibe.feature.radar.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO del perfil propio sincronizado con Supabase (`public.profiles`).
 * Feature 2: incluye headline, profession, bio, is_premium y
 * premium_catalog para soportar el Payload extendido Premium.
 */
@Serializable
data class RemoteProfileDto(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("status") val status: String? = null,
    @SerialName("headline") val headline: String = "",
    @SerialName("profession") val profession: String? = null,
    @SerialName("bio") val bio: String = "",
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("premium_catalog") val premiumCatalog: String? = null,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("avatar_base64") val avatarBase64: String? = null,
)
