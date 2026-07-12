package com.example.airvibe.feature.chat.data.remote

import com.example.airvibe.core.network.toEpochMillisOr
import com.example.airvibe.core.network.toIsoTimestamp
import com.example.airvibe.feature.chat.data.local.entity.ProximityRoomEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteProximityRoomDto(
    @SerialName("id") val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("title") val title: String,
    @SerialName("host_node_id") val hostNodeId: String,
    @SerialName("host_name") val hostName: String,
    @SerialName("joined") val joined: Boolean = false,
    @SerialName("is_host") val isHost: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

fun ProximityRoomEntity.toRemoteDto(ownerId: String): RemoteProximityRoomDto = RemoteProximityRoomDto(
    id = id,
    ownerId = ownerId,
    title = title,
    hostNodeId = hostNodeId,
    hostName = hostName,
    joined = joined,
    isHost = isHost,
    createdAt = createdAt.toIsoTimestamp(),
)

fun RemoteProximityRoomDto.toEntity(): ProximityRoomEntity = ProximityRoomEntity(
    id = id,
    title = title,
    hostNodeId = hostNodeId,
    hostName = hostName,
    createdAt = createdAt.toEpochMillisOr(),
    joined = joined,
    isHost = isHost,
    isSynced = true,
)
