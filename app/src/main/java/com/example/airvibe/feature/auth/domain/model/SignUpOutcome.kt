package com.example.airvibe.feature.auth.domain.model

/**
 * Resultado de un registro con email/contraseña.
 * Con confirmación de correo activa, Supabase no crea sesión hasta verificar.
 */
sealed class SignUpOutcome {
    data class SignedIn(val user: AuthUser) : SignUpOutcome()
    data class ConfirmationRequired(val email: String) : SignUpOutcome()
}
