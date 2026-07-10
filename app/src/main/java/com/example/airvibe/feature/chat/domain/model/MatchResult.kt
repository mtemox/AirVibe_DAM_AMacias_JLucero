package com.example.airvibe.feature.chat.domain.model

import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile

/**
 * Resultado de evaluar un peer contra los [MatchCriteria] activos.
 * Es lo que la UI consume para mostrar el feedback y lo que el
 * [com.example.airvibe.feature.chat.data.notification.MatchNotificationManager]
 * utiliza para decidir si postea una notificación.
 */
sealed interface MatchResult {
    /** El peer no cumple ningún criterio (o el filtro está apagado). */
    data object Ignored : MatchResult

    /**
     * El peer coincide con un criterio, pero ya se le notificó
     * recientemente. Se usa para evitar spam.
     */
    data class Duplicate(val profile: ScannerProfile) : MatchResult

    /**
     * Coincidencia nueva. El caller debería disparar la
     * notificación / feedback.
     */
    data class Match(
        val profile: ScannerProfile,
        val matchedKeyword: String,
    ) : MatchResult
}
