package com.example.airvibe.feature.radar.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Tipo de nodo visible en el radar. Determina la silueta, color
 * semántico y la información que se muestra al previsualizar.
 */
enum class RadarNodeKind(val displayName: String) {
    Person("Persona"),
    Service("Servicio"),
    Group("Grupo"),
}

/**
 * Estado de presencia del nodo. Sirve para dibujar el indicador en
 * la burbuja y para alimentar la notificación de "disponible".
 */
enum class PresenceStatus(val displayName: String) {
    Online("En línea"),
    Available("Disponible"),
    Busy("Ocupado"),
    Looking("En busca de…"),
    Away("Ausente"),
    Emergency("Emergencia"),
}

/**
 * Modelo inmutable que representa un nodo dentro del radar.
 *
 * @property id identificador estable (MAC hasheada, UUID, etc.)
 * @property displayName nombre a mostrar
 * @property status estado / intención del nodo (ej. "Electricista disponible")
 * @property detail descripción extendida que aparece en el preview
 * @property kind categoría semántica
 * @property presence estado de presencia para el indicador
 * @property angleDegrees posición angular en el radar (0 = norte, sentido horario)
 * @property distanceNormalized distancia radial normalizada (0 = centro, 1 = borde)
 * @property signalStrength intensidad de la señal (0..1)
 * @property accentColor color de acento asociado al nodo
 * @property headline profesión o título corto (separado de [status])
 * @property bio biografía corta (1–2 frases)
 * @property isPremium indica si el peer es Premium y trae un
 *   Payload extendido (Payload v3)
 * @property premiumCatalog portafolio o mini-catálogo de precios
 *   que un Premium puede anunciar (null si no aplica)
 */
data class RadarNode(
    val id: String,
    val displayName: String,
    val status: String,
    val detail: String,
    val kind: RadarNodeKind,
    val presence: PresenceStatus,
    val angleDegrees: Float,
    val distanceNormalized: Float,
    val signalStrength: Float,
    val accentColor: Color,
    val tags: List<String> = emptyList(),
    val headline: String = "",
    val bio: String = "",
    val isPremium: Boolean = false,
    val premiumCatalog: String? = null,
    val avatarUrl: String? = null,
    val avatarBase64: String? = null,
) {
    init {
        require(angleDegrees in 0f..360f) { "angleDegrees must be in 0..360" }
        require(distanceNormalized in 0f..1f) { "distanceNormalized must be in 0..1" }
        require(signalStrength in 0f..1f) { "signalStrength must be in 0..1" }
    }
}
