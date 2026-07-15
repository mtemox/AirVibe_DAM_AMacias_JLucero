package com.example.airvibe.feature.chat.domain.repository

import com.example.airvibe.feature.chat.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Contrato que el dominio exige para persistir y servir mensajes
 * de chat. La capa de presentación nunca conoce Room ni el
 * gateway Bluetooth: solo observa los [Flow] reactivos.
 *
 * Esta interfaz es la **única fuente de verdad** para la pantalla
 * de chat. Los mensajes entrantes por Bluetooth se persisten
 * aquí primero (offline-first) y la UI se entera a través del
 * `Flow`.
 */
interface ChatRepository {

    /**
     * Stream reactivo con los mensajes de una conversación,
     * ordenados cronológicamente. La fuente es Room, por lo que
     * cualquier escritura (entrante o saliente) actualiza la UI
     * automáticamente.
     */
    fun observeConversation(peerNodeId: String): Flow<List<ChatMessage>>

    /**
     * Stream con la **lista de conversaciones** activas. Cada
     * entrada resume el último mensaje de un peer para alimentar
     * la bandeja de entrada.
     */
    fun observeConversations(): Flow<List<ConversationSummary>>

    /** Conversaciones cuya última actividad fue un mensaje entrante. */
    fun observeUnreadConversationCount(): Flow<Int>

    /**
     * Cantidad de mensajes pendientes de sincronizar con la nube.
     * Hoy sólo se reportan como métrica; en el futuro se podrían
     * enviar a Supabase.
     */
    fun observeUnsyncedCount(): Flow<Int>

    /**
     * Persiste un mensaje saliente y delega el envío al gateway
     * Bluetooth. Devuelve el mensaje ya persistido (con su `id`).
     */
    suspend fun sendMessage(
        peerNodeId: String,
        text: String,
    ): ChatMessage

    /**
     * Crea una sala y envía invitación a todos los peers conectados.
     * Devuelve cantidad de destinatarios y el id de la sala creada.
     */
    suspend fun broadcast(text: String): BroadcastResult

    /** Elimina el historial de chat con un peer. */
    suspend fun clearConversation(peerNodeId: String)

    /** Marca todos los mensajes con este peer como leídos. */
    suspend fun markConversationAsRead(peerNodeId: String)

    /** Envía un mensaje en una sala de proximidad. */
    suspend fun sendRoomMessage(roomId: String, text: String): com.example.airvibe.feature.chat.domain.model.RoomMessage

    /** Envía la notificación de unión al anfitrión. */
    suspend fun sendRoomJoin(roomId: String)

    /** Abandona la sala y notifica al anfitrión. */
    suspend fun sendRoomLeave(roomId: String)

    /** Destruye la sala y notifica a los participantes. */
    suspend fun sendRoomDestroy(roomId: String)
}

data class ConversationSummary(
    val nodeId: String,
    val displayName: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val isGroupInvite: Boolean,
    val avatarBase64: String? = null,
)

data class BroadcastResult(
    val recipientCount: Int,
    val roomId: String,
)
