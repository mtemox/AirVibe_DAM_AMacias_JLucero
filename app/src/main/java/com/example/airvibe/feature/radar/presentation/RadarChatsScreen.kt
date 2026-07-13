package com.example.airvibe.feature.radar.presentation

import android.app.Application
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airvibe.R
import com.example.airvibe.feature.chat.domain.repository.ConversationSummary
import com.example.airvibe.feature.chat.presentation.ConversationsListViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MockChat(
    val nodeId: String,
    val name: String,
    val message: String,
    val time: String,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false
)

private object RadarDefaults {
    val CollapsedHeight = 300.dp
    val MinHeight = 120.dp
    val MaxHeight = 640.dp
    val ExpandedHeight = 2000.dp
}

@Composable
fun RadarChatsScreen(
    onOpenChat: (String) -> Unit,
    onMenuClick: () -> Unit = {},
    viewModel: ConversationsListViewModel = radarChatsViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chats = remember(state.conversations) {
        state.conversations.map { it.toMockChat() }
    }

    var radarHeight by remember { mutableStateOf(RadarDefaults.CollapsedHeight) }
    var isRadarExpanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val animatedRadarHeight by animateDpAsState(
        targetValue = if (isRadarExpanded) RadarDefaults.ExpandedHeight else radarHeight,
        animationSpec = spring(),
        label = "radarHeight",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top App Bar Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(imageVector = Icons.Rounded.Menu, contentDescription = "Menu")
            }
            Text(
                text = "AirVibe",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            )
            // Profile Image Mock
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_background), // Fallback image
                contentDescription = "Profile",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )
        }
        // Radar Area (Mock)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedRadarHeight)
                .then(if (!isRadarExpanded) Modifier.clickable { isRadarExpanded = true } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            // Concentric circles
            Box(modifier = Modifier.size(240.dp).border(1.dp, Color.LightGray.copy(alpha=0.3f), CircleShape))
            Box(modifier = Modifier.size(160.dp).border(1.dp, Color.LightGray.copy(alpha=0.5f), CircleShape))
            
            // Center Pulse
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isRadarExpanded) {
                    Icon(imageVector = Icons.Rounded.Search, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }

            if (isRadarExpanded) {
                IconButton(
                    onClick = { isRadarExpanded = false },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = "Shrink Radar")
                }
            }

            // Mock Bubble 1
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 60.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .border(2.dp, Color(0xFFFFA84D), CircleShape)
            )

            // Mock Bubble 2
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.BottomStart)
                    .padding(bottom = 60.dp, start = 80.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            )

            // Peers nearby indicator
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFA84D), CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "4 Peers Nearby", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Draggable Handle (oculto cuando el radar está expandido)
        if (!isRadarExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            val delta = with(density) { dragAmount.toDp() }
                            radarHeight = (radarHeight + delta)
                                .coerceIn(RadarDefaults.MinHeight, RadarDefaults.MaxHeight)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(width = 32.dp, height = 4.dp).background(Color.LightGray, CircleShape))
            }

            // Chats List
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(items = chats, key = { it.nodeId }) { chat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChat(chat.nodeId) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (chat.isGroup) {
                                Icon(imageVector = Icons.Rounded.Person, contentDescription = "Group", tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_background),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Texts
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chat.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = chat.time,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chat.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (chat.unreadCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = chat.unreadCount.toString(),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Rounded.Lock, contentDescription = "Encrypted", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "End-to-end encrypted locally", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private fun ConversationSummary.toMockChat(): MockChat = MockChat(
    nodeId = nodeId,
    name = displayName,
    message = lastMessage,
    time = lastTimestamp.toChatTimestamp(),
    unreadCount = unreadCount,
    isGroup = isGroupInvite,
)

private fun Long.toChatTimestamp(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    if (diff < 0L) return ""

    val nowCal = Calendar.getInstance().apply { timeInMillis = now }
    val thenCal = Calendar.getInstance().apply { timeInMillis = this@toChatTimestamp }

    val sameDay = nowCal.get(Calendar.YEAR) == thenCal.get(Calendar.YEAR) &&
        nowCal.get(Calendar.DAY_OF_YEAR) == thenCal.get(Calendar.DAY_OF_YEAR)
    val yesterday = nowCal.apply { add(Calendar.DAY_OF_YEAR, -1) }
    val sameYesterday = yesterday.get(Calendar.YEAR) == thenCal.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == thenCal.get(Calendar.DAY_OF_YEAR)
    val sameWeek = nowCal.get(Calendar.YEAR) == thenCal.get(Calendar.YEAR) &&
        nowCal.get(Calendar.WEEK_OF_YEAR) == thenCal.get(Calendar.WEEK_OF_YEAR)
    val sameYear = nowCal.get(Calendar.YEAR) == thenCal.get(Calendar.YEAR)

    return when {
        sameDay -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(this))
        sameYesterday -> "Yesterday"
        sameWeek -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(this))
        sameYear -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(this))
        else -> SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date(this))
    }
}

@Composable
private fun radarChatsViewModel(): ConversationsListViewModel {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val factory = remember {
        ConversationsListViewModel.Factory(appContext = application)
    }
    return viewModel(factory = factory)
}

