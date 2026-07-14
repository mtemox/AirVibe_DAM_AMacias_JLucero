package com.example.airvibe.feature.chat.data.device.nearby

/**
 * Tipos de payload de chat que sabe codificar el códec.
 */
enum class NearbyChatPayloadKind {
    Chat,
    GroupInvite,
    RoomMessage,
    FriendAdd,
    HandshakeRequest,
    HandshakeAccept,
    HandshakeReject,
    RoomJoin,
    RoomLeave,
    RoomAnnounce,
}

/**
 * Códec binario para payloads de chat/salas por Nearby Connections.
 *
 * Esquema v2:
 *   v2|chat|<messageId>|<senderNodeId>|<text>|<createdAtMillis>
 *   v2|invite|<messageId>|<senderNodeId>|<text>|<createdAtMillis>|<roomId>|<hostName>
 *   v2|room|<messageId>|<senderNodeId>|<senderName>|<roomId>|<text>|<createdAtMillis>
 *   v2|friend|<messageId>|<senderNodeId>|<displayName>|<status>|<detail>|<tags>|<createdAtMillis>
 *
 * Feature 3 — Handshake:
 *   v2|hs_req|<handshakeId>|<senderNodeId>|<displayName>|<headline>|<status>|<presence>|<tags\u001F…>|<key>|<createdAtMillis>
 *   v2|hs_ok|<handshakeId>|<senderNodeId>|<key>|<createdAtMillis>
 *   v2|hs_no|<handshakeId>|<senderNodeId>|<createdAtMillis>
 *
 * Feature 4 — Salas de Proximidad:
 *   v2|rm_join|<roomId>|<senderNodeId>|<displayName>|<hostNodeId>|<roomTitle>|<createdAtMillis>
 *   v2|rm_leave|<roomId>|<senderNodeId>|<createdAtMillis>
 *   v2|rm_annc|<roomId>|<senderNodeId>|<senderName>|<messageId>|<text>|<createdAtMillis>
 */
internal object NearbyChatPayloadCodec {

    private const val SCHEMA_VERSION = "v2"
    const val TYPE_CHAT = "chat"
    const val TYPE_INVITE = "invite"
    const val TYPE_ROOM = "room"
    const val TYPE_FRIEND = "friend"
    const val TYPE_HANDSHAKE_REQUEST = "hs_req"
    const val TYPE_HANDSHAKE_ACCEPT = "hs_ok"
    const val TYPE_HANDSHAKE_REJECT = "hs_no"
    const val TYPE_ROOM_JOIN = "rm_join"
    const val TYPE_ROOM_LEAVE = "rm_leave"
    const val TYPE_ROOM_ANNOUNCE = "rm_annc"

    private const val FIELD_SEPARATOR = "|"
    private const val DELIMITER = "\u001F"

    fun encodeChat(
        messageId: String,
        senderNodeId: String,
        text: String,
        createdAtMillis: Long,
    ): ByteArray = encode(
        type = TYPE_CHAT,
        messageId = messageId,
        senderNodeId = senderNodeId,
        text = text,
        createdAtMillis = createdAtMillis,
    )

    fun encodeInvite(
        messageId: String,
        senderNodeId: String,
        senderName: String,
        text: String,
        createdAtMillis: Long,
        roomId: String,
    ): ByteArray = encode(
        type = TYPE_INVITE,
        messageId = messageId,
        senderNodeId = senderNodeId,
        text = text,
        createdAtMillis = createdAtMillis,
        roomId = roomId,
        extra = sanitize(senderName),
    )

    fun encodeRoomMessage(
        messageId: String,
        senderNodeId: String,
        senderName: String,
        roomId: String,
        text: String,
        createdAtMillis: Long,
    ): ByteArray {
        val payload = listOf(
            SCHEMA_VERSION,
            TYPE_ROOM,
            messageId,
            senderNodeId,
            sanitize(senderName),
            roomId,
            sanitize(text),
            createdAtMillis.toString(),
        ).joinToString(FIELD_SEPARATOR)
        return payload.toByteArray(Charsets.UTF_8)
    }

