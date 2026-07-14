package com.example.airvibe.feature.radar.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.airvibe.feature.radar.domain.model.PresenceStatus
import com.example.airvibe.feature.radar.domain.model.RadarNode
import com.example.airvibe.feature.radar.domain.model.RadarNodeKind

/**
 * Token visual (color + icono) que la burbuja del radar debe
 * mostrar para un nodo concreto. Permite que el usuario identifique
 * de un vistazo la **intención** del peer (Networking, Servicio,
 * Grupo, Emergencia…) sin tener que abrir el preview.
 */
data class RadarIntentToken(
    val accent: Color,
    val ring: Color,
    val icon: ImageVector,
    val label: String,
)

/**
 * Resuelve el token visual a partir del tipo de nodo y la
 * presencia. Se aplica una jerarquía:
 *
 *  1. **Emergencia** siempre gana, sea cual sea el `kind`.
 *  2. **Servicio** + Disponible/Ocupado/Buscando usa paleta ámbar.
 *  3. **Grupo** + cualquier presencia usa paleta violeta.
 *  4. **Persona** con presencia de networking o disponible usa
 *     verde o cyan para reforzar el mensaje de "abierto a
 *     conectar".
 */
fun RadarNode.intentToken(): RadarIntentToken {
    if (presence == PresenceStatus.Emergency) {
        return RadarIntentToken(
            accent = Color(0xFFF43F5E),
            ring = Color(0xFFF43F5E),
            icon = Icons.Rounded.Warning,
            label = PresenceStatus.Emergency.displayName,
        )
    }
    return when (kind) {
        RadarNodeKind.Service -> serviceToken()
        RadarNodeKind.Group -> groupToken()
        RadarNodeKind.Person -> personToken()
    }
}

private fun RadarNode.serviceToken(): RadarIntentToken = when (presence) {
    PresenceStatus.Busy -> RadarIntentToken(
        accent = Color(0xFFEF4444),
        ring = Color(0xFFEF4444),
        icon = Icons.Rounded.Build,
        label = "Servicio ocupado",
    )
    PresenceStatus.Available -> RadarIntentToken(
        accent = Color(0xFFF59E0B),
        ring = Color(0xFFF59E0B),
        icon = Icons.Rounded.Build,
        label = "Ofrece servicio",
    )
    else -> RadarIntentToken(
        accent = Color(0xFFF59E0B).copy(alpha = 0.8f),
        ring = Color(0xFFF59E0B).copy(alpha = 0.8f),
        icon = Icons.Rounded.Build,
        label = "Servicio",
    )
}

private fun RadarNode.groupToken(): RadarIntentToken = when (presence) {
    PresenceStatus.Looking -> RadarIntentToken(
        accent = Color(0xFF8B5CF6),
        ring = Color(0xFF8B5CF6),
        icon = Icons.Rounded.Groups,
        label = "Sala abierta",
    )
    else -> RadarIntentToken(
        accent = Color(0xFF8B5CF6).copy(alpha = 0.85f),
        ring = Color(0xFF8B5CF6).copy(alpha = 0.85f),
        icon = Icons.Rounded.Groups,
        label = "Grupo",
    )
}

private fun RadarNode.personToken(): RadarIntentToken = when (presence) {
    PresenceStatus.Looking -> RadarIntentToken(
        accent = Color(0xFF6366F1),
        ring = Color(0xFF6366F1),
        icon = Icons.Rounded.Search,
        label = "Busca equipo / networking",
    )
    PresenceStatus.Available -> RadarIntentToken(
        accent = Color(0xFF06B6D4),
        ring = Color(0xFF06B6D4),
        icon = Icons.Rounded.Handshake,
        label = "Disponible para conectar",
    )
    PresenceStatus.Busy -> RadarIntentToken(
        accent = Color(0xFFEF4444),
        ring = Color(0xFFEF4444),
        icon = Icons.Rounded.Person,
        label = "Ocupado",
    )
    else -> RadarIntentToken(
        accent = accentColor,
        ring = accentColor.copy(alpha = 0.8f),
        icon = Icons.Rounded.Person,
        label = "En línea",
    )
}
