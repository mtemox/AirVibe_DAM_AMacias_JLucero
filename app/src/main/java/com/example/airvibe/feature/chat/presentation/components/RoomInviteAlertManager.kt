package com.example.airvibe.feature.chat.presentation.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RoomInviteAlert(
    val roomId: String,
    val roomTitle: String,
    val hostName: String,
    val hostNodeId: String,
    val createdAt: Long,
)

object RoomInviteAlertManager {
    private val _currentAlert = MutableStateFlow<RoomInviteAlert?>(null)
    val currentAlert: StateFlow<RoomInviteAlert?> = _currentAlert.asStateFlow()

    fun showInvite(roomId: String, roomTitle: String, hostName: String, hostNodeId: String, createdAt: Long) {
        _currentAlert.value = RoomInviteAlert(roomId, roomTitle, hostName, hostNodeId, createdAt)
    }

    fun dismiss() {
        _currentAlert.value = null
    }
}