    fun encodeFriendAdd(
        messageId: String,
        senderNodeId: String,
        displayName: String,
        status: String,
        detail: String,
        tags: List<String>,
        createdAtMillis: Long,
    ): ByteArray {
        val tagsField = tags.joinToString(",")
        val payload = listOf(
            SCHEMA_VERSION,
            TYPE_FRIEND,
            messageId,
            senderNodeId,
            sanitize(displayName),
            sanitize(status),
            sanitize(detail),
            sanitize(tagsField),
            createdAtMillis.toString(),
        ).joinToString(FIELD_SEPARATOR)
        return payload.toByteArray(Charsets.UTF_8)
    }

    /**
     * Feature 3 — Handshake.
     * Serializa una solicitud de conexión P2P. Incluye un
     * [handshakeId] único, un [key] opaco (la "llave" del feature)
     * y un snapshot del perfil para que el receptor pueda decidir
     * con quién conectar.
     */
    fun encodeHandshakeRequest(
        handshakeId: String,
        senderNodeId: String,
        displayName: String,
        headline: String,
        status: String,
        presence: String,
        tags: List<String>,
        key: String,
        createdAtMillis: Long,
    ): ByteArray {
        val tagsField = tags.joinToString(DELIMITER)
        val payload = listOf(
            SCHEMA_VERSION,
            TYPE_HANDSHAKE_REQUEST,
            sanitize(handshakeId),
            sanitize(senderNodeId),
            sanitize(displayName),
            sanitize(headline),
            sanitize(status),
            sanitize(presence),
            sanitize(tagsField),
            sanitize(key),
            createdAtMillis.toString(),
        ).joinToString(FIELD_SEPARATOR)
        return payload.toByteArray(Charsets.UTF_8)
    }

    fun encodeHandshakeAccept(
        handshakeId: String,
        senderNodeId: String,
        key: String,
        createdAtMillis: Long,
    ): ByteArray {
        val payload = listOf(
            SCHEMA_VERSION,
            TYPE_HANDSHAKE_ACCEPT,
            sanitize(handshakeId),
            sanitize(senderNodeId),
            sanitize(key),
            createdAtMillis.toString(),
        ).joinToString(FIELD_SEPARATOR)
        return payload.toByteArray(Charsets.UTF_8)
    }

    fun encodeHandshakeReject(
        handshakeId: String,
        senderNodeId: String,
        createdAtMillis: Long,
    ): ByteArray {
        val payload = listOf(
            SCHEMA_VERSION,
            TYPE_HANDSHAKE_REJECT,
            sanitize(handshakeId),
            sanitize(senderNodeId),
            createdAtMillis.toString(),
        ).joinToString(FIELD_SEPARATOR)
        return payload.toByteArray(Charsets.UTF_8)
    }

    /**
     * Feature 4 — Un Guest le pide al Host unirse a la sala.
     * El Host responde con un [RoomMessage] de broadcast a todos
     * los miembros (incluyendo el recién llegado).
     */
    fun encodeRoomJoin(
        roomId: String,
        senderNodeId: String,
        displayName: String,
        hostNodeId: String,
        roomTitle: String,
        createdAtMillis: Long,
    ): ByteArray {
        val payload = listOf(
            SCHEMA_VERSION,
            TYPE_ROOM_JOIN,
            sanitize(roomId),
            sanitize(senderNodeId),
            sanitize(displayName),
            sanitize(hostNodeId),
            sanitize(roomTitle),
            createdAtMillis.toString(),
        ).joinToString(FIELD_SEPARATOR)
        return payload.toByteArray(Charsets.UTF_8)
    }

    fun encodeRoomLeave(
        roomId: String,
        senderNodeId: String,
        createdAtMillis: Long,
    ): ByteArray {
        val payload = listOf(
            SCHEMA_VERSION,
            TYPE_ROOM_LEAVE,
            sanitize(roomId),
            sanitize(senderNodeId),
            createdAtMillis.toString(),
        ).joinToString(FIELD_SEPARATOR)
        return payload.toByteArray(Charsets.UTF_8)
    }

