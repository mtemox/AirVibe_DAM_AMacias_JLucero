package com.example.airvibe.feature.chat.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class SupabaseRoomMessageDataSource(
    private val supabase: SupabaseClient,
) {
    suspend fun upsert(messages: List<RemoteRoomMessageDto>): Result<List<String>> = runCatching {
        if (messages.isEmpty()) return@runCatching emptyList()
        supabase.postgrest.from(TABLE).upsert(messages) {
            onConflict = "id"
        }
        messages.map { it.id }
    }

    suspend fun fetchAll(ownerId: String): Result<List<RemoteRoomMessageDto>> = runCatching {
        supabase.postgrest
            .from(TABLE)
            .select {
                filter { eq("owner_id", ownerId) }
            }
            .decodeList<RemoteRoomMessageDto>()
    }

    companion object {
        const val TABLE = "room_messages"
    }
}
