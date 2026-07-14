package com.example.airvibe.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.auth.domain.model.SignUpOutcome
import com.example.airvibe.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla de autenticación. Sigue MVVM con un
 * único [StateFlow] inmutable.
 */
class AuthViewModel(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged -> _uiState.update { it.copy(email = event.value, errorMessage = null) }
            is AuthUiEvent.PasswordChanged -> _uiState.update { it.copy(password = event.value, errorMessage = null) }
            is AuthUiEvent.DisplayNameChanged -> _uiState.update { it.copy(displayName = event.value, errorMessage = null) }
            is AuthUiEvent.ModeChanged -> _uiState.update { it.copy(mode = event.mode, errorMessage = null, infoMessage = null) }
            AuthUiEvent.Submit -> submit()
            AuthUiEvent.SignOut -> signOut()
            AuthUiEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            AuthUiEvent.DismissInfo -> _uiState.update { it.copy(infoMessage = null) }
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (!state.canSubmit || state.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            when (state.mode) {
                AuthMode.SignIn -> {
                    repository.signIn(state.email, state.password).fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(isSubmitting = false, password = "", errorMessage = null)
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    errorMessage = error.message ?: "Error desconocido",
                                )
                            }
                        },
                    )
                }
                AuthMode.SignUp -> {
                    repository.signUp(
                        email = state.email,
                        password = state.password,
                        displayName = state.displayName.takeIf { it.isNotBlank() },
                    ).fold(
                        onSuccess = { outcome ->
                            when (outcome) {
                                is SignUpOutcome.SignedIn -> {
                                    _uiState.update {
                                        it.copy(
                                            isSubmitting = false,
                                            password = "",
                                            errorMessage = null,
                                            infoMessage = null,
                                        )
                                    }
                                }
                                is SignUpOutcome.ConfirmationRequired -> {
                                    _uiState.update {
                                        it.copy(
                                            isSubmitting = false,
                                            mode = AuthMode.SignIn,
                                            password = "",
                                            errorMessage = null,
                                            infoMessage =
                                                "Te enviamos un correo a ${outcome.email}. " +
                                                "Ábrelo, confirma tu cuenta y luego inicia sesión.",
                                        )
                                    }
                                }
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    errorMessage = error.message ?: "Error desconocido",
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _uiState.update { AuthUiState() }
        }
    }

    class Factory(
        private val repository: AuthRepository = ServiceLocator.authRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                "Unknown ViewModel class: $modelClass"
            }
            return AuthViewModel(repository) as T
        }
    }
}
