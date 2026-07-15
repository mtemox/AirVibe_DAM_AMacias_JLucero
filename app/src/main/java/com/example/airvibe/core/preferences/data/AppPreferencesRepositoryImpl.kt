package com.example.airvibe.core.preferences.data

import android.content.Context
import android.content.SharedPreferences
import com.example.airvibe.core.preferences.domain.AppPreferencesRepository
import com.example.airvibe.core.preferences.domain.AppTheme
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

class AppPreferencesRepositoryImpl(
    context: Context
) : AppPreferencesRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val cache = MutableStateFlow(readThemeFromDisk())

    override fun getAppTheme(): Flow<AppTheme> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == KEY_THEME) {
                val updated = readThemeFromDisk()
                cache.value = updated
                trySend(updated)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.onStart {
        emit(cache.value)
    }

    override fun setAppTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    private fun readThemeFromDisk(): AppTheme {
        val themeString = prefs.getString(KEY_THEME, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return try {
            AppTheme.valueOf(themeString)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    companion object {
        private const val PREFS_NAME = "airvibe_app_prefs"
        private const val KEY_THEME = "key_app_theme"
    }
}
