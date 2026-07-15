package com.example.airvibe.core.ui.feedback

/**
 * Mensajes efímeros que un ViewModel emite hacia la UI para
 * mostrar feedback al usuario (snackbars, toasts, banners, etc.).
 *
 * Modelamos los mensajes como una jerarquía sellada para que la
 * UI pueda aplicar estilos distintos en función de la naturaleza
 * del feedback (éxito, error, info) sin tener que parsear strings.
 *
 * El estado en sí (loading, success, error) lo siguen viviendo en
 * el `StateFlow` del feature; este modelo se usa solo para
 * notificaciones one-shot (no se re-emiten al rotar la pantalla).
 */
sealed interface UserMessage {
    val text: String

    data class Success(override val text: String) : UserMessage
    data class Error(override val text: String) : UserMessage
    data class Info(override val text: String) : UserMessage
}