    fun encodeRoomAnnounce(
        roomId: String,
        senderNodeId: String,
        senderName: String,
        messageId: String,
        text: String,
        createdAtMillis: Long,
    ): ByteArray {
        val payload = listOf(
            SCHEMA_VERSION,
            TYPE_ROOM_ANNOUNCE,
            sanitize(roomId),
            sanitize(senderNodeId),
            sanitize(senderName),
            sanitize(messageId),
            sanitize(text),
            createdAtMillis.toString(),
        ).joinToString(FIELD_SEPARATOR)
        return payload.toByteArray(Charsets.UTF_8)
    }

    private fun encode(
        type: String,
        messageId: String,
        senderNodeId: String,
        text: String,
        createdAtMillis: Long,
        roomId: String? = null,
        extra: String? = null,
    ): ByteArray {
        val parts = mutableListOf(
            SCHEMA_VERSION,
            type,
            messageId,
            senderNodeId,
            sanitize(text),
            createdAtMillis.toString(),
        )
        if (roomId != null) parts += roomId
        if (extra != null) parts += extra
        return parts.joinToString(FIELD_SEPARATOR).toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): DecodedChatPayload? {
        val text = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull() ?: return null
        val parts = text.split(FIELD_SEPARATOR)
        if (parts.size < 6 || parts[0] != SCHEMA_VERSION) return null

        return when (parts[1]) {
            TYPE_CHAT -> {
                val createdAt = parts[5].toLongOrNull() ?: return null
                DecodedChatPayload(
                    kind = NearbyChatPayloadKind.Chat,
                    messageId = parts[2],
                    senderNodeId = parts[3],
                    text = parts[4],
                    createdAtMillis = createdAt,
                )
            }
            TYPE_INVITE -> {
                val createdAt = parts[5].toLongOrNull() ?: return null
                DecodedChatPayload(
                    kind = NearbyChatPayloadKind.GroupInvite,
                    messageId = parts[2],
                    senderNodeId = parts[3],
                    text = parts[4],
                    createdAtMillis = createdAt,
                    roomId = parts.getOrNull(6),
                    // parts[7] = hostName (added in this version, optional for compat)
                    senderDisplayName = parts.getOrNull(7),
                )
            }
            TYPE_ROOM -> {
                // v2|room: 8 fields with senderName
                // v2|room (old, 7 fields): no senderName
                if (parts.size >= 8) {
                    val createdAt = parts[7].toLongOrNull() ?: return null
                    DecodedChatPayload(
                        kind = NearbyChatPayloadKind.RoomMessage,
                        messageId = parts[2],
                        senderNodeId = parts[3],
                        senderDisplayName = parts[4],
                        roomId = parts[5],
                        text = parts[6],
                        createdAtMillis = createdAt,
                    )
                } else {
                    // legacy 7-field format without senderName
                    if (parts.size < 7) return null
                    val createdAt = parts[6].toLongOrNull() ?: return null
                    DecodedChatPayload(
                        kind = NearbyChatPayloadKind.RoomMessage,
                        messageId = parts[2],
                        senderNodeId = parts[3],
                        senderDisplayName = null,
                        roomId = parts[4],
                        text = parts[5],
                        createdAtMillis = createdAt,
                    )
                }
            }
            TYPE_FRIEND -> {
                if (parts.size < 9) return null
                val createdAt = parts[8].toLongOrNull() ?: return null
                val tags = parts[7].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                DecodedChatPayload(
                    kind = NearbyChatPayloadKind.FriendAdd,
                    messageId = parts[2],
                    senderNodeId = parts[3],
                    text = parts[4],
                    createdAtMillis = createdAt,
                    senderDisplayName = parts[4],
                    senderStatus = parts[5],
                    senderDetail = parts[6],
                    senderTags = tags,
                )
            }
            TYPE_HANDSHAKE_REQUEST -> {
                if (parts.size < 11) return null
                val createdAt = parts[10].toLongOrNull() ?: return null
                val tags = parts[8].split(DELIMITER)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                DecodedChatPayload(
                    kind = NearbyChatPayloadKind.HandshakeRequest,
                    messageId = parts[2],
                    senderNodeId = parts[3],
                    text = parts[4],
                    createdAtMillis = createdAt,
                    senderDisplayName = parts[4],
                    senderHeadline = parts[5],
                    senderStatus = parts[6],
                    senderPresence = parts[7],
                    senderTags = tags,
                    senderHandshakeKey = parts[9],
                )
            }
            TYPE_HANDSHAKE_ACCEPT -> {
                if (parts.size < 6) return null
                val createdAt = parts[5].toLongOrNull() ?: return null
                DecodedChatPayload(
                    kind = NearbyChatPayloadKind.HandshakeAccept,
                    messageId = parts[2],
                    senderNodeId = parts[3],
                    text = "",
                    createdAtMillis = createdAt,
                    senderHandshakeKey = parts[4],
                )
            }
            TYPE_HANDSHAKE_REJECT -> {
                if (parts.size < 5) return null
                val createdAt = parts[4].toLongOrNull() ?: return null
                DecodedChatPayload(
                    kind = NearbyChatPayloadKind.HandshakeReject,
                    messageId = parts[2],
                    senderNodeId = parts[3],
                    text = "",
                    createdAtMillis = createdAt,
                )
            }
            TYPE_ROOM_JOIN -> {
                if (parts.size < 8) return null
                val createdAt = parts[7].toLongOrNull() ?: return null
                DecodedChatPayload(
                    kind = NearbyChatPayloadKind.RoomJoin,
                    messageId = parts[2],
                    senderNodeId = parts[3],
                    text = parts[4],
                    createdAtMillis = createdAt,
                    senderDisplayName = parts[4],
                    roomId = parts[2],
                    senderHeadline = parts[6],
                )
            }
            TYPE_ROOM_LEAVE -> {
                if (parts.size < 5) return null
                val createdAt = parts[4].toLongOrNull() ?: return null
                DecodedChatPayload(
                    kind = NearbyChatPayloadKind.RoomLeave,
                    messageId = parts[2],
                    senderNodeId = parts[3],
                    text = "",
                    createdAtMillis = createdAt,
                    roomId = parts[2],
                )
            }
            TYPE_ROOM_ANNOUNCE -> {
                if (parts.size < 8) return null
                val createdAt = parts[7].toLongOrNull() ?: return null
                DecodedChatPayload(
                    kind = NearbyChatPayloadKind.RoomAnnounce,
                    messageId = parts[5],
                    senderNodeId = parts[3],
                    text = parts[6],
                    createdAtMillis = createdAt,
                    senderDisplayName = parts[4],
                    roomId = parts[2],
                )
            }
            else -> null
        }
    }

    fun looksLikeChatPayload(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val prefix = runCatching { String(bytes.copyOfRange(0, 4), Charsets.UTF_8) }
            .getOrNull() ?: return false
        return prefix.startsWith("v2|")
    }

    fun sanitize(text: String): String = text
        .replace(FIELD_SEPARATOR, " ")
        .replace(DELIMITER, " ")
        .replace("\n", " ")
}

data class DecodedChatPayload(
    val kind: NearbyChatPayloadKind,
    val messageId: String,
    val senderNodeId: String,
    val text: String,
    val createdAtMillis: Long,
    val roomId: String? = null,
    val senderDisplayName: String? = null,
    val senderStatus: String? = null,
    val senderDetail: String? = null,
    val senderTags: List<String> = emptyList(),
    /** Feature 3 — Handshake. Identifica la solicitud de conexión. */
    val senderHandshakeKey: String? = null,
    /** Feature 3 — Handshake. Profesión / título del solicitante. */
    val senderHeadline: String? = null,
    /** Feature 3 — Handshake. Presencia (Online, Available…). */
    val senderPresence: String? = null,
)
