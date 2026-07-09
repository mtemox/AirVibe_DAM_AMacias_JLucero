package com.example.airvibe.feature.radar.domain.scanner

/**
 * Estado observable del [RadarScanner]. La capa de presentación
 * lo consume para renderizar indicadores (loading, error) y decidir
 * si el modal de permisos debe mostrarse.
 */
sealed interface ScannerState {

    /** El scanner está detenido. No consume recursos. */
    data object Idle : ScannerState

    /** Solicitando el arranque al sistema operativo. */
    data object Starting : ScannerState

    /**
     * El radar está activo. [discovered] es la lista de peers
     * vistos durante la sesión actual (puede ser vacía al inicio).
     */
    data class Active(
        val discovered: Int,
    ) : ScannerState

    /**
     * El scanner no pudo arrancar por una condición recuperable
     * (permisos denegados, hardware apagado, etc.).
     */
    data class Error(val reason: ScannerError) : ScannerState
}

sealed interface ScannerError {
    data object MissingPermissions : ScannerError
    data object BluetoothUnavailable : ScannerError
    data object LocationUnavailable : ScannerError
    data class Unknown(val message: String) : ScannerError
}
