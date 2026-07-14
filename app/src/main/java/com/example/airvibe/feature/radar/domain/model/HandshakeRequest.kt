package com.example.airvibe.feature.radar.domain.model

import com.example.airvibe.feature.radar.data.local.entity.HandshakeRequestEntity

/**
 * Estado inmutable que consume la UI para mostrar las
 * solicitudes de conexión entrantes. Es la proyección de
 * dominio de [HandshakeRequestEntity].
 */
data class HandshakeRequest(
    val id: Long,
    val handshakeId: String,
    val peerNodeId: String,
    val peerDisplayName: String,
    val peerHeadline: String,
    val peerStatus: String,
    val peerPresence: PresenceStatus,
    val peerTags: List<String>,
    val handshakeKey: String,
    val direction: Direction,
    val status: Status,
    val createdAt: Long,
    val respondedAt: Long?,
) {
    enum class Direction { Incoming, Outgoing }
    enum class Status { Pending, Accepted, Rejected, Expired, Cancelled }

    companion object {
        fun fromEntity(entity: HandshakeRequestEntity): HandshakeRequest = HandshakeRequest(
            id = entity.id,
            handshakeId = entity.handshakeId,
            peerNodeId = entity.peerNodeId,
            peerDisplayName = entity.peerDisplayName,
            peerHeadline = entity.peerHeadline,
            peerStatus = entity.peerStatus,
            peerPresence = runCatching { PresenceStatus.valueOf(entity.peerPresence) }
                .getOrDefault(PresenceStatus.Online),
            peerTags = entity.peerTags,
            handshakeKey = entity.handshakeKey,
            direction = if (entity.direction == HandshakeRequestEntity.DIRECTION_OUTGOING) {
                Direction.Outgoing
            } else {
                Direction.Incoming
            },
            status = when (entity.status) {
                HandshakeRequestEntity.STATUS_ACCEPTED -> Status.Accepted
                HandshakeRequestEntity.STATUS_REJECTED -> Status.Rejected
                HandshakeRequestEntity.STATUS_EXPIRED -> Status.Expired
                HandshakeRequestEntity.STATUS_CANCELLED -> Status.Cancelled
                else -> Status.Pending
            },
            createdAt = entity.createdAt,
            respondedAt = entity.respondedAt,
        )
    }
}
