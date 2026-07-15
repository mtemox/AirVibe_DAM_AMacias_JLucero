package com.example.airvibe.core.preferences.domain

import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    fun getAppTheme(): Flow<AppTheme>
    fun setAppTheme(theme: AppTheme)
}
