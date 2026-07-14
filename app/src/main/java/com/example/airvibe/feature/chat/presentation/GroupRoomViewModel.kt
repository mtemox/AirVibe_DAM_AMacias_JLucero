package com.example.airvibe.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.chat.domain.model.ProximityRoom
import com.example.airvibe.feature.chat.domain.model.RoomMessage
import com.example.airvibe.feature.chat.domain.repository.ChatRepository
import com.example.airvibe.feature.chat.domain.repository.ProximityRoomRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupRoomUiState(
    val room: ProximityRoom? = null,
    val messages: List<RoomMessage> = emptyList(),
    val isSending: Boolean = false,
    val joined: Boolean = false,
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val memberCount: Int = 0,
)

class GroupRoomViewModel(
    private val roomId: String,
    private val roomRepository: ProximityRoomRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupRoomUiState())
    val uiState: StateFlow<GroupRoomUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            var ready = false
            for (attempt in 0 until 20) {
                if (roomRepository.getRoom(roomId) != null) {
                    ready = true
                    break
                }
                delay(100)
            }
            if (!ready) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadError = "No se encontró la sala. Revisa Salas cercanas o espera la invitación.",
                    )
                }
                return@launch
            }
            roomRepository.joinRoom(roomId)
            _uiState.update { it.copy(isLoading = false, loadError = null) }
        }
        viewModelScope.launch {
            combine(
                roomRepository.observeRoom(roomId),
                roomRepository.observeMessages(roomId),
                roomRepository.observeActiveMembers(roomId),
            ) { room, messages, members ->
                GroupRoomUiState(
                    room = room,
                    messages = messages,
                    joined = room?.joined == true,
                    isLoading = false,
                    memberCount = members.size,
                )
            }.collect { state ->
                _uiState.update {
                    it.copy(
                        room = state.room,
                        messages = state.messages,
                        joined = state.joined,
                        isLoading = it.isLoading && state.room == null,
                        memberCount = state.memberCount,
                    )
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            runCatching { chatRepository.sendRoomMessage(roomId, text) }
            _uiState.update { it.copy(isSending = false) }
        }
    }

    class Factory(
        private val roomId: String,
        private val roomRepository: ProximityRoomRepository = ServiceLocator.proximityRoomRepository,
        private val chatRepository: ChatRepository = ServiceLocator.chatRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GroupRoomViewModel(roomId, roomRepository, chatRepository) as T
        }
    }
}
