package com.example.airvibe.feature.chat.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

/**
 * Data source PostgREST para la tabla `chat_messages` de Supabase.
 *
 * Modelo offline-first: cada usuario guarda su propia copia del
 * historial 1-a-1 con [ownerId] = `auth.uid()`. La RLS del backend
 * garantiza que solo el dueño puede leer/escribir sus filas.
 */
class SupabaseChatMessageDataSource(
    private val supabase: SupabaseClient,
) {

    suspend fun upsert(messages: List<RemoteChatMessageDto>): Result<List<String>> = runCatching {
        if (messages.isEmpty()) return@runCatching emptyList()
        supabase.postgrest.from(TABLE).upsert(messages) {
            onConflict = "id"
        }
        messages.map { it.id }
    }

    suspend fun fetchAll(ownerId: String): Result<List<RemoteChatMessageDto>> = runCatching {
        supabase.postgrest
            .from(TABLE)
            .select {
                filter { eq("owner_id", ownerId) }
            }
            .decodeList<RemoteChatMessageDto>()
    }

    companion object {
        const val TABLE = "chat_messages"
    }
}
