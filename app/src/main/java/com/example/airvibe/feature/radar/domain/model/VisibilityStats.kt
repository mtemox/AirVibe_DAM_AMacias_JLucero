package com.example.airvibe.feature.radar.domain.model

/**
 * Feature 5 — Telemetría agregada que consume el dashboard de
 * Visibilidad del usuario Premium. Combina datos locales (no
 * sincronizados) y remotos (ya en Supabase) en una sola
 * proyección para la UI.
 */
data class VisibilityStats(
    val viewsLast7Days: Int = 0,
    val tapsLast7Days: Int = 0,
    val uniqueVisitorsLast7Days: Int = 0,
    val viewsLast30Days: Int = 0,
    val tapsLast30Days: Int = 0,
    val totalPendingSync: Int = 0,
    val byDay: List<VisibilityDay> = emptyList(),
) {
    /**
     * Versión "vacía" usada mientras carga la red.
     */
    companion object {
        val Empty = VisibilityStats()
    }
}

data class VisibilityDay(
    val dayIso: String,
    val views: Int,
    val taps: Int,
    val broadcasts: Int,
    val uniqueVisitors: Int,
)
