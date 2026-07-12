package com.example.airvibe.feature.chat.data.device.nearby

/**
 * Tipos de payload de chat que sabe codificar el códec.
 */
enum class NearbyChatPayloadKind {
    Chat,
    GroupInvite,
    RoomMessage,
    FriendAdd,
}

/**
 * Códec binario para payloads de chat/salas por Nearby Connections.
 *
 * Esquema v2:
 *   v2|chat|<messageId>|<senderNodeId>|<text>|<createdAtMillis>
 *   v2|invite|<messageId>|<senderNodeId>|<text>|<createdAtMillis>|<roomId>|<hostName>
 *   v2|room|<messageId>|<senderNodeId>|<senderName>|<roomId>|<text>|<createdAtMillis>
 *   v2|friend|<messageId>|<senderNodeId>|<displayName>|<status>|<detail>|<tags>|<createdAtMillis>
 */
internal object NearbyChatPayloadCodec {

    private const val SCHEMA_VERSION = "v2"
    const val TYPE_CHAT = "chat"
    const val TYPE_INVITE = "invite"
    const val TYPE_ROOM = "room"
    const val TYPE_FRIEND = "friend"

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
)
