package com.example.airvibe.feature.radar.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

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

    companion object {
        const val PROFILES_TABLE = "profiles"
    }
}
