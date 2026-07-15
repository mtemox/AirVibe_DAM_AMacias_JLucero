package com.example.airvibe.feature.radar.data.remote

import com.example.airvibe.core.network.toEpochMillisOr
import com.example.airvibe.core.network.toIsoTimestamp
import com.example.airvibe.feature.radar.data.local.entity.ProfileViewEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO `@Serializable` para la tabla `profile_views` de Supabase.
 *
 * `target_user_id` debe corresponder a `auth.users.id` del
 * usuario Premium cuyas métricas se están alimentando. El
 * cliente (viewer) sólo escribe; el `target_user_id` resuelve
 * a partir del `nodeId` Bluetooth mediante un mapping
 * server-side (o se deja como `null` si todavía no se ha
 * vinculado a una cuenta).
 */
@Serializable
data class RemoteProfileViewDto(
    @SerialName("target_user_id") val targetUserId: String,
    @SerialName("source_node_id") val sourceNodeId: String,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("kind") val kind: String = "View",
    @SerialName("created_at") val createdAt: String? = null,
)

fun ProfileViewEntity.toRemoteDto(ownerId: String? = null): RemoteProfileViewDto = RemoteProfileViewDto(
    targetUserId = targetUserId,
    sourceNodeId = sourceNodeId,
    ownerId = ownerId,
    kind = kind,
    createdAt = createdAt.toIsoTimestamp(),
)

fun RemoteProfileViewDto.toEntity(): ProfileViewEntity = ProfileViewEntity(
    targetUserId = targetUserId,
    sourceNodeId = sourceNodeId,
    kind = kind,
    createdAt = createdAt.toEpochMillisOr(),
    isSynced = true,
)
