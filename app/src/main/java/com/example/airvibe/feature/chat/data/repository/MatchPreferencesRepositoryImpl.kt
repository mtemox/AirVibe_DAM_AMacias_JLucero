package com.example.airvibe.feature.chat.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.airvibe.feature.chat.domain.model.MatchCriteria
import com.example.airvibe.feature.chat.domain.repository.MatchPreferencesRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import org.json.JSONArray

/**
 * Implementación basada en [SharedPreferences] del
 * [MatchPreferencesRepository].
 *
 * SharedPreferences es suficiente para esta cantidad de datos
 * (decenas de keywords) y evita añadir una dependencia pesada
 * como DataStore para una sola preferencia.
 *
 * Escuchamos el `OnSharedPreferenceChangeListener` para emitir
 * los cambios en un [Flow] reactivo. El [MatchEngine] que vive
 * en `data/notification` también se suscribe para que las
 * notificaciones inteligentes respondan en tiempo real.
 */
class MatchPreferencesRepositoryImpl(
    context: Context,
) : MatchPreferencesRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Estado en memoria cacheado para que [current] (snapshot
     * síncrono, usado desde el background) sea O(1) y no requiera
     * I/O en cada peer descubierto.
     */
    private val cache = MutableStateFlow(readFromDisk())

    override fun observe(): Flow<MatchCriteria> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == KEY_ENABLED || key == KEY_KEYWORDS || key == KEY_MIN_SIGNAL) {
                val updated = readFromDisk()
                cache.value = updated
                trySend(updated)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
        .onStart { emit(cache.value) }
        .distinctUntilChanged()

    override fun current(): MatchCriteria = cache.value

    override suspend fun set(criteria: MatchCriteria) {
        prefs.edit().apply {
            putBoolean(KEY_ENABLED, criteria.enabled)
            putFloat(KEY_MIN_SIGNAL, criteria.minSignal)
            putString(KEY_KEYWORDS, encodeKeywords(criteria.keywords))
            apply()
        }
        cache.value = criteria
    }

    private fun readFromDisk(): MatchCriteria = MatchCriteria(
        enabled = prefs.getBoolean(KEY_ENABLED, true),
        keywords = decodeKeywords(prefs.getString(KEY_KEYWORDS, null)),
        minSignal = prefs.getFloat(KEY_MIN_SIGNAL, 0.30f),
    )

    private fun encodeKeywords(keywords: List<String>): String {
        val array = JSONArray()
        keywords.forEach { array.put(it) }
        return array.toString()
    }

    private fun decodeKeywords(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    add(array.optString(i))
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val PREFS_NAME = "airvibe.match_prefs"
        private const val KEY_ENABLED = "criteria.enabled"
        private const val KEY_KEYWORDS = "criteria.keywords"
        private const val KEY_MIN_SIGNAL = "criteria.minSignal"
    }
}
