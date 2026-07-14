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

    // -------- Feature 3: Handshake --------

    /** Envía una solicitud de conexión P2P al peer del nodo. */
    data class SendHandshakeRequest(val nodeId: String) : RadarUiEvent

    /** Limpia el mensaje informativo tras enviar la solicitud. */
    data object ConsumeHandshakeSentMessage : RadarUiEvent

    /** Abre el sheet para revisar una solicitud entrante. */
    data class OpenHandshakeRequest(val handshakeId: String) : RadarUiEvent

    /** Cierra el sheet de la solicitud sin responder. */
    data object DismissHandshakeRequest : RadarUiEvent

    /** Acepta la solicitud activa (la del sheet). */
    data class AcceptHandshakeRequest(val handshakeId: String) : RadarUiEvent

    /** Rechaza la solicitud activa. */
    data class RejectHandshakeRequest(val handshakeId: String) : RadarUiEvent
}
