package com.example.airvibe.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airvibe.feature.auth.presentation.components.AuthScreenContent

/**
 * Punto de entrada de la pantalla de autenticación. Crea su
 * propio [AuthViewModel] y renderiza el contenido glass.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory()),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AuthScreenContent(state = state, onEvent = viewModel::onEvent)
}
