package com.example.airvibe.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.chat.domain.model.ProximityRoom
import com.example.airvibe.feature.chat.domain.repository.ChatRepository
import com.example.airvibe.feature.chat.domain.repository.ProximityRoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun createRoom(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || _isCreating.value) return
        _isCreating.value = true
        viewModelScope.launch {
            val result = runCatching { chatRepository.broadcast(trimmed) }
            _isCreating.value = false
            result.onSuccess { broadcastResult ->
                if (broadcastResult.roomId.isNotBlank()) {
                    _newRoomId.value = broadcastResult.roomId
                }
            }
        }
    }

    fun consumeNewRoomId() {
        _newRoomId.value = null
    }
}
