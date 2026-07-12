package com.example.airvibe.feature.chat.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class SupabaseProximityRoomDataSource(
    private val supabase: SupabaseClient,
) {
    suspend fun upsert(rooms: List<RemoteProximityRoomDto>): Result<List<String>> = runCatching {
        if (rooms.isEmpty()) return@runCatching emptyList()
        supabase.postgrest.from(TABLE).upsert(rooms) {
            onConflict = "id"
        }
        rooms.map { it.id }
    }

    suspend fun fetchAll(ownerId: String): Result<List<RemoteProximityRoomDto>> = runCatching {
        supabase.postgrest
            .from(TABLE)
            .select {
                filter { eq("owner_id", ownerId) }
            }
            .decodeList<RemoteProximityRoomDto>()
    }

    companion object {
        const val TABLE = "proximity_rooms"
    }
}
