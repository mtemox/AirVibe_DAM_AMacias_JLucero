package com.example.airvibe.feature.radar.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Gestiona el estado Premium del usuario: "entitlement" (derecho adquirido
 * mediante compra, simulada o real) y "activo" (si el usuario elige mostrarse
 * como Premium en el radar).
 *
 * El entitlement se persiste localmente y se sincroniza con Supabase
 * profiles.is_premium. El estado activo se controla desde el perfil local
 * y se transmite en el payload P2P.
 */
interface PremiumRepository {

    /** Flujo del estado de entitlement. */
    fun observeEntitlement(): Flow<Boolean>

    /** Estado actual de entitlement (compra completada). */
    fun hasEntitlement(): Boolean

    /** Estado actual: el usuario eligió mostrarse como Premium. */
    fun isActive(): Boolean

    /**
     * Simula una compra Premium: guarda entitlement localmente,
     * activa Premium por defecto, y sincroniza con Supabase vía RPC.
     * @param catalog Catálogo de servicios opcional.
     * @return Result.success si todo OK.
     */
    suspend fun purchasePremium(catalog: String? = null): Result<Unit>

    /**
     * Activa o desactiva el modo Premium (solo si tiene entitlement).
     * Cuando se desactiva, el catálogo se mantiene en prefs para restaurarlo
     * cuando se reactive.
     */
    suspend fun setActive(active: Boolean): Result<Unit>

    /**
     * Revoca el entitlement (para testing o admin).
     */
    suspend fun revokeEntitlement(): Result<Unit>
}
