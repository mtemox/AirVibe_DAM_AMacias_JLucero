package com.example.airvibe.feature.chat.presentation

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airvibe.core.designsystem.components.AirVibeAmbientBackground
import com.example.airvibe.feature.chat.domain.model.RoomMessage
import com.example.airvibe.feature.chat.presentation.components.ChatComposer
import com.example.airvibe.feature.chat.presentation.components.ChatTopBar

@Composable
fun GroupRoomScreen(
    roomId: String,
    onBack: () -> Unit,
    viewModel: GroupRoomViewModel = groupRoomViewModel(roomId),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val room = state.room
    var composer by remember { mutableStateOf("") }
    val loadError = state.loadError

    Box(modifier = Modifier.fillMaxSize()) {
        AirVibeAmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            ChatTopBar(
                peerDisplayName = room?.title ?: "Sala cercana",
                isConnected = loadError == null,
                subtitle = room?.let { "Anfitrión: ${it.hostName}" },
                badgeText = "Sala",
                onBack = onBack,
            )

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Abriendo sala…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                loadError != null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = loadError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                else -> {
                    // reverseLayout=true: newest message at the bottom, scroll up for history
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = listState,
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
                            // Pass reversed list so index 0 = newest = renders at bottom
                            items = state.messages.asReversed(),
                            key = { it.id },
                        ) { message ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (message.isOwn) {
                                    Arrangement.End
                                } else {
                                    Arrangement.Start
                                },
                            ) {
                                RoomMessageBubble(message = message)
                            }
                        }
                    }
                }
            }

            ChatComposer(
                value = composer,
                onValueChange = { composer = it },
                onSend = {
                    viewModel.sendMessage(composer)
                    composer = ""
                },
                onBroadcast = {},
                enabled = !state.isSending && loadError == null && !state.isLoading,
                isSending = state.isSending,
                isBroadcasting = false,
                showBroadcast = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RoomMessageBubble(message: RoomMessage) {
    val maxBubbleWidth = (LocalConfiguration.current.screenWidthDp * 0.78f).dp
    val bg = if (message.isOwn) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }
    Column(
        modifier = Modifier
            .widthIn(max = maxBubbleWidth)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (!message.isOwn) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun groupRoomViewModel(roomId: String): GroupRoomViewModel {
    val factory = remember(roomId) { GroupRoomViewModel.Factory(roomId) }
    return viewModel(factory = factory, key = "room:$roomId")
}
