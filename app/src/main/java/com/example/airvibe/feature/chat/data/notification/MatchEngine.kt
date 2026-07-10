package com.example.airvibe.feature.chat.data.notification

import com.example.airvibe.feature.chat.domain.model.MatchCriteria
import com.example.airvibe.feature.chat.domain.model.MatchResult
import com.example.airvibe.feature.chat.domain.repository.MatchPreferencesRepository
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Motor de **matching** que vive en background (dentro del
 * Foreground Service) y decide si un peer recién descubierto
 * cumple los [MatchCriteria] del usuario.
 *
 * Comportamiento:
 *
 *  1. Lee los criterios del [MatchPreferencesRepository].
 *  2. Cada vez que el scanner nos pasa un perfil por
 *     [onPeerDiscovered], evaluamos el match.
 *  3. Si hay match nuevo, emitimos un [MatchResult.Match] por
 *     un [SharedFlow] para que el
 *     [MatchNotificationManager] postee la alerta.
 *  4. Deduplicamos: el mismo `nodeId` no vuelve a matchear
 *     hasta que pase [dedupeWindowMillis].
 */
class MatchEngine(
    private val preferences: MatchPreferencesRepository,
    private val dedupeWindowMillis: Long = DEFAULT_DEDUPE_WINDOW,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observeJob: Job? = null

    @Volatile
    private var criteria: MatchCriteria = preferences.current()

    private val lastNotifiedAt = ConcurrentHashMap<String, Long>()

    private val _events = MutableSharedFlow<MatchResult>(
        replay = 0,
        extraBufferCapacity = 16,
    )
    val events: SharedFlow<MatchResult> = _events.asSharedFlow()

    fun start() {
        if (observeJob?.isActive == true) return
        observeJob = scope.launch {
            preferences.observe().collect { latest ->
                criteria = latest
            }
        }
    }

    fun stop() {
        observeJob?.cancel()
        observeJob = null
    }

    /**
     * Evalúa un peer contra los criterios activos. Devuelve
     * [MatchResult.Match] si debe notificarse; las otras
     * variantes sólo se usan para que los tests inspeccionen el
     * flujo sin depender del [SharedFlow].
     */
    fun onPeerDiscovered(profile: ScannerProfile): MatchResult {
        val current = criteria
        if (!current.isActive) return MatchResult.Ignored

        val matchedKeyword = current.keywords.firstOrNull { keyword ->
            val needle = keyword.trim().lowercase()
            needle.isNotEmpty() && (
                profile.displayName.lowercase().contains(needle) ||
                    profile.status.lowercase().contains(needle) ||
                    profile.tags.any { it.lowercase().contains(needle) }
                )
        } ?: return MatchResult.Ignored

        val now = System.currentTimeMillis()
        val previous = lastNotifiedAt[profile.id] ?: 0L
        if (now - previous < dedupeWindowMillis) {
            return MatchResult.Duplicate(profile)
        }
        lastNotifiedAt[profile.id] = now

        val match = MatchResult.Match(profile, matchedKeyword)
        _events.tryEmit(match)
        return match
    }

    /** Limpia la memoria de deduplicación. Útil al reiniciar sesión. */
    fun resetDedupe() {
        lastNotifiedAt.clear()
    }

    companion object {
        private const val DEFAULT_DEDUPE_WINDOW = 5L * 60_000L // 5 minutos
    }
}
