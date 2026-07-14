package com.example.airvibe.feature.chat.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel de la bandeja de entrada. Combina el Flow de
 * conversaciones con el contador de mensajes no sincronizados
 * para alimentar la UI en un único [StateFlow].
 */
class ConversationsListViewModel(
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsListUiState())
    val uiState: StateFlow<ConversationsListUiState> = _uiState.asStateFlow()

    init {
        observe()
    }

    private fun observe() {
        viewModelScope.launch {
            chatRepository.observeConversations()
                .combine(chatRepository.observeUnsyncedCount()) { conversations, unsynced ->
                    ConversationsListUiState(
                        conversations = conversations,
                        isLoading = false,
                        unsyncedCount = unsynced,
                    )
                }
                .collect { state -> _uiState.value = state }
        }
    }

    fun deleteConversation(nodeId: String) {
        viewModelScope.launch {
            chatRepository.clearConversation(nodeId)
        }
    }

    class Factory(
        @Suppress("UNUSED_PARAMETER") appContext: Application,
        private val chatRepository: ChatRepository = ServiceLocator.chatRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ConversationsListViewModel::class.java)) {
                "Unknown ViewModel class: $modelClass"
            }
            return ConversationsListViewModel(chatRepository) as T
        }
    }
}
