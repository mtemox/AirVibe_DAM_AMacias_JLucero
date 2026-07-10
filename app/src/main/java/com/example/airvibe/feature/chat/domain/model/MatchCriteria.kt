package com.example.airvibe.feature.chat.domain.model

/**
 * Criterio de **matching** que el usuario define para recibir
 * notificaciones inteligentes.
 *
 * El [MatchEngine] compara los `tags` y `status` de cada peer
 * descubierto contra [keywords]. La coincidencia es *case
 * insensitive* y por subcadena: si la palabra "albañil" aparece
 * en cualquiera de los tags o en el `status` del peer, se
 * considera un match.
 *
 * @property enabled     Si el matching está activo.
 * @property keywords    Lista de palabras que el usuario busca.
 * @property minSignal   Fuerza mínima de la señal (0..1) para
 *                       considerar al peer "suficientemente cerca".
 */
data class MatchCriteria(
    val enabled: Boolean = true,
    val keywords: List<String> = emptyList(),
    val minSignal: Float = 0.30f,
) {
    /** Devuelve true si el criterio está bien formado. */
    val isActive: Boolean
        get() = enabled && keywords.any { it.isNotBlank() }

    /**
     * Determina si un peer cumple el criterio. La comparación es
     * insensible a mayúsculas y se hace por subcadena sobre
     * [tags] y [status].
     */
    fun matches(tags: List<String>, status: String): Boolean {
        if (!isActive) return false
        val haystack = buildString {
            append(status.lowercase())
            tags.forEach { append(' '); append(it.lowercase()) }
        }
        return keywords.any { keyword ->
            val needle = keyword.trim().lowercase()
            needle.isNotEmpty() && haystack.contains(needle)
        }
    }
}
