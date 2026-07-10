package com.example.airvibe.feature.chat.presentation

/**
 * Eventos que la UI del chat envía hacia el [ChatViewModel].
 */
sealed interface ChatUiEvent {
    data class ComposerChanged(val value: String) : ChatUiEvent
    data object Send : ChatUiEvent
    data object Broadcast : ChatUiEvent
    data object ClearConversation : ChatUiEvent
    data object DismissError : ChatUiEvent
}
