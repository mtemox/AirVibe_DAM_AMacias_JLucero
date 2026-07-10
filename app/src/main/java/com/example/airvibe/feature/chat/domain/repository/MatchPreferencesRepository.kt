package com.example.airvibe.feature.chat.domain.repository

import com.example.airvibe.feature.chat.domain.model.MatchCriteria
import kotlinx.coroutines.flow.Flow

/**
 * Contrato para persistir y observar los [MatchCriteria] del
 * usuario. La implementación debe sobrevivir al reinicio de la
 * app (DataStore / SharedPreferences) y exponer un [Flow] para
 * que el radar y la pantalla de filtros reaccionen en vivo.
 */
interface MatchPreferencesRepository {

    /** Stream reactivo con los criterios actuales. */
    fun observe(): Flow<MatchCriteria>

    /** Snapshot síncrono (para usar desde el background del scanner). */
    fun current(): MatchCriteria

    /** Reemplaza los criterios activos. */
    suspend fun set(criteria: MatchCriteria)
}
