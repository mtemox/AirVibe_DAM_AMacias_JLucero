package com.example.airvibe.feature.auth.data.dto

import io.github.jan.supabase.auth.user.UserInfo
import com.example.airvibe.feature.auth.domain.model.AuthUser

/**
 * Adaptador de la `UserInfo` de GoTrue al modelo de dominio
 * `AuthUser`. Mantener este mapeo en `data` garantiza que la
 * capa de dominio nunca importe tipos de Supabase.
 */
internal fun UserInfo.toDomain(): AuthUser = AuthUser(
    id = id,
    email = email.orEmpty(),
    displayName = userMetadata?.get("display_name")?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        ?: email?.substringBefore('@'),
    avatarUrl = userMetadata?.get("avatar_url")?.toString(),
)
