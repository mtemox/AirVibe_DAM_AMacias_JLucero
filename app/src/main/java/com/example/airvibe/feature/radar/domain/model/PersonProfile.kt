package com.example.airvibe.feature.radar.domain.model

/**
 * Resumen completo del perfil que se muestra al tocar un nodo.
 * El modelo de dominio no conoce nada de UI ni de Android.
 *
 * @property headline profesión o título corto. Distinto de [status]
 *   (que es la "vibe" o intención actual, p.ej. "Disponible").
 * @property isPremium indica si el peer paga Premium y trae un
 *   catálogo/portafolio en el payload.
 * @property premiumCatalog portafolio o mini-catálogo (null si
 *   el peer no es Premium o no incluyó catálogo).
 */
data class PersonProfile(
    val id: String,
    val displayName: String,
    val headline: String,
    val bio: String,
    val status: String,
    val presence: PresenceStatus,
    val tags: List<String>,
    val distanceMeters: Int,
    val isFavorite: Boolean,
    val accentHue: Float,
    val isPremium: Boolean = false,
    val premiumCatalog: String? = null,
) {
    companion object {
        fun fromNode(node: RadarNode, distanceMeters: Int = 120): PersonProfile = PersonProfile(
            id = node.id,
            displayName = node.displayName,
            headline = node.headline.ifBlank { node.status },
            bio = node.bio.ifBlank { node.detail },
            status = node.status,
            presence = node.presence,
            tags = node.tags,
            distanceMeters = distanceMeters,
            isFavorite = false,
            accentHue = 0f,
            isPremium = node.isPremium,
            premiumCatalog = node.premiumCatalog,
        )
    }
}
