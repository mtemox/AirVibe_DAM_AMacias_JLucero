package com.example.airvibe.feature.radar.domain.scanner

import com.example.airvibe.feature.radar.domain.model.RadarNode
import com.example.airvibe.feature.radar.domain.model.RadarNodeKind

/**
 * Resultado crudo del descubrimiento de un endpoint Bluetooth/Wi-Fi.
 * Contiene:
 *  - El [ScannerProfile] recibido en el payload de Nearby.
 *  - El [endpointId] estable que identifica al dispositivo dentro
 *    de la sesión de Nearby.
 *  - Una métrica de distancia derivada de `getDistanceToEndpoint`
 *    para alimentar la posición del nodo en el radar.
 *  - La calidad de la señal (`0..1`) estimada para modular el
 *    tamaño del nodo en pantalla.
 */
data class DiscoveredPeer(
    val endpointId: String,
    val profile: ScannerProfile,
    val distanceLevel: DistanceLevel,
    val signalStrength: Float,
) {

    /**
     * Convierte el peer en un [RadarNode] listo para que el
     * repositorio lo persista en Room. Mantenemos este mapeo
     * aquí para que la capa de datos solo sepa de tipos de dominio.
     */
    fun toRadarNode(
        accentColor: androidx.compose.ui.graphics.Color,
    ): RadarNode = RadarNode(
        id = profile.id,
        displayName = profile.displayName,
        status = profile.status,
        detail = profile.bio.ifBlank {
            buildString {
                append(profile.status)
                if (profile.tags.isNotEmpty()) {
                    append(" · ")
                    append(profile.tags.joinToString(" · "))
                }
            }
        },
        kind = profile.kind,
        presence = profile.presence,
        angleDegrees = (profile.id.hashCode().toLong() and 0xFFFFFFFFL)
            .let { ((it % 360).toInt()).toFloat() }
            .coerceIn(0f, 359.9f),
        distanceNormalized = distanceLevel.normalized,
        signalStrength = signalStrength,
        accentColor = accentColor,
        tags = profile.tags,
        headline = profile.headline,
        bio = profile.bio,
        isPremium = profile.isPremium,
        premiumCatalog = profile.premiumCatalog,
        avatarUrl = profile.avatarUrl,
        avatarBase64 = profile.avatarBase64,
    )

    companion object {
        /**
         * Calcula la intensidad de la señal a partir del nivel de
         * distancia reportado por Nearby. Es un mapeo razonable que
         * mantiene la coherencia visual del radar: cerca → señal
         * fuerte, lejos → señal débil.
         */
        fun signalFor(distanceLevel: DistanceLevel): Float = when (distanceLevel) {
            DistanceLevel.VERY_CLOSE -> 0.96f
            DistanceLevel.CLOSE -> 0.82f
            DistanceLevel.NEAR -> 0.66f
            DistanceLevel.FAR -> 0.48f
            DistanceLevel.VERY_FAR -> 0.30f
            DistanceLevel.UNKNOWN -> 0.55f
        }
    }
}

/**
 * Categorías oficiales de distancia que expone Nearby Connections
 * a través de [com.google.android.gms.nearby.connection.DistanceInfo].
 */
enum class DistanceLevel(val normalized: Float) {
    UNKNOWN(0.55f),
    VERY_CLOSE(0.18f),
    CLOSE(0.32f),
    NEAR(0.48f),
    FAR(0.68f),
    VERY_FAR(0.86f),
}

internal fun RadarNodeKind.toKindName(): String = name
internal fun String.toNodeKindOrDefault(): RadarNodeKind =
    runCatching { RadarNodeKind.valueOf(this) }.getOrDefault(RadarNodeKind.Person)
