package com.example.airvibe.feature.chat.presentation

import com.example.airvibe.feature.chat.domain.repository.ConversationSummary

/**
 * Estado inmutable que consume la pantalla de bandeja de
 * entrada (lista de conversaciones).
 */
data class ConversationsListUiState(
    val conversations: List<ConversationSummary> = emptyList(),
    val isLoading: Boolean = true,
    val unsyncedCount: Int = 0,
)
