package com.example.airvibe.feature.radar.domain.scanner

import com.example.airvibe.feature.radar.domain.model.PresenceStatus
import com.example.airvibe.feature.radar.domain.model.RadarNodeKind

/**
 * Perfil ligero que el usuario **anuncia** a los demás dispositivos
 * mediante Nearby Connections. Se serializa a bytes para transmitirse
 * como payload de descubrimiento.
 *
 * Mantenemos un tamaño reducido (apenas los campos visibles en la
 * preview) para respetar el límite de Nearby y minimizar el consumo
 * de batería durante el escaneo.
 *
 * @property presence estado de presencia (Online, Available, Busy,
 *   Looking, Away o Emergency). Permite que otros usuarios vean
 *   en su radar si estamos disponibles, ocupados o necesitamos
 *   ayuda, sin abrir el chat.
 * @property headline profesión / título corto (Ej. "Diseñador
 *   Gráfico"). Separado de [status] (que es la "vibe" o intención
 *   actual, p.ej. "Disponible para colaborar").
 * @property bio biografía corta (1–2 frases) que se muestra en
 *   la preview.
 * @property isPremium indica si el usuario paga la suscripción
 *   Premium. Cuando es `true`, [premiumCatalog] viaja en el
 *   payload (Payload v3 extendido).
 * @property premiumCatalog portafolio o mini-catálogo de precios
 *   en texto plano. Solo se serializa si [isPremium] es `true`.
 */
data class ScannerProfile(
    val id: String,
    val displayName: String,
    val status: String,
    val kind: RadarNodeKind,
    val presence: PresenceStatus = PresenceStatus.Online,
    val headline: String = "",
    val bio: String = "",
    val isPremium: Boolean = false,
    val premiumCatalog: String? = null,
    val tags: List<String> = emptyList(),
)
