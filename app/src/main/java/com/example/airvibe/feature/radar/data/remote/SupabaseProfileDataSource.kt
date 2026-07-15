package com.example.airvibe.feature.radar.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseProfileDataSource(
    private val supabase: SupabaseClient,
) {
    suspend fun upsert(profile: RemoteProfileDto): Result<Unit> = runCatching {
        supabase.postgrest.from(PROFILES_TABLE).upsert(profile) {
            onConflict = "id"
        }
    }

    suspend fun fetch(userId: String): Result<RemoteProfileDto?> = runCatching {
        supabase.postgrest
            .from(PROFILES_TABLE)
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingleOrNull<RemoteProfileDto>()
    }

    /**
     * Llama al RPC `apply_premium` de Supabase para activar Premium
     * en el perfil del usuario. Actualiza `is_premium = true`,
     * `premium_catalog` y `premium_since`.
     */
    suspend fun applyPremium(userId: String, catalog: String?): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            function = "apply_premium",
            parameters = ApplyPremiumParams(
                pUserId = userId,
                pCatalog = catalog,
            ),
        )
    }

    @Serializable
    private data class ApplyPremiumParams(
        @SerialName("p_user_id") val pUserId: String,
        @SerialName("p_catalog") val pCatalog: String? = null,
    )

    companion object {
        const val PROFILES_TABLE = "profiles"
    }
}
