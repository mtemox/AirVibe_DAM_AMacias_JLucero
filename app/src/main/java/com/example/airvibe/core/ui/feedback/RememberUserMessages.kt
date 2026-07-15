package com.example.airvibe.core.ui.feedback

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Visuals personalizados que envuelven un [UserMessage] y añaden
 * la `severity` para que la UI pueda aplicar el estilo correcto.
 */
data class UserMessageVisuals(
    override val message: String,
    val severity: Severity,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = severity == Severity.Error
    override val duration: SnackbarDuration
        get() = if (severity == Severity.Error) SnackbarDuration.Long else SnackbarDuration.Short
}

enum class Severity { Success, Error, Info }

/**
 * Crea un `SnackbarHostState` + `MutableSharedFlow<UserMessage>`
 * listos para usar desde un ViewModel.
 */
@Composable
fun rememberUserMessages(): Pair<SnackbarHostState, MutableSharedFlow<UserMessage>> {
    val host = remember { SnackbarHostState() }
    val flow = remember { MutableSharedFlow<UserMessage>(replay = 0, extraBufferCapacity = 8) }
    return host to flow
}

@Composable
fun rememberUserMessages(
    flow: Flow<UserMessage>,
    host: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(flow) {
        flow.collect { message ->
            host.currentSnackbarData?.dismiss()
            host.showSnackbar(
                visuals = UserMessageVisuals(
                    message = message.text,
                    severity = when (message) {
                        is UserMessage.Success -> Severity.Success
                        is UserMessage.Error -> Severity.Error
                        is UserMessage.Info -> Severity.Info
                    },
                ),
            )
        }
    }
    SnackbarHost(
        hostState = host,
        modifier = modifier,
        snackbar = { data -> UserFeedbackSnackbar(data = data) },
    )
}
