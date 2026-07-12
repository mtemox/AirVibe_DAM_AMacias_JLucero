package com.example.airvibe.feature.radar.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteProfileDto(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("status") val status: String? = null,
    @SerialName("tags") val tags: List<String> = emptyList(),
)
