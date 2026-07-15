package com.example.airvibe.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.core.ui.feedback.UserMessage
import com.example.airvibe.feature.chat.domain.model.ProximityRoom
import com.example.airvibe.feature.chat.domain.model.RoomMessage
import com.example.airvibe.feature.chat.domain.repository.ChatRepository
import com.example.airvibe.feature.chat.domain.repository.ProximityRoomRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupRoomUiState(
    val room: ProximityRoom? = null,
    val messages: List<RoomMessage> = emptyList(),
    val isSending: Boolean = false,
    val isDeleting: Boolean = false,
    val joined: Boolean = false,
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val memberCount: Int = 0,
    val members: List<com.example.airvibe.feature.chat.domain.model.RoomMember> = emptyList(),
    val avatars: Map<String, String> = emptyMap(),
)

class GroupRoomViewModel(
    private val roomId: String,
    private val roomRepository: ProximityRoomRepository,
    private val chatRepository: ChatRepository,
    private val radarRepository: com.example.airvibe.feature.radar.domain.repository.RadarRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupRoomUiState())
    val uiState: StateFlow<GroupRoomUiState> = _uiState.asStateFlow()

    private val _userMessages = MutableSharedFlow<UserMessage>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val userMessages: SharedFlow<UserMessage> = _userMessages.asSharedFlow()

    init {
        com.example.airvibe.feature.chat.domain.state.ActiveChatState.setRoomChat(roomId)
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
            runCatching { chatRepository.sendRoomJoin(roomId) }
            _uiState.update { it.copy(isLoading = false, loadError = null) }
        }
        viewModelScope.launch {
            combine(
                roomRepository.observeRoom(roomId),
                roomRepository.observeMessages(roomId),
                roomRepository.observeActiveMembers(roomId),
            ) { room, messages, members ->
                val avatars = mutableMapOf<String, String>()
                members.forEach { m ->
                    radarRepository.getProfile(m.nodeId)?.avatarBase64?.let { base64 ->
                        avatars[m.nodeId] = base64
                    }
                }
                messages.forEach { msg ->
                    if (!avatars.containsKey(msg.senderNodeId)) {
                        radarRepository.getProfile(msg.senderNodeId)?.avatarBase64?.let { base64 ->
                            avatars[msg.senderNodeId] = base64
                        }
                    }
                }
                
                GroupRoomUiState(
                    room = room,
                    messages = messages,
                    joined = room?.joined == true,
                    isLoading = false,
                    memberCount = members.size,
                    members = members,
                    avatars = avatars,
                )
            }.collect { state ->
                _uiState.update {
                    it.copy(
                        room = state.room,
                        messages = state.messages,
                        joined = state.joined,
                        isLoading = it.isLoading && state.room == null,
                        memberCount = state.memberCount,
                        members = state.members,
                        avatars = state.avatars,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        com.example.airvibe.feature.chat.domain.state.ActiveChatState.setRoomChat(null)
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            val result = runCatching { chatRepository.sendRoomMessage(roomId, text) }
            _uiState.update { it.copy(isSending = false) }
            result.onSuccess {
                _userMessages.tryEmit(UserMessage.Success("Mensaje enviado"))
            }.onFailure {
                _userMessages.tryEmit(
                    UserMessage.Error(it.message ?: "No se pudo enviar el mensaje"),
                )
            }
        }
    }

    fun leaveOrDeleteRoom() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true) }
        _userMessages.tryEmit(UserMessage.Info("Eliminando sala…"))
        viewModelScope.launch {
            val room = uiState.value.room
            val result = runCatching {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    if (room?.isHost == true) {
                        runCatching { chatRepository.sendRoomDestroy(roomId) }
                    } else {
                        runCatching { chatRepository.sendRoomLeave(roomId) }
                    }
                    roomRepository.deleteRoomLocally(roomId)
                }
            }
            _uiState.update { it.copy(isDeleting = false) }
            result.onSuccess {
                _userMessages.tryEmit(UserMessage.Success("Sala eliminada"))
            }.onFailure {
                _userMessages.tryEmit(
                    UserMessage.Error(it.message ?: "No se pudo eliminar la sala"),
                )
            }
        }
    }

    class Factory(
        private val roomId: String,
        private val roomRepository: ProximityRoomRepository = ServiceLocator.proximityRoomRepository,
        private val chatRepository: ChatRepository = ServiceLocator.chatRepository,
        private val radarRepository: com.example.airvibe.feature.radar.domain.repository.RadarRepository = ServiceLocator.radarRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GroupRoomViewModel(roomId, roomRepository, chatRepository, radarRepository) as T
        }
    }
}
