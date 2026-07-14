package com.example.airvibe.core.network

/**
 * Constantes de conexión con Supabase. Mantenerlas centralizadas
 * facilita un eventual build flavors (staging / production) o
 * un cambio de proyecto.
 */
object SupabaseConfig {

    /**
     * URL del proyecto Supabase. Provista por el equipo.
     */
    const val PROJECT_URL: String = "https://sgdvortojvmfbfauginq.supabase.co"

    /**
     * Clave pública (`sb_publishable_…`). Es segura de exponer
     * en el cliente; la seguridad real reside en las políticas
     * RLS de la base de datos.
     */
    const val PUBLISHABLE_KEY: String =
        "sb_publishable_SU9sfSIXfr8BTHoBNpSkUw_nfiXukkQ"

    /**
     * Tabla remota donde se persisten los nodos del radar del
     * usuario. El SQL esperado en el proyecto de Supabase es:
     *
     * ```sql
     * create table public.radar_nodes (
     *   id text primary key,
     *   owner_id uuid references auth.users(id) default auth.uid(),
     *   display_name text not null,
     *   status text not null default '',
     *   detail text not null default '',
     *   kind text not null default 'Person',
     *   presence text not null default 'Online',
     *   angle_degrees double precision not null default 0,
     *   distance_normalized double precision not null default 0.5,
     *   signal_strength double precision not null default 0.5,
     *   accent_color_argb bigint not null default 0,
     *   tags text[] not null default '{}',
     *   is_favorite boolean not null default false,
     *   updated_at timestamptz not null default now(),
     *   created_at timestamptz not null default now()
     * );
     * alter table public.radar_nodes enable row level security;
     * create policy "owner_all" on public.radar_nodes
     *   for all using (auth.uid() = owner_id)
     *   with check (auth.uid() = owner_id);
     * ```
     */
    const val RADAR_NODES_TABLE: String = "radar_nodes"

    /** Landing page tras confirmar correo (Vercel). Debe coincidir con Site URL en Supabase. */
    const val EMAIL_CONFIRM_REDIRECT_URL: String =
        "https://airvibe.vercel.app/auth/confirm/"
}
