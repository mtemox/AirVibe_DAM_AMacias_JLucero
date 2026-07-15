package com.example.airvibe.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.core.ui.feedback.UserMessage
import com.example.airvibe.feature.chat.domain.model.ProximityRoom
import com.example.airvibe.feature.chat.domain.repository.ChatRepository
import com.example.airvibe.feature.chat.domain.repository.ProximityRoomRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomsListViewModel(
    roomRepository: ProximityRoomRepository = ServiceLocator.proximityRoomRepository,
    private val chatRepository: ChatRepository = ServiceLocator.chatRepository,
) : ViewModel() {
    val rooms: StateFlow<List<ProximityRoom>> = roomRepository.observeActiveRooms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _newRoomId = MutableStateFlow<String?>(null)
    val newRoomId: StateFlow<String?> = _newRoomId.asStateFlow()

    private val _userMessages = MutableSharedFlow<UserMessage>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val userMessages: SharedFlow<UserMessage> = _userMessages.asSharedFlow()

    fun createRoom(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || _isCreating.value) return
        _isCreating.value = true
        _userMessages.tryEmit(UserMessage.Info("Creando sala…"))
        viewModelScope.launch {
            val result = runCatching { chatRepository.broadcast(trimmed) }
            _isCreating.value = false
            result.onSuccess { broadcastResult ->
                if (broadcastResult.roomId.isNotBlank()) {
                    _newRoomId.value = broadcastResult.roomId
                    _userMessages.tryEmit(
                        UserMessage.Success("Sala creada, esperando a los primeros miembros"),
                    )
                } else {
                    _userMessages.tryEmit(
                        UserMessage.Error("No se pudo crear la sala. Inténtalo de nuevo."),
                    )
                }
            }.onFailure {
                _userMessages.tryEmit(
                    UserMessage.Error(it.message ?: "No se pudo crear la sala. Inténtalo de nuevo."),
                )
            }
        }
    }

    fun consumeNewRoomId() {
        _newRoomId.value = null
    }
}
