package com.example.airvibe.feature.radar.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

/**
 * Data source PostgREST para la tabla `profile_views` de Supabase.
 *
 * El cliente (viewer) registra sus `RemoteProfileViewDto` y la
 * nube se encarga del resto:
 *  - el `target_user_id` resuelve a `auth.users.id` cuando el
 *    peer Premium se vincula.
 *  - el trigger SQL `trg_profile_views_aggregate` mantiene
 *    `visibility_daily` actualizado.
 *
 * RLS: cualquier `authenticated` puede INSERT, sólo el dueño
 * puede SELECT. El cliente sólo escribe.
 */
class SupabaseTelemetryDataSource(
    private val supabase: SupabaseClient,
) {

    suspend fun upsert(views: List<RemoteProfileViewDto>): Result<List<String>> = runCatching {
        if (views.isEmpty()) return@runCatching emptyList()
        supabase.postgrest.from(TABLE).insert(views)
        views.map { "${it.targetUserId}:${it.sourceNodeId}:${it.createdAt}" }
    }

    suspend fun fetchVisibility(
        ownerId: String,
        fromIso: String,
        toIso: String,
    ): Result<List<RemoteVisibilityDayDto>> = runCatching {
        supabase.postgrest
            .from(VISIBILITY_TABLE)
            .select {
                filter {
                    eq("target_user_id", ownerId)
                    gte("day", fromIso)
                    lte("day", toIso)
                }
            }
            .decodeList<RemoteVisibilityDayDto>()
    }

    companion object {
        const val TABLE = "profile_views"
        const val VISIBILITY_TABLE = "visibility_daily"
    }
}
