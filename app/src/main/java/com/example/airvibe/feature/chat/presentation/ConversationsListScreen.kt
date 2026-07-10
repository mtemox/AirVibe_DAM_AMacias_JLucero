package com.example.airvibe.feature.chat.presentation

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airvibe.core.designsystem.components.AirVibeAmbientBackground
import com.example.airvibe.core.designsystem.components.GlassCard
import com.example.airvibe.core.designsystem.components.GlassPill
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.feature.chat.presentation.components.ConversationRow

/**
 * Bandeja de entrada de chats. Es la pantalla raíz de la feature
 * de chat: muestra todas las conversaciones conocidas (derivadas
 * de la tabla `chat_messages`) y, al tocar una fila, abre el
 * [ChatScreen] con ese peer.
 */
@Composable
fun ConversationsListScreen(
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    viewModel: ConversationsListViewModel = conversationsListViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        AirVibeAmbientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            TopHeader(
                onBack = onBack,
                unsyncedCount = state.unsyncedCount,
            )

            if (state.conversations.isEmpty() && !state.isLoading) {
                EmptyInbox(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(
                        items = state.conversations,
                        key = { it.nodeId },
                    ) { summary ->
                        ConversationRow(
                            summary = summary,
                            onClick = { onOpenConversation(summary.nodeId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopHeader(
    onBack: () -> Unit,
    unsyncedCount: Int,
) {
    val tokens = AirVibeTheme.glass
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 26.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.surfaceFillStrong)
                        .clickableNoRipple(onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chats",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Mensajes offline · P2P",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (unsyncedCount > 0) {
                    GlassPill(
                        text = "$unsyncedCount sin sincronizar",
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyInbox(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Forum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = "Aún no hay conversaciones",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Abre un perfil del radar y toca \"Conectar\" para iniciar un chat.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = source,
        indication = null,
        onClick = onClick,
    )
}

@Composable
private fun conversationsListViewModel(): ConversationsListViewModel {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val factory = remember {
        ConversationsListViewModel.Factory(appContext = application)
    }
    return viewModel(factory = factory)
}
