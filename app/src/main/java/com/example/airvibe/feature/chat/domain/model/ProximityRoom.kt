package com.example.airvibe.feature.chat.domain.model

data class ProximityRoom(
    val id: String,
    val title: String,
    val hostNodeId: String,
    val hostName: String,
    val createdAt: Long,
    val joined: Boolean,
    val isHost: Boolean,
)

data class RoomMessage(
    val id: String,
    val roomId: String,
    val senderNodeId: String,
    val senderName: String,
    val text: String,
    val createdAt: Long,
    val isOwn: Boolean,
)

/**
 * Feature 4 — Miembro de una sala. Solo se persisten miembros
 * activos. Cuando un Guest sale, su fila se marca como
 * `isActive = false` para mantener historial.
 */
data class RoomMember(
    val id: Long,
    val roomId: String,
    val nodeId: String,
    val displayName: String,
    val role: String,
    val isActive: Boolean,
    val joinedAt: Long,
    val lastSeenAt: Long,
)
