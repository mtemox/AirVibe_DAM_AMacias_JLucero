package com.example.airvibe.feature.radar.data.seed

import com.example.airvibe.feature.radar.data.local.entity.NodeEntity
import com.example.airvibe.feature.radar.domain.model.PresenceStatus
import com.example.airvibe.feature.radar.domain.model.RadarNode
import com.example.airvibe.feature.radar.domain.model.RadarNodeKind
import androidx.compose.ui.graphics.Color

/**
 * Datos semilla que se insertan automáticamente la primera vez que
 * la app se abre. Sirven para validar visualmente el radar y para
 * que el servicio de sincronización del paso 4 tenga algo que
 * empujar a Supabase.
 *
 * Los IDs se prefijan con [SEED_ID_PREFIX] para que
 * `RadarDao.clearDiscovered()` pueda limpiar únicamente los nodos
 * descubiertos por Bluetooth sin tocar el contenido del seed.
 *
 * En pasos siguientes esta clase se complementará con datos
 * reales descubiertos vía Nearby Connections.
 */
object RadarSeedData {

    const val SEED_ID_PREFIX: String = "LOCAL_"

    fun seedEntities(): List<NodeEntity> {
        val now = System.currentTimeMillis()
        return seedNodes().map { node ->
            node.toEntity(
                isFavorite = false,
                isSynced = false,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    private fun seedNodes(): List<RadarNode> = listOf(
        RadarNode(
            id = "${SEED_ID_PREFIX}node-001",
            displayName = "Sofía Mendoza",
            status = "En busca de equipo de diseño",
            detail = "Diseñadora UX, abierta a colaborar en proyectos de impacto social.",
            kind = RadarNodeKind.Person,
            presence = PresenceStatus.Looking,
            angleDegrees = 38f,
            distanceNormalized = 0.32f,
            signalStrength = 0.92f,
            accentColor = Color(0xFF6366F1),
            tags = listOf("Diseño", "UX", "Remote"),
        ),
        RadarNode(
            id = "${SEED_ID_PREFIX}node-002",
            displayName = "Carlos Rodríguez",
            status = "Electricista disponible",
            detail = "Instalaciones residenciales y comerciales. Atención inmediata.",
            kind = RadarNodeKind.Service,
            presence = PresenceStatus.Available,
            angleDegrees = 95f,
            distanceNormalized = 0.58f,
            signalStrength = 0.78f,
            accentColor = Color(0xFFF59E0B),
            tags = listOf("Electricidad", "Urgencias 24/7"),
        ),
        RadarNode(
            id = "${SEED_ID_PREFIX}node-003",
            displayName = "Andrea Pérez",
            status = "Networking en cowork central",
            detail = "Lanzando una startup edtech. Conversemos si te interesa la educación.",
            kind = RadarNodeKind.Person,
            presence = PresenceStatus.Online,
            angleDegrees = 165f,
            distanceNormalized = 0.44f,
            signalStrength = 0.85f,
            accentColor = Color(0xFF06B6D4),
            tags = listOf("EdTech", "Fundadora"),
        ),
        RadarNode(
            id = "${SEED_ID_PREFIX}node-004",
            displayName = "Talleres Lucero",
            status = "Mecánica automotriz — abierto",
            detail = "Diagnóstico electrónico, alineación y balanceo. Aceptamos tarjetas.",
            kind = RadarNodeKind.Service,
            presence = PresenceStatus.Available,
            angleDegrees = 220f,
            distanceNormalized = 0.70f,
            signalStrength = 0.62f,
            accentColor = Color(0xFF10B981),
            tags = listOf("Mecánica", "Diagnóstico"),
        ),
        RadarNode(
            id = "${SEED_ID_PREFIX}node-005",
            displayName = "Marlon Vargas",
            status = "DJ para eventos esta noche",
            detail = "Set en vivo desde las 21h. Acepto colaboraciones y bookings.",
            kind = RadarNodeKind.Person,
            presence = PresenceStatus.Online,
            angleDegrees = 285f,
            distanceNormalized = 0.26f,
            signalStrength = 0.95f,
            accentColor = Color(0xFFEC4899),
            tags = listOf("Música", "Eventos"),
        ),
        RadarNode(
            id = "${SEED_ID_PREFIX}node-006",
            displayName = "Sala Coworking Central",
            status = "8 personas conectadas",
            detail = "Sala abierta para chats broadcast. Tema: IA para PYMEs.",
            kind = RadarNodeKind.Group,
            presence = PresenceStatus.Looking,
            angleDegrees = 312f,
            distanceNormalized = 0.62f,
            signalStrength = 0.70f,
            accentColor = Color(0xFF8B5CF6),
            tags = listOf("Grupo", "IA", "PYME"),
        ),
        RadarNode(
            id = "${SEED_ID_PREFIX}node-007",
            displayName = "Lucía Andrade",
            status = "Coach ontológico — 1 cupos",
            detail = "Sesiones express de 25 min. Ideal para founders en bloqueo.",
            kind = RadarNodeKind.Service,
            presence = PresenceStatus.Busy,
            angleDegrees = 135f,
            distanceNormalized = 0.52f,
            signalStrength = 0.74f,
            accentColor = Color(0xFF0EA5E9),
            tags = listOf("Coaching", "Founders"),
        ),
    )

    private fun RadarNode.toEntity(
        isFavorite: Boolean,
        isSynced: Boolean,
        createdAt: Long,
        updatedAt: Long,
    ): NodeEntity = NodeEntity(
        id = id,
        displayName = displayName,
        status = status,
        detail = detail,
        kind = kind.name,
        presence = presence.name,
        angleDegrees = angleDegrees,
        distanceNormalized = distanceNormalized,
        signalStrength = signalStrength,
        accentColorArgb = accentColor.toArgb(),
        tags = tags,
        isFavorite = isFavorite,
        isSynced = isSynced,
        updatedAt = updatedAt,
        createdAt = createdAt,
    )
}

private fun Color.toArgb(): Int {
    val a = (alpha * 255f).toInt().coerceIn(0, 255)
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
