package com.example.airvibe.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Centraliza la creación del cliente de Supabase con las
 * dependencias que la app consume (Auth + Postgrest).
 *
 * Mantener un único [SupabaseClient] en toda la vida del proceso
 * es la recomendación oficial: internamente cachea conexiones y
 * reusa la sesión persistida en DataStore.
 */
object SupabaseClientFactory {

    /**
     * Construye un [SupabaseClient] con la configuración de red
     * por defecto. Se centraliza aquí para poder testear o migrar
     * a otra región en un único lugar.
     */
    fun create(): SupabaseClient = createSupabaseClient(
        supabaseUrl = SupabaseConfig.PROJECT_URL,
        supabaseKey = SupabaseConfig.PUBLISHABLE_KEY,
    ) {
        install(Auth)
        install(Postgrest)
    }
}
