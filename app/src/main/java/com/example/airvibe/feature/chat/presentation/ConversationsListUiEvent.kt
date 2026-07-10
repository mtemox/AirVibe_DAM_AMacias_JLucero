package com.example.airvibe.feature.chat.presentation

/**
 * Eventos que la bandeja de entrada envía al ViewModel.
 */
sealed interface ConversationsListUiEvent {
    data class OpenConversation(val nodeId: String) : ConversationsListUiEvent
    data object Dismiss : ConversationsListUiEvent
}
