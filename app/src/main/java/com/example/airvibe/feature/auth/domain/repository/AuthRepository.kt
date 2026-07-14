package com.example.airvibe.feature.auth.domain.repository

import com.example.airvibe.feature.auth.domain.model.AuthStatus
import com.example.airvibe.feature.auth.domain.model.AuthUser
import com.example.airvibe.feature.auth.domain.model.SignUpOutcome
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de la capa de autenticación. Define el lenguaje que la
 * UI y los casos de uso entienden; la implementación vive en la
 * capa `data` y se inyecta a través del ServiceLocator.
 */
interface AuthRepository {

    /** Estado reactivo de la sesión. */
    val status: StateFlow<AuthStatus>

    /** Usuario actual si la sesión está activa. */
    val currentUser: StateFlow<AuthUser?>

    /**
     * Inicia sesión con email + password. Devuelve [Result] con
     * el error normalizado (no expone excepciones de GoTrue).
     */
    suspend fun signIn(email: String, password: String): Result<AuthUser>

    /** Registra un usuario nuevo. Puede requerir confirmación por correo. */
    suspend fun signUp(email: String, password: String, displayName: String?): Result<SignUpOutcome>

    /** Cierra la sesión actual. */
    suspend fun signOut(): Result<Unit>

    /**
     * Dispara una sincronización inmediata del trabajo pendiente
     * de WorkManager (no espera a las constraints).
     */
    suspend fun requestSyncNow()
}
