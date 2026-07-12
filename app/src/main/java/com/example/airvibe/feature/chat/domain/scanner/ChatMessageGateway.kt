package com.example.airvibe.feature.chat.domain.scanner

/**
 * Pasarela (gateway) de **salida y entrada** entre el dominio
 * de chat y el hardware Nearby Connections.
 *
 * Mantener esta interfaz en la capa de dominio nos permite
 * cambiar el transporte (BLE, Wi-Fi Aware, mock) sin tocar el
 * ViewModel ni el repositorio.
 */
interface ChatMessageGateway {

    /**
     * Envía un mensaje a un peer concreto. Devuelve `true` si la
     * capa de transporte aceptó el payload (no garantiza
     * entrega).
     */
    suspend fun sendMessage(targetNodeId: String, text: String): Boolean

    /**
     * Envía el mismo texto a **todos** los peers conectados.
     * @deprecated Usar [broadcastRoomInvite] para salas.
     */
    suspend fun broadcast(text: String): Int

    /** Invita a todos los peers conectados a unirse a una sala. */
    suspend fun broadcastRoomInvite(text: String, roomId: String): Int

    /** Envía un mensaje dentro de una sala a todos los peers conectados. */
    suspend fun sendRoomMessage(roomId: String, text: String, messageId: String): Boolean

    /** Notifica a un peer que fue agregado como contacto. */
    suspend fun sendFriendAdd(targetNodeId: String): Boolean

    /**
     * Persiste un mensaje entrante recibido como bytes del
     * transporte. La implementación se encarga de decodificar el
     * payload y guardarlo en Room. Devuelve `true` si el payload
     * era un mensaje de chat válido (y por tanto se procesó).
     */
    fun onIncomingPayload(endpointId: String, bytes: ByteArray): Boolean
}
