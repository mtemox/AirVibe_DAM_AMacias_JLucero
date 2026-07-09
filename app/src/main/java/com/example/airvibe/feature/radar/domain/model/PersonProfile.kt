package com.example.airvibe.feature.radar.domain.model

/**
 * Resumen completo del perfil que se muestra al tocar un nodo.
 * El modelo de dominio no conoce nada de UI ni de Android.
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
) {
    companion object {
        fun fromNode(node: RadarNode, distanceMeters: Int = 120): PersonProfile = PersonProfile(
            id = node.id,
            displayName = node.displayName,
            headline = node.status,
            bio = node.detail,
            status = node.status,
            presence = node.presence,
            tags = node.tags,
            distanceMeters = distanceMeters,
            isFavorite = false,
            accentHue = 0f,
        )
    }
}
