package com.example.airvibe

import android.content.Intent
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
import com.example.airvibe.feature.chat.presentation.GroupRoomScreen
import com.example.airvibe.feature.chat.presentation.RoomsListScreen
import com.example.airvibe.feature.radar.presentation.FriendsScreen
import com.example.airvibe.feature.radar.presentation.RadarScreen
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.auth.presentation.components.SplashScreen
import com.example.airvibe.feature.auth.presentation.components.OnboardingScreen

class MainActivity : ComponentActivity() {

    private val deepLinkState = mutableStateOf<AppDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkState.value = parseDeepLink(intent)
        setContent {
            val deepLink by deepLinkState
            AirVibeApp(
                initialDeepLink = deepLink ?: parseDeepLink(intent),
                onDeepLinkHandled = { deepLinkState.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkState.value = parseDeepLink(intent)
    }

    private fun parseDeepLink(intent: Intent?): AppDeepLink? {
        val roomId = intent?.getStringExtra(EXTRA_OPEN_ROOM_ID)
        if (!roomId.isNullOrBlank()) return AppDeepLink.Room(roomId)
        val nodeId = intent?.getStringExtra(EXTRA_OPEN_CHAT_WITH_NODE_ID)
        val fromMatch = intent?.getBooleanExtra(EXTRA_OPEN_CHAT_FROM_MATCH, false) == true
        if (fromMatch && !nodeId.isNullOrBlank()) return AppDeepLink.Chat(nodeId, fromMatch = true)
        return null
    }

    companion object {
        const val EXTRA_OPEN_CHAT_WITH_NODE_ID = "airvibe.extra.OPEN_CHAT_WITH_NODE_ID"
        const val EXTRA_OPEN_CHAT_FROM_MATCH = "airvibe.extra.OPEN_CHAT_FROM_MATCH"
        const val EXTRA_OPEN_ROOM_ID = "airvibe.extra.OPEN_ROOM_ID"
    }
}

private sealed interface AppDeepLink {
    data class Room(val roomId: String) : AppDeepLink
    data class Chat(val nodeId: String, val fromMatch: Boolean = false) : AppDeepLink
}

@Composable
private fun AirVibeApp(
    initialDeepLink: AppDeepLink? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    val authStatus by ServiceLocator.authRepository.status.collectAsStateWithLifecycle()
    val darkTheme = remember { false }
    var chatTarget by rememberSaveable(stateSaver = ChatTarget.Saver) {
        mutableStateOf(
            when (initialDeepLink) {
                is AppDeepLink.Room -> ChatTarget.ActiveRoom(initialDeepLink.roomId)
                is AppDeepLink.Chat -> ChatTarget.ActiveChat(initialDeepLink.nodeId)
                null -> ChatTarget.Closed
            },
        )
    }
    var fromMatchNavigation by rememberSaveable {
        mutableStateOf((initialDeepLink as? AppDeepLink.Chat)?.fromMatch == true)
    }
    var hasSeenOnboarding by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialDeepLink) {
        when (initialDeepLink) {
            is AppDeepLink.Room -> {
                chatTarget = ChatTarget.ActiveRoom(initialDeepLink.roomId)
                onDeepLinkHandled()
            }
            is AppDeepLink.Chat -> {
                chatTarget = ChatTarget.ActiveChat(initialDeepLink.nodeId)
                fromMatchNavigation = initialDeepLink.fromMatch
                onDeepLinkHandled()
            }
            null -> Unit
        }
    }

    AirVibeTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (authStatus) {
                AuthStatus.Loading -> SplashScreen()
                AuthStatus.SignedOut -> {
                    if (hasSeenOnboarding) {
                        AuthScreen()
                    } else {
                        OnboardingScreen(
                            onSkip = { hasSeenOnboarding = true },
                            onNext = { hasSeenOnboarding = true }
                        )
                    }
                }
                AuthStatus.SignedIn -> when (val target = chatTarget) {
                    ChatTarget.Closed -> com.example.airvibe.feature.main.presentation.MainScreen(
                        radarContent = { onMenuClick ->
                            com.example.airvibe.feature.radar.presentation.RadarChatsScreen(
                                onOpenChat = { nodeId -> chatTarget = ChatTarget.ActiveChat(nodeId) },
                                onMenuClick = onMenuClick
                            )
                        },
                        servicesContent = {
                            com.example.airvibe.feature.services.presentation.ServicesScreen()
                        },
                        groupsContent = {
                            com.example.airvibe.feature.groups.presentation.GroupsScreen(
                                onOpenRoom = { roomId -> chatTarget = ChatTarget.ActiveRoom(roomId) }
                            )
                        },
                        profileContent = {
                            com.example.airvibe.feature.profile.presentation.ProfileScreen()
                        }
                    )
                    ChatTarget.List -> ConversationsListScreen(
                        onBack = { chatTarget = ChatTarget.Closed },
                        onOpenConversation = { nodeId ->
                            chatTarget = ChatTarget.ActiveChat(nodeId)
                        },
                    )
                    ChatTarget.Friends -> FriendsScreen(
                        onBack = { chatTarget = ChatTarget.Closed },
                        onOpenChat = { nodeId -> chatTarget = ChatTarget.ActiveChat(nodeId) },
                    )
                    ChatTarget.RoomsList -> RoomsListScreen(
                        onBack = { chatTarget = ChatTarget.Closed },
                        onOpenRoom = { roomId -> chatTarget = ChatTarget.ActiveRoom(roomId) },
                    )
                    is ChatTarget.ActiveChat -> ChatScreen(
                        peerNodeId = target.peerNodeId,
                        onBack = { chatTarget = ChatTarget.Closed },
                    )
                    is ChatTarget.ActiveRoom -> GroupRoomScreen(
                        roomId = target.roomId,
                        onBack = { chatTarget = ChatTarget.Closed },
                    )
                }
            }
        }
    }
}

sealed interface ChatTarget {
    data object Closed : ChatTarget
    data object List : ChatTarget
    data object Friends : ChatTarget
    data object RoomsList : ChatTarget
    data class ActiveChat(val peerNodeId: String) : ChatTarget
    data class ActiveRoom(val roomId: String) : ChatTarget

    companion object {
        val Saver: androidx.compose.runtime.saveable.Saver<ChatTarget, String> =
            androidx.compose.runtime.saveable.Saver(
                save = {
                    when (it) {
                        Closed -> "closed"
                        List -> "list"
                        Friends -> "friends"
                        RoomsList -> "rooms"
                        is ActiveChat -> "chat:${it.peerNodeId}"
                        is ActiveRoom -> "room:${it.roomId}"
                    }
                },
                restore = { value ->
                    when {
                        value == "closed" -> Closed
                        value == "list" -> List
                        value == "friends" -> Friends
                        value == "rooms" -> RoomsList
                        value.startsWith("chat:") ->
                            ActiveChat(value.removePrefix("chat:"))
                        value.startsWith("room:") ->
                            ActiveRoom(value.removePrefix("room:"))
                        else -> Closed
                    }
                },
            )
    }
}


