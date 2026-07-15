package com.example.airvibe.feature.chat.presentation

import com.example.airvibe.feature.chat.domain.model.ChatMessage

/**
 * Estado inmutable que consume la pantalla de chat con un peer
 * concreto. La UI nunca muta este objeto: lo recibe del
 * ViewModel y dispara eventos a través de [ChatUiEvent].
 */
data class ChatUiState(
    val peerNodeId: String = "",
    val peerDisplayName: String = "",
    val peerAvatarBase64: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val composer: String = "",
    val isSending: Boolean = false,
    val isConnected: Boolean = true,
    val isBroadcasting: Boolean = false,
    val lastBroadcastCount: Int = 0,
    val errorMessage: String? = null,
) {
    val canSend: Boolean
        get() = !isSending && composer.trim().isNotEmpty()
}
