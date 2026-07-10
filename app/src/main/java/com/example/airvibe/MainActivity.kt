package com.example.airvibe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.feature.auth.domain.model.AuthStatus
import com.example.airvibe.feature.auth.presentation.AuthScreen
import com.example.airvibe.feature.chat.presentation.ChatScreen
import com.example.airvibe.feature.chat.presentation.ConversationsListScreen
import com.example.airvibe.feature.radar.presentation.RadarScreen
import com.example.airvibe.core.di.ServiceLocator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Si la app se abrió desde una notificación de match,
        // abrimos el chat con el peer indicado en el intent.
        val openWithNodeId = intent?.getStringExtra(EXTRA_OPEN_CHAT_WITH_NODE_ID)
        val fromMatch = intent?.getBooleanExtra(EXTRA_OPEN_CHAT_FROM_MATCH, false) == true
        setContent {
            AirVibeApp(
                initialOpenNodeId = openWithNodeId,
                initialFromMatch = fromMatch,
            )
        }
    }

    companion object {
        const val EXTRA_OPEN_CHAT_WITH_NODE_ID = "airvibe.extra.OPEN_CHAT_WITH_NODE_ID"
        const val EXTRA_OPEN_CHAT_FROM_MATCH = "airvibe.extra.OPEN_CHAT_FROM_MATCH"
    }
}

/**
 * Router raíz de la app. Mantenemos un router state-based (sin
 * Navigation Compose) por consistencia con los pasos anteriores.
 */
@Composable
private fun AirVibeApp(
    initialOpenNodeId: String? = null,
    initialFromMatch: Boolean = false,
) {
    val authStatus by ServiceLocator.authRepository.status.collectAsStateWithLifecycle()
    val darkTheme = remember { false }
    var chatTarget by rememberSaveable(stateSaver = ChatTarget.Saver) {
        mutableStateOf(
            when {
                initialFromMatch && !initialOpenNodeId.isNullOrBlank() ->
                    ChatTarget.ActiveChat(initialOpenNodeId)
                else -> ChatTarget.Closed
            },
        )
    }

    AirVibeTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (authStatus) {
                AuthStatus.Loading -> SplashPlaceholder()
                AuthStatus.SignedOut -> AuthScreen()
                AuthStatus.SignedIn -> when (val target = chatTarget) {
                    ChatTarget.Closed -> RadarScreen(
                        onOpenChats = { chatTarget = ChatTarget.List },
                        onOpenChat = { nodeId -> chatTarget = ChatTarget.ActiveChat(nodeId) },
                    )
                    ChatTarget.List -> ConversationsListScreen(
                        onBack = { chatTarget = ChatTarget.Closed },
                        onOpenConversation = { nodeId ->
                            chatTarget = ChatTarget.ActiveChat(nodeId)
                        },
                    )
                    is ChatTarget.ActiveChat -> ChatScreen(
                        peerNodeId = target.peerNodeId,
                        onBack = {
                            // Si llegamos desde una notificación,
                            // cerramos al radar; si no, volvemos a
                            // la lista.
                            chatTarget = if (initialFromMatch) ChatTarget.Closed else ChatTarget.List
                        },
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        // Si el usuario cierra sesión o se desuscribe, la UI
        // vuelve automáticamente a AuthScreen porque `authStatus`
        // cambia; no hace falta limpiar `chatTarget` manualmente
        // (al re-entrarse al radar se reinicia a Closed).
    }
}

/**
 * Estado de navegación dentro de la feature de chat.
 */
sealed interface ChatTarget {
    data object Closed : ChatTarget
    data object List : ChatTarget
    data class ActiveChat(val peerNodeId: String) : ChatTarget

    companion object {
        val Saver: androidx.compose.runtime.saveable.Saver<ChatTarget, String> =
            androidx.compose.runtime.saveable.Saver(
                save = {
                    when (it) {
                        Closed -> "closed"
                        List -> "list"
                        is ActiveChat -> "chat:${it.peerNodeId}"
                    }
                },
                restore = { value ->
                    when {
                        value == "closed" -> Closed
                        value == "list" -> List
                        value.startsWith("chat:") ->
                            ActiveChat(value.removePrefix("chat:"))
                        else -> Closed
                    }
                },
            )
    }
}

@Composable
private fun SplashPlaceholder() {
    Surface(modifier = Modifier.fillMaxSize()) { }
}
