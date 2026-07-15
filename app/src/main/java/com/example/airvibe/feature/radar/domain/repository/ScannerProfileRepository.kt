package com.example.airvibe.feature.radar.domain.repository

import com.example.airvibe.feature.radar.domain.model.PresenceStatus
import com.example.airvibe.feature.radar.domain.model.RadarNodeKind
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

    /**
     * Persistir el "tipo" de nodo anunciado (Persona, Servicio o
     * Grupo). Se usa para colorear la burbuja de los peers en su
     * radar.
     */
    suspend fun updateKind(kind: RadarNodeKind)

    /** Persistir el estado de presencia actual (Online, Available…). */
    suspend fun updatePresence(presence: PresenceStatus)

    /**
     * Persistir la profesión / título corto del usuario
     * (Feature 2 — Payload). Aparece en el `headline` del
     * preview y se incluye en el Payload v3.
     */
    suspend fun updateHeadline(headline: String)

    /**
     * Persistir la biografía corta (1–2 frases). Aparece en el
     * preview sheet al tocar un nodo.
     */
    suspend fun updateBio(bio: String)

    /**
     * Activar o desactivar el modo Premium. Cuando se activa, se
     * transmite un Payload v3 con [premiumCatalog] (si fue
     * establecido) para que los peers lo vean en su radar sin
     * necesidad de "hacer match".
     */
    suspend fun updatePremium(isPremium: Boolean, catalog: String? = null)
    
    /** Guarda la foto de perfil en versión URL (alta calidad) y en versión Base64 (Bluetooth) */
    suspend fun updateAvatar(avatarUrl: String?, avatarBase64: String?)

    /** Sincroniza el nombre desde Supabase Auth si el usuario no lo editó. */
    suspend fun applyAuthDisplayName(displayName: String?)

    /** Empuja el perfil editable a Supabase (tabla profiles). */
    suspend fun syncToRemote(profile: ScannerProfile)

    /** Restaura el perfil desde Supabase si existe. */
    suspend fun restoreFromRemote(userId: String)
}
