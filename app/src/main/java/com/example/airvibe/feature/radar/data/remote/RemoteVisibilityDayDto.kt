package com.example.airvibe.feature.radar.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO del agregado diario `visibility_daily`. Se consulta desde
 * la app del usuario Premium para alimentar su dashboard de
 * "Visibilidad" (vistas / toques / visitors únicos / día).
 */
@Serializable
data class RemoteVisibilityDayDto(
    @SerialName("target_user_id") val targetUserId: String,
    @SerialName("day") val day: String,
    @SerialName("views_count") val viewsCount: Int = 0,
    @SerialName("taps_count") val tapsCount: Int = 0,
    @SerialName("broadcasts_count") val broadcastsCount: Int = 0,
    @SerialName("unique_visitors_count") val uniqueVisitorsCount: Int = 0,
)
