package com.example.airvibe.feature.auth.domain.model

/**
 * Usuario autenticado. Es un modelo de **dominio** puro: la capa
 * de presentación no sabe nada de `UserInfo` ni de la sesión
 * interna de GoTrue.
 */
data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String?,
    val avatarUrl: String?,
)

/** Estado de la sesión en el cliente. */
enum class AuthStatus {
    /** Aún no sabemos si hay sesión. La UI puede mostrar un splash. */
    Loading,

    /** El usuario no ha iniciado sesión. */
    SignedOut,

    /** El usuario tiene una sesión activa. */
    SignedIn,
}
