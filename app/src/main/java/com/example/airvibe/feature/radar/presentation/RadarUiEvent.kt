package com.example.airvibe.feature.radar.presentation

import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile

/**
 * Eventos que la UI envía hacia el ViewModel. Usamos un sealed
 * interface para mantener un único punto de entrada a la lógica.
 */
sealed interface RadarUiEvent {
    data class NodeClicked(val nodeId: String) : RadarUiEvent
    data object DismissPreview : RadarUiEvent
    data object ToggleScan : RadarUiEvent
    data object StartScanning : RadarUiEvent
    data object StopScanning : RadarUiEvent
    data object Refresh : RadarUiEvent
    data object Connect : RadarUiEvent
    data object AddToContacts : RadarUiEvent
    data object DismissPermissions : RadarUiEvent
    data object SignOut : RadarUiEvent
    data class UpdateOwnProfile(val profile: ScannerProfile) : RadarUiEvent

    /** Abre la bandeja de entrada de chats. */
    data object OpenChats : RadarUiEvent

    /** Abre el sheet de filtros inteligentes. */
    data object OpenMatchFilters : RadarUiEvent

    /** Cierra el sheet de filtros inteligentes. */
    data object DismissMatchFilters : RadarUiEvent
}
