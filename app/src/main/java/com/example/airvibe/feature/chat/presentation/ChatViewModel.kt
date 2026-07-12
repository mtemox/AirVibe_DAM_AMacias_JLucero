package com.example.airvibe.feature.chat.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.chat.domain.repository.ChatRepository
import com.example.airvibe.feature.radar.domain.repository.RadarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla de chat. Sigue MVVM con un único
 * [StateFlow] inmutable.
 *
 * La conversación se reconstruye reactivamente desde Room (la
 * única fuente de verdad). El envío y el broadcast se delegan al
 * [ChatRepository], que a su vez coordina con el gateway
 * Bluetooth.
 */
class ChatViewModel(
    private val peerNodeId: String,
    private val chatRepository: ChatRepository,
    private val radarRepository: RadarRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(peerNodeId = peerNodeId))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeMessages()
        loadPeerDisplayName()
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatRepository.observeConversation(peerNodeId)
                .onEach { messages ->
                    _uiState.update { it.copy(messages = messages, errorMessage = null) }
                }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = throwable.message ?: "Error al cargar el chat")
                    }
                }
                .collect()
        }
    }

    private fun loadPeerDisplayName() {
        viewModelScope.launch {
            val profile = radarRepository.getProfile(peerNodeId)
            val name = profile?.displayName?.takeIf { it.isNotBlank() } ?: peerNodeId
            _uiState.update { it.copy(peerDisplayName = name) }
        }
    }

    fun onEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.ComposerChanged -> _uiState.update { it.copy(composer = event.value) }
            ChatUiEvent.Send -> send()
            ChatUiEvent.Broadcast -> broadcast()
            ChatUiEvent.ClearConversation -> clearConversation()
            ChatUiEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun send() {
        val state = _uiState.value
        if (!state.canSend) return
        val text = state.composer
        _uiState.update { it.copy(isSending = true, composer = "") }
        viewModelScope.launch {
            val result = runCatching {
                chatRepository.sendMessage(peerNodeId, text)
            }
            result.onSuccess {
                _uiState.update { it.copy(isSending = false) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        composer = text,
                        errorMessage = throwable.message ?: "No se pudo enviar el mensaje",
                    )
                }
            }
        }
    }

    private fun broadcast() {
        val state = _uiState.value
        if (state.composer.isBlank()) return
        val text = state.composer
        _uiState.update { it.copy(isBroadcasting = true) }
        viewModelScope.launch {
            val result = runCatching { chatRepository.broadcast(text) }
                .getOrElse { com.example.airvibe.feature.chat.domain.repository.BroadcastResult(0, "") }
            _uiState.update {
                it.copy(
                    isBroadcasting = false,
                    composer = "",
                    lastBroadcastCount = result.recipientCount,
                )
            }
        }
    }

    private fun clearConversation() {
        viewModelScope.launch {
            chatRepository.clearConversation(peerNodeId)
        }
    }

    /**
     * Factory parametrizable. Crea una instancia nueva por cada
     * `peerNodeId` para que las pantallas de chat sean
     * independientes.
     */
    class Factory(
        private val peerNodeId: String,
        private val appContext: Application,
        private val chatRepository: ChatRepository = ServiceLocator.chatRepository,
        private val radarRepository: RadarRepository = ServiceLocator.radarRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                "Unknown ViewModel class: $modelClass"
            }
            return ChatViewModel(
                peerNodeId = peerNodeId,
                chatRepository = chatRepository,
                radarRepository = radarRepository,
            ) as T
        }
    }
}
