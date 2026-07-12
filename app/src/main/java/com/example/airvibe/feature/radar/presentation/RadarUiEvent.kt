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
    data object ToggleFavorite : RadarUiEvent
    data object DismissPermissions : RadarUiEvent
    /** Muestra el modal de permisos antes de iniciar el escaneo. */
    data object RequestPermissions : RadarUiEvent
    data object SignOut : RadarUiEvent
    data class UpdateOwnProfile(val profile: ScannerProfile) : RadarUiEvent

    /** Abre el editor del perfil propio anunciado por Bluetooth. */
    data object OpenOwnProfile : RadarUiEvent
    data object DismissOwnProfile : RadarUiEvent

    /** Abre el sheet para enviar broadcast a todos los peers. */
    data object OpenBroadcast : RadarUiEvent
    data object DismissBroadcast : RadarUiEvent
    data class SendBroadcast(val text: String) : RadarUiEvent

    /** Abre la bandeja de entrada de chats. */
    data object OpenChats : RadarUiEvent

    /** Abre la lista de amigos guardados. */
    data object OpenFriends : RadarUiEvent

    /** Abre el sheet de filtros inteligentes. */
    data object OpenMatchFilters : RadarUiEvent

    /** Cierra el sheet de filtros inteligentes. */
    data object DismissMatchFilters : RadarUiEvent

    /** Limpia el evento de navegación tras abrir una sala creada. */
    data object ConsumeBroadcastRoomNav : RadarUiEvent

    /** Limpia el mensaje de contacto agregado tras mostrar el snackbar. */
    data object ConsumeContactAddedMessage : RadarUiEvent
}
