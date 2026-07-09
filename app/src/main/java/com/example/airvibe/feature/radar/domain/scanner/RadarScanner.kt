package com.example.airvibe.feature.radar.domain.scanner

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato que el dominio exige a cualquier implementación de radar
 * P2P. La capa de presentación depende **exclusivamente** de esta
 * interfaz, lo que permite sustituir Nearby Connections por BLE,
 * Wi-Fi Aware o una simulación para testing.
 *
 * El scanner es el **único responsable** de traducir la realidad
 * del hardware (peers descubiertos) en cambios persistidos a
 * través de [repository]. La UI nunca recibe eventos crudos de
 * Nearby; observa el `Flow` reactivo de Room.
 */
interface RadarScanner {

    /** Estado reactivo del scanner. */
    val state: StateFlow<ScannerState>

    /**
     * Inicia el escaneo + advertising. Devuelve `false` si el
     * sistema no concedió los permisos o el hardware no está
     * disponible.
     */
    suspend fun start(profile: ScannerProfile): Boolean

    /** Detiene el escaneo + advertising de forma ordenada. */
    suspend fun stop()

    /**
     * Actualiza el perfil anunciado (por ejemplo, cuando el
     * usuario cambia su estado de presencia).
     */
    suspend fun updateProfile(profile: ScannerProfile)
}
