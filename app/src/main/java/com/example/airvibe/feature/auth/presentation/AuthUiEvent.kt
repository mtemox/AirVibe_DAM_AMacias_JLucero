package com.example.airvibe.feature.auth.presentation

/**
 * Eventos que la pantalla de autenticación envía al ViewModel.
 */
sealed interface AuthUiEvent {
    data class EmailChanged(val value: String) : AuthUiEvent
    data class PasswordChanged(val value: String) : AuthUiEvent
    data class DisplayNameChanged(val value: String) : AuthUiEvent
    data class ModeChanged(val mode: AuthMode) : AuthUiEvent
    data object Submit : AuthUiEvent
    data object SignOut : AuthUiEvent
    data object DismissError : AuthUiEvent
    data object DismissInfo : AuthUiEvent
}
