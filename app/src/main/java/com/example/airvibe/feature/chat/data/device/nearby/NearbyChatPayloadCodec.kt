package com.example.airvibe.feature.chat.data.device.nearby

/**
 * Tipos de payload de chat que sabe codificar el códec.
 *
 * Lo declaramos a nivel de archivo (público) para que tipos
 * también públicos como [DecodedChatPayload] puedan referenciarlo
 * sin filtrar la visibilidad del [NearbyChatPayloadCodec]
 * (que es `internal`).
 */
enum class NearbyChatPayloadKind {
    Chat,
    GroupInvite,
}

/**
 * Códec binario para los payloads de chat que AirVibe intercambia
 * por Nearby Connections.
 *
 * El formato es CSV con un separador poco común (`\u001F`) para
 * minimizar colisiones con texto del usuario.
 *
 * Esquema del payload (versión 2):
 *
 *     v2|chat|<messageId>|<senderNodeId>|<text>|<createdAtMillis>
 *     v2|invite|<messageId>|<senderNodeId>|<text>|<createdAtMillis>
 *
 * El prefijo `v2` indica el esquema. La segunda columna discrimina
 * entre un mensaje normal y una invitación broadcast. Esto permite
 * que el receptor aplique un estilo distinto sin ambigüedad.
 */
internal object NearbyChatPayloadCodec {

    private const val SCHEMA_VERSION = "v2"
    const val TYPE_CHAT = "chat"
    const val TYPE_INVITE = "invite"

    private const val FIELD_SEPARATOR = "|"
    private const val DELIMITER = "\u001F"

    /** Serializa un mensaje de chat a bytes UTF-8 listos para
     * [com.google.android.gms.nearby.connection.Payload.fromBytes]. */
    fun encode(
        kind: NearbyChatPayloadKind,
        messageId: String,
        senderNodeId: String,
        text: String,
        createdAtMillis: Long,
    ): ByteArray {
        val sanitizedText = text.replace(FIELD_SEPARATOR, " ")
            .replace(DELIMITER, " ")
            .replace("\n", " ")
        val type = when (kind) {
            NearbyChatPayloadKind.Chat -> TYPE_CHAT
            NearbyChatPayloadKind.GroupInvite -> TYPE_INVITE
        }
        val payload = listOf(
            SCHEMA_VERSION,
            type,
            messageId,
            senderNodeId,
            sanitizedText,
            createdAtMillis.toString(),
        ).joinToString(FIELD_SEPARATOR)
        return payload.toByteArray(Charsets.UTF_8)
    }

    /**
     * Resultado de decodificar un payload. Devuelve `null` si
     * el contenido no respeta el esquema de `v2`.
     */
    fun decode(bytes: ByteArray): DecodedChatPayload? {
        val text = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull() ?: return null
        val parts = text.split(FIELD_SEPARATOR)
        if (parts.size < 6) return null
        if (parts[0] != SCHEMA_VERSION) return null
        val type = parts[1]
        val kind = when (type) {
            TYPE_CHAT -> NearbyChatPayloadKind.Chat
            TYPE_INVITE -> NearbyChatPayloadKind.GroupInvite
            else -> return null
        }
        val createdAt = parts[5].toLongOrNull() ?: return null
        return DecodedChatPayload(
            kind = kind,
            messageId = parts[2],
            senderNodeId = parts[3],
            text = parts[4],
            createdAtMillis = createdAt,
        )
    }

    /** Indica si un payload parece ser de chat (para dispatch rápido). */
    fun looksLikeChatPayload(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val prefix = runCatching { String(bytes.copyOfRange(0, 4), Charsets.UTF_8) }
            .getOrNull() ?: return false
        return prefix.startsWith("v2|")
    }
}

/**
 * Payload de chat ya decodificado. El [messageId] que recibimos
 * se usa para deduplicar: si el receptor ya tiene un mensaje con
 * ese id, lo descartamos (caso poco frecuente pero posible si el
 * emisor reintenta).
 */
data class DecodedChatPayload(
    val kind: NearbyChatPayloadKind,
    val messageId: String,
    val senderNodeId: String,
    val text: String,
    val createdAtMillis: Long,
)
