package com.example.airvibe.feature.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airvibe.core.designsystem.components.AirVibeAmbientBackground
import com.example.airvibe.core.designsystem.components.AvatarMonogram
import com.example.airvibe.core.designsystem.components.GlassPill
import com.example.airvibe.core.ui.feedback.rememberUserMessages
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

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    val room = state.room
    var composer by remember { mutableStateOf("") }
    val loadError = state.loadError
    var showMenu by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val (snackbarHostState, snackbarFlow) = rememberUserMessages()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFA84D))) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.example.airvibe.R.drawable.wave_pattern),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.5f
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            ChatTopBar(
                peerDisplayName = room?.title ?: "Sala cercana",
                isConnected = loadError == null,
                subtitle = room?.let { "Anfitrión: ${it.hostName}" },
                badgeText = "Sala",
                onBack = onBack,
                onMore = { showMenu = true },
                moreMenu = {
                    androidx.compose.material3.DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(if (room?.isHost == true) "Eliminar Grupo" else "Abandonar Grupo") },
                            onClick = {
                                showMenu = false
                                showConfirmDialog = true
                            }
                        )
                    }
                }
            )
            
            if (showConfirmDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text(if (room?.isHost == true) "Eliminar Grupo" else "Abandonar Grupo") },
                    text = { Text(if (room?.isHost == true) "¿Estás seguro que deseas eliminar este grupo para todos los miembros?" else "¿Estás seguro que deseas abandonar este grupo?") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showConfirmDialog = false
                                viewModel.leaveOrDeleteRoom()
                                onBack()
                            }
                        ) {
                            Text("Sí")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { showConfirmDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
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
                    // -------- Feature 4: header con miembros (nuevo) --------
                    // No reemplaza ninguna parte de la pantalla. Solo
                    // añade una barra con el conteo y avatares
                    // apilados por encima de la lista de mensajes.
                    RoomMembersHeader(
                        memberCount = state.memberCount,
                        members = state.members,
                        avatars = state.avatars,
                        hostName = room?.hostName.orEmpty(),
                    )

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
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(
                            // Pass reversed list so index 0 = newest = renders at bottom
                            items = state.messages.asReversed(),
                            key = { _, it -> it.id },
                        ) { index, message ->
                            val reversedMessages = state.messages.asReversed()
                            val isAboveSameSender = if (index + 1 < reversedMessages.size) {
                                reversedMessages[index + 1].senderNodeId == message.senderNodeId
                            } else false

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (isAboveSameSender) 0.dp else 6.dp),
                                horizontalArrangement = if (message.isOwn) {
                                    Arrangement.End
                                } else {
                                    Arrangement.Start
                                },
                            ) {
                                RoomMessageBubble(
                                    message = message,
                                    isAboveSameSender = isAboveSameSender,
                                    avatarBase64 = state.avatars[message.senderNodeId]
                                )
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

        // Overlay de carga al eliminar la sala. Mantiene la
        // estética de la pantalla sin bloquear el flujo.
        if (state.isDeleting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "Eliminando…",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
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
private fun RoomMessageBubble(
    message: RoomMessage,
    isAboveSameSender: Boolean = false,
    avatarBase64: String? = null,
) {
    val maxBubbleWidth = (LocalConfiguration.current.screenWidthDp * 0.85f).dp
    val topCorner = if (isAboveSameSender) 8.dp else 0.dp
    val bubbleShape = if (message.isOwn) {
        RoundedCornerShape(topStart = 8.dp, topEnd = topCorner, bottomStart = 8.dp, bottomEnd = 8.dp)
    } else {
        RoundedCornerShape(topStart = topCorner, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    }
    val bubbleColor = if (message.isOwn) {
        Color(0xFFDCF8C6)
    } else {
        Color.White
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isOwn) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(end = if (message.isOwn) 48.dp else 36.dp, bottom = 8.dp)
            ) {
                if (!message.isOwn) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        if (!avatarBase64.isNullOrEmpty()) {
                            AvatarMonogram(
                                name = message.senderName,
                                size = 16.dp,
                                imageModel = avatarBase64,
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
                        }
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1A1C1C),
                )
            }

            RoomMessageMetaBox(
                timestampMillis = message.createdAt,
                isOwn = message.isOwn,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun RoomMessageMetaBox(
    timestampMillis: Long,
    isOwn: Boolean,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
    val time = remember(timestampMillis) { timeFormatter.format(java.util.Date(timestampMillis)) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Color(0x991A1C1C),
        )
        if (isOwn) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Enviado",
                tint = Color(0xFF888888),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun groupRoomViewModel(roomId: String): GroupRoomViewModel {
    val factory = remember(roomId) { GroupRoomViewModel.Factory(roomId) }
    return viewModel(factory = factory, key = "room:$roomId")
}

@Composable
private fun RoomMembersHeader(
    memberCount: Int,
    members: List<com.example.airvibe.feature.chat.domain.model.RoomMember>,
    avatars: Map<String, String>,
    hostName: String,
) {
    if (memberCount <= 0 && hostName.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sala activa",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = buildString {
                    append("$memberCount participante${if (memberCount == 1) "" else "s"}")
                    if (hostName.isNotBlank()) {
                        append(" · Anfitrión: ")
                        append(hostName)
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (memberCount >= 2) {
            // Avatares apilados simples
            Row(
                horizontalArrangement = Arrangement.spacedBy((-6).dp),
            ) {
                val displayMembers = members.take(3)
                displayMembers.forEachIndexed { index, member ->
                    val label = member.displayName.ifBlank { "Invitado ${index + 1}" }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .offset(x = (-index * 6).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                    ) {
                        AvatarMonogram(
                            name = label,
                            size = 24.dp,
                            imageModel = avatars[member.nodeId],
                        )
                    }
                }
                if (memberCount > 3) {
                    GlassPill(
                        text = "+${memberCount - 3}",
                    )
                }
            }
        }
    }
}
