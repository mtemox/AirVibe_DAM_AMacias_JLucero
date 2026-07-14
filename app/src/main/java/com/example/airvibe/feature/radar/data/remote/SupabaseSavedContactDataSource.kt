package com.example.airvibe.feature.radar.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class SupabaseSavedContactDataSource(
    private val supabase: SupabaseClient,
) {
    suspend fun upsert(contacts: List<RemoteSavedContactDto>): Result<List<String>> = runCatching {
        if (contacts.isEmpty()) return@runCatching emptyList()
        supabase.postgrest.from(TABLE).upsert(contacts) {
            onConflict = "owner_id,peer_node_id"
        }
        contacts.map { it.peerNodeId }
    }

    suspend fun fetchAll(ownerId: String): Result<List<RemoteSavedContactDto>> = runCatching {
        supabase.postgrest
            .from(TABLE)
            .select {
                filter { eq("owner_id", ownerId) }
            }
            .decodeList<RemoteSavedContactDto>()
    }

    suspend fun delete(peerNodeId: String, ownerId: String): Result<Unit> = runCatching {
        supabase.postgrest.from(TABLE).delete {
            filter {
                eq("owner_id", ownerId)
                eq("peer_node_id", peerNodeId)
            }
        }
    }

    companion object {
        const val TABLE = "saved_contacts"
    }
}
