package com.example.airvibe.feature.radar.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.airvibe.feature.radar.data.remote.SupabaseProfileDataSource
import com.example.airvibe.feature.radar.domain.repository.PremiumRepository
import com.example.airvibe.feature.radar.domain.repository.ScannerProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementación del repositorio Premium con compra simulada.
 *
 * Almacena el entitlement en SharedPreferences y sincroniza con Supabase
 * mediante el RPC `apply_premium`. El estado activo se delega al
 * ScannerProfileRepository para mantener coherencia con el payload P2P.
 */
class PremiumRepositoryImpl(
    context: Context,
    private val profileDataSource: SupabaseProfileDataSource,
    private val scannerProfileRepository: ScannerProfileRepository,
    private val currentAuthUserId: () -> String?,
) : PremiumRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _entitlement = MutableStateFlow(prefs.getBoolean(KEY_ENTITLEMENT, false))

    override fun observeEntitlement(): Flow<Boolean> = _entitlement.asStateFlow()

    override fun hasEntitlement(): Boolean = _entitlement.value

    override fun isActive(): Boolean = scannerProfileRepository.current().isPremium

    override suspend fun purchasePremium(catalog: String?): Result<Unit> = runCatching {
        val userId = currentAuthUserId()
            ?: throw IllegalStateException("Debe iniciar sesión para obtener Premium")

        // Simular delay de procesamiento de pago (~1.5s)
        delay(SIMULATED_PURCHASE_DELAY_MS)

        // Guardar entitlement local
        prefs.edit().putBoolean(KEY_ENTITLEMENT, true).apply()
        _entitlement.value = true

        // Activar Premium por defecto con el catálogo
        scannerProfileRepository.updatePremium(isPremium = true, catalog = catalog)

        // Sincronizar con Supabase vía RPC
        profileDataSource.applyPremium(userId, catalog).getOrThrow()

        // Sincronizar perfil completo
        scannerProfileRepository.syncToRemote(scannerProfileRepository.current())
    }

    override suspend fun setActive(active: Boolean): Result<Unit> = runCatching {
        if (!hasEntitlement()) {
            throw IllegalStateException("No tiene suscripción Premium activa")
        }

        // Recuperar catálogo guardado si se está reactivando
        val catalog = if (active) {
            prefs.getString(KEY_SAVED_CATALOG, null)
        } else {
            // Guardar catálogo actual antes de desactivar
            val currentCatalog = scannerProfileRepository.current().premiumCatalog
            if (!currentCatalog.isNullOrBlank()) {
                prefs.edit().putString(KEY_SAVED_CATALOG, currentCatalog).apply()
            }
            null
        }

        scannerProfileRepository.updatePremium(isPremium = active, catalog = catalog)

        // Sincronizar con Supabase
        val userId = currentAuthUserId()
        if (userId != null) {
            scannerProfileRepository.syncToRemote(scannerProfileRepository.current())
        }
    }

    override suspend fun revokeEntitlement(): Result<Unit> = runCatching {
        // Desactivar Premium
        scannerProfileRepository.updatePremium(isPremium = false, catalog = null)

        // Limpiar entitlement local
        prefs.edit()
            .putBoolean(KEY_ENTITLEMENT, false)
            .remove(KEY_SAVED_CATALOG)
            .apply()
        _entitlement.value = false

        // Sincronizar con Supabase
        val userId = currentAuthUserId()
        if (userId != null) {
            scannerProfileRepository.syncToRemote(scannerProfileRepository.current())
        }
    }

    companion object {
        private const val PREFS_NAME = "airvibe.premium"
        private const val KEY_ENTITLEMENT = "premium.entitlement"
        private const val KEY_SAVED_CATALOG = "premium.savedCatalog"
        private const val SIMULATED_PURCHASE_DELAY_MS = 1500L
    }
}
