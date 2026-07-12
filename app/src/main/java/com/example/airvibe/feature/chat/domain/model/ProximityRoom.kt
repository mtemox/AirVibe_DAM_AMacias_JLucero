package com.example.airvibe.feature.chat.domain.model

data class ProximityRoom(
    val id: String,
    val title: String,
    val hostNodeId: String,
    val hostName: String,
    val createdAt: Long,
    val joined: Boolean,
    val isHost: Boolean,
)

data class RoomMessage(
    val id: String,
    val roomId: String,
    val senderNodeId: String,
    val senderName: String,
    val text: String,
    val createdAt: Long,
    val isOwn: Boolean,
)
