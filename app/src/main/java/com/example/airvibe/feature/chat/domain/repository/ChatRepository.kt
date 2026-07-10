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
     * Envía el mismo texto a **todos** los peers actualmente
     * conectados. Devuelve el número de peers a los que se
     * entregó el payload.
     */
    suspend fun broadcast(text: String): Int

    /** Elimina el historial de chat con un peer. */
    suspend fun clearConversation(peerNodeId: String)
}

/**
 * Resumen de una conversación, derivado de la tabla
 * `chat_messages`. La capa de UI lo consume en la bandeja de
 * entrada.
 */
data class ConversationSummary(
    val nodeId: String,
    val displayName: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val isGroupInvite: Boolean,
)
