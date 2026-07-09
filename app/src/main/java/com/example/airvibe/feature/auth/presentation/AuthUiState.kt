package com.example.airvibe.feature.auth.presentation

import com.example.airvibe.feature.auth.domain.model.AuthUser

/**
 * Estado inmutable de la pantalla de autenticación. La UI nunca
 * lo muta; lo recibe del [AuthViewModel] y emite eventos a través
 * de [AuthUiEvent].
 */
data class AuthUiState(
    val mode: AuthMode = AuthMode.SignIn,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val currentUser: AuthUser? = null,
) {
    val canSubmit: Boolean
        get() = email.contains("@") &&
            password.length >= 6 &&
            (mode == AuthMode.SignIn || displayName.isNotBlank())
}

/** Pestaña activa en la pantalla. */
enum class AuthMode { SignIn, SignUp }
