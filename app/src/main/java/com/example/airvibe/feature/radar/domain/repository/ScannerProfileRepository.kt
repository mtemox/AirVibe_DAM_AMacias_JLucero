package com.example.airvibe.feature.radar.domain.repository

import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile
import kotlinx.coroutines.flow.Flow

/**
 * Perfil que el dispositivo anuncia por Bluetooth. Combina la
 * identidad local estable con los datos editables del usuario.
 */
interface ScannerProfileRepository {

    fun observe(): Flow<ScannerProfile>

    fun current(): ScannerProfile

    suspend fun update(
        displayName: String,
        status: String,
        tags: List<String>,
    )

    /** Sincroniza el nombre desde Supabase Auth si el usuario no lo editó. */
    suspend fun applyAuthDisplayName(displayName: String?)
}
