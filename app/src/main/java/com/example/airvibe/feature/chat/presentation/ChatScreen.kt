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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import com.example.airvibe.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airvibe.core.designsystem.components.AirVibeAmbientBackground
import com.example.airvibe.core.ui.feedback.rememberUserMessages
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

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    val (snackbarHostState, snackbarFlow) = rememberUserMessages()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bgColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFE8ECEF)

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        val invertMatrix = androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        Image(
            painter = painterResource(id = R.drawable.wave_pattern),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = if (isDark) 0.15f else 0.5f,
            colorFilter = if (isDark) androidx.compose.ui.graphics.ColorFilter.colorMatrix(invertMatrix) else null
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            ChatTopBar(
                peerDisplayName = state.peerDisplayName,
                peerAvatarBase64 = state.peerAvatarBase64,
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
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(
                        items = state.messages.asReversed(),
                        key = { _, it -> it.id },
                    ) { index, message ->
                        val isMine = message.direction == MessageDirection.Outgoing
                        val reversedMessages = state.messages.asReversed()
                        val isAboveSameSender = if (index + 1 < reversedMessages.size) {
                            reversedMessages[index + 1].direction == message.direction
                        } else false

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (isAboveSameSender) 0.dp else 6.dp),
                            horizontalArrangement = if (isMine) {
                                Arrangement.End
                            } else {
                                Arrangement.Start
                            },
                        ) {
                            MessageBubble(
                                message = message,
                                isAboveSameSender = isAboveSameSender
                            )
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

        rememberUserMessages(
            flow = snackbarFlow,
            host = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
        )
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
