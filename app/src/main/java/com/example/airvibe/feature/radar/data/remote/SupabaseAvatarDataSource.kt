package com.example.airvibe.feature.radar.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class SupabaseAvatarDataSource(
    private val supabase: SupabaseClient
) {
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): Result<String> = runContext {
        val bucket = supabase.storage.from(AVATARS_BUCKET)
        val fileName = "$userId/${UUID.randomUUID()}.jpg"
        
        // Subimos el archivo
        bucket.upload(fileName, imageBytes) {
            upsert = true
        }
        
        // Obtenemos la URL pública
        val publicUrl = bucket.publicUrl(fileName)
        Result.success(publicUrl)
    }

    private suspend fun <T> runContext(block: suspend () -> Result<T>): Result<T> =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    companion object {
        const val AVATARS_BUCKET = "avatars"
    }
}
