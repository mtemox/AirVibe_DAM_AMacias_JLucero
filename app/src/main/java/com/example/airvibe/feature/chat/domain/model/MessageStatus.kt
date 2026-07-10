package com.example.airvibe.feature.chat.domain.model

/**
 * Ciclo de vida de un mensaje en AirVibe.
 *
 * El modelo es **offline-first**: un mensaje siempre existe en
 * Room desde el instante en que el usuario lo escribe. El flag
 * [Synced] se activa cuando el payload fue entregado al peer
 * Bluetooth y, eventualmente, cuando el worker de Supabase lo
 * replique a la nube.
 */
enum class MessageStatus(val displayName: String) {
    /** Se está serializando y aún no se entregó al peer. */
    Sending("Enviando…"),

    /** El peer lo recibió (ack de Nearby Connections). */
    Sent("Enviado"),

    /** El mensaje no pudo entregarse (peer desconectado, etc.). */
    Failed("Falló"),

    /** Replicado a Supabase por el [SyncWorker]. */
    Synced("Sincronizado"),
}
