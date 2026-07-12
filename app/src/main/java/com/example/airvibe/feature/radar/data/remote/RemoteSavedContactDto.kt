package com.example.airvibe.feature.radar.data.remote

import com.example.airvibe.core.network.toEpochMillisOr
import com.example.airvibe.core.network.toIsoTimestamp
import com.example.airvibe.feature.radar.data.local.entity.SavedContactEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteSavedContactDto(
    @SerialName("owner_id") val ownerId: String,
    @SerialName("peer_node_id") val peerNodeId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("headline") val headline: String = "",
    @SerialName("bio") val bio: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("presence") val presence: String = "Away",
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("accent_color_argb") val accentColorArgb: Long = 4286619633L,
    @SerialName("added_by_peer") val addedByPeer: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun SavedContactEntity.toRemoteDto(ownerId: String): RemoteSavedContactDto = RemoteSavedContactDto(
    ownerId = ownerId,
    peerNodeId = nodeId,
    displayName = displayName,
    headline = headline,
    bio = bio,
    status = status,
    presence = presence,
    tags = tags,
    accentColorArgb = accentColorArgb.toLong() and 0xFFFFFFFFL,
    addedByPeer = addedByPeer,
    createdAt = createdAt.toIsoTimestamp(),
    updatedAt = updatedAt.toIsoTimestamp(),
)

fun RemoteSavedContactDto.toEntity(): SavedContactEntity = SavedContactEntity(
    nodeId = peerNodeId,
    displayName = displayName,
    headline = headline,
    bio = bio,
    status = status,
    presence = presence,
    tags = tags,
    accentColorArgb = accentColorArgb.toInt(),
    addedByPeer = addedByPeer,
    isSynced = true,
    createdAt = createdAt.toEpochMillisOr(),
    updatedAt = updatedAt.toEpochMillisOr(),
)
