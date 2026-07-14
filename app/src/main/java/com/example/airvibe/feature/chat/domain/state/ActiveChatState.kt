package com.example.airvibe.feature.chat.domain.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ActiveChatState {
    private val _currentPeerId = MutableStateFlow<String?>(null)
    val currentPeerId: StateFlow<String?> = _currentPeerId.asStateFlow()

    private val _currentRoomId = MutableStateFlow<String?>(null)
    val currentRoomId: StateFlow<String?> = _currentRoomId.asStateFlow()

    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    fun setAppInForeground(inForeground: Boolean) {
        _isAppInForeground.value = inForeground
    }

    fun setPeerChat(nodeId: String?) {
        _currentPeerId.value = nodeId
    }

    fun setRoomChat(roomId: String?) {
        _currentRoomId.value = roomId
    }
}
