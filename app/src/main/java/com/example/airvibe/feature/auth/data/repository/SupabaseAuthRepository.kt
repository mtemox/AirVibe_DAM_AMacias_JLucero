package com.example.airvibe.feature.auth.data.repository

import com.example.airvibe.feature.auth.data.dto.toDomain
import com.example.airvibe.feature.auth.domain.model.AuthStatus
import com.example.airvibe.feature.auth.domain.model.AuthUser
import com.example.airvibe.feature.auth.domain.repository.AuthRepository
import com.example.airvibe.feature.radar.data.sync.SyncScheduler
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Implementación de [AuthRepository] respaldada por el SDK de
 * Supabase (módulo `auth-kt`).
 *
 *  - Observa [SessionStatus] para mantener `status` y
 *    `currentUser` actualizados ante refresh de token o signOut.
 *  - Normaliza los errores a [Result.failure] para que la UI
 *    nunca reciba excepciones tipadas de GoTrue.
 */
class SupabaseAuthRepository(
    private val supabase: SupabaseClient,
    private val onSignedIn: suspend () -> Unit = {},
    private val onSignedOut: suspend () -> Unit = {},
) : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob())

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    override val status: StateFlow<AuthStatus> = supabase.auth.sessionStatus
        .map { it.toAuthStatus() }
        .onEach { newStatus ->
            _currentUser.value = if (newStatus == AuthStatus.SignedIn) {
                currentUserOrNull()
            } else {
                null
            }
            when (newStatus) {
                AuthStatus.SignedIn -> onSignedIn()
                AuthStatus.SignedOut -> onSignedOut()
                AuthStatus.Loading -> Unit
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, AuthStatus.Loading)

    override suspend fun signIn(email: String, password: String): Result<AuthUser> = runCatching {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        currentUserOrNull() ?: error("Inicio de sesión sin usuario activo")
    }.recoverCatching { throwable ->
        throw throwable.toAuthException()
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String?,
    ): Result<AuthUser> = runCatching {
        val metadata = buildMap {
            if (!displayName.isNullOrBlank()) put("display_name", displayName)
        }.takeIf { it.isNotEmpty() }
            ?.let { entries -> JsonObject(entries.mapValues { JsonPrimitive(it.value) }) }

        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            if (metadata != null) {
                this.data = metadata
            }
        }
        currentUserOrNull() ?: error("Registro sin usuario activo")
    }.recoverCatching { throwable ->
        throw throwable.toAuthException()
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
    }

    override suspend fun requestSyncNow() {
        SyncScheduler.requestNow()
    }

    /**
     * Recupera el usuario desde la sesión activa sin suspender
     * (sólo lee el `StateFlow`).
     */
    private fun currentUserOrNull(): AuthUser? {
        val state = supabase.auth.sessionStatus.value
        return if (state is SessionStatus.Authenticated) {
            state.session.user?.toDomain()
        } else {
            null
        }
    }

    private fun SessionStatus.toAuthStatus(): AuthStatus = when (this) {
        is SessionStatus.Initializing -> AuthStatus.Loading
        is SessionStatus.NotAuthenticated -> AuthStatus.SignedOut
        is SessionStatus.Authenticated -> AuthStatus.SignedIn
        is SessionStatus.RefreshFailure -> AuthStatus.SignedOut
    }

    /**
     * Mapea excepciones crípticas de GoTrue a mensajes amigables
     * que se mostrarán en la UI.
     */
    private fun Throwable.toAuthException(): Throwable {
        val message = this.message.orEmpty().lowercase()
        val friendly = when {
            "invalid login" in message || "invalid_grant" in message ->
                "Email o contraseña incorrectos."
            "user already registered" in message ->
                "Este email ya está registrado. Inicia sesión."
            "password" in message && "at least" in message ->
                "La contraseña debe tener al menos 6 caracteres."
            "email" in message && "invalid" in message ->
                "El email no es válido."
            "network" in message || "timeout" in message ->
                "Sin conexión. Verifica tu Wi-Fi o datos móviles."
            else -> "Algo salió mal. Inténtalo de nuevo."
        }
        return IllegalStateException(friendly, this)
    }
}
