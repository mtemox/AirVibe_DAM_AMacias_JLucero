package com.example.airvibe.feature.chat.presentation

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airvibe.core.designsystem.components.AirVibeAmbientBackground
import com.example.airvibe.feature.chat.domain.model.MessageDirection
import com.example.airvibe.feature.chat.presentation.components.ChatComposer
import com.example.airvibe.feature.chat.presentation.components.ChatTopBar
import com.example.airvibe.feature.chat.presentation.components.MessageBubble

/**
 * Pantalla de chat con un peer. Estilo iOS 18 con burbujas
 * asimétricas, glassmorfismo y un composer inferior con dos
 * acciones: enviar (1-a-1) y broadcast (a todos).
 *
 * La pantalla es 100% reactiva: observa el `Flow` de Room y
 * hace auto-scroll al último mensaje cuando llega uno nuevo.
 */
@Composable
fun ChatScreen(
    peerNodeId: String,
    onBack: () -> Unit,
    onMore: () -> Unit = {},
    viewModel: ChatViewModel = chatViewModel(peerNodeId = peerNodeId),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        AirVibeAmbientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            ChatTopBar(
                peerDisplayName = state.peerDisplayName,
                isConnected = state.isConnected,
                onBack = onBack,
                onMore = onMore,
            )

            if (state.messages.isEmpty()) {
                EmptyChat(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = state.messages.asReversed(),
                        key = { it.id },
                    ) { message ->
                        val isMine = message.direction == MessageDirection.Outgoing
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMine) {
                                Arrangement.End
                            } else {
                                Arrangement.Start
                            },
                        ) {
                            MessageBubble(message = message)
                        }
                    }
                }
            }

            if (state.lastBroadcastCount > 0) {
                BroadcastAck(count = state.lastBroadcastCount)
            }

            ChatComposer(
                value = state.composer,
                onValueChange = { viewModel.onEvent(ChatUiEvent.ComposerChanged(it)) },
                onSend = { viewModel.onEvent(ChatUiEvent.Send) },
                onBroadcast = { viewModel.onEvent(ChatUiEvent.Broadcast) },
                enabled = !state.isBroadcasting,
                isSending = state.isSending,
                isBroadcasting = state.isBroadcasting,
            )
        }
    }
}

@Composable
private fun EmptyChat(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = "Sin mensajes todavía",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Envía un mensaje o usa la antena para hacer broadcast a todos los peers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BroadcastAck(count: Int) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "Invitación enviada a $count peer${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun chatViewModel(peerNodeId: String): ChatViewModel {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val factory = remember(peerNodeId) {
        ChatViewModel.Factory(peerNodeId = peerNodeId, appContext = application)
    }
    return viewModel(
        key = "chat:$peerNodeId",
        factory = factory,
    )
}
