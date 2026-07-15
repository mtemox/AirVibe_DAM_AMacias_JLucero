package com.example.airvibe.feature.radar.presentation

import android.app.Application
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.example.airvibe.feature.radar.presentation.components.RadarNodeBubble
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.airvibe.core.permissions.rememberRadarPermissionsState
import com.example.airvibe.feature.radar.presentation.components.permissions.PermissionsScreen
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.core.designsystem.modifiers.glassShadow
import com.example.airvibe.core.designsystem.modifiers.glassBlur
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
    val isGroup: Boolean = false,
    val avatarBase64: String? = null
)

private object RadarDefaults {
    val CollapsedHeight = 300.dp
    val MinHeight = 120.dp
    val MaxHeight = 640.dp
    val ExpandedHeight = 2000.dp
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RadarChatsScreen(
    onOpenChat: (String) -> Unit = {},
    onMenuClick: () -> Unit = {},
    viewModel: ConversationsListViewModel = radarChatsViewModel(),
    radarViewModel: com.example.airvibe.feature.radar.presentation.RadarViewModel = radarViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val radarState by radarViewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val chats = remember(state.conversations) {
        state.conversations.map { it.toMockChat() }
    }

    var radarHeight by remember { mutableStateOf(RadarDefaults.CollapsedHeight) }
    var isRadarExpanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val permissionsState = rememberRadarPermissionsState()
    var showPermissionsModal by remember { mutableStateOf(false) }

    var chatToDelete by remember { mutableStateOf<MockChat?>(null) }

    val animatedRadarHeight by animateDpAsState(
        targetValue = if (isRadarExpanded) RadarDefaults.ExpandedHeight else radarHeight,
        animationSpec = spring(),
        label = "radarHeight",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                Icon(imageVector = Icons.Rounded.Menu, contentDescription = "Menú")
            }
            Text(
                text = "AirVibe",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            )
            // Profile Image
            val profile by com.example.airvibe.core.di.ServiceLocator.scannerProfileRepository.observe()
                .collectAsStateWithLifecycle(initialValue = null)

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                if (profile?.avatarBase64 != null) {
                    com.example.airvibe.core.designsystem.components.AvatarMonogram(
                        name = profile?.displayName ?: "?",
                        size = 40.dp,
                        imageModel = profile?.avatarBase64
                    )
                } else if (profile != null) {
                    com.example.airvibe.core.designsystem.components.AvatarMonogram(
                        name = profile?.displayName ?: "?",
                        size = 40.dp
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_background), // Fallback image
                        contentDescription = "Perfil",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Radar Area (Mock)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedRadarHeight)
                .then(if (!isRadarExpanded) Modifier.clickable { isRadarExpanded = true } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            // Map Background with Grid
            RadarMapBackground()

            // Concentric circles
            Box(modifier = Modifier.size(240.dp).border(1.dp, Color.LightGray.copy(alpha=0.3f), CircleShape))
            Box(modifier = Modifier.size(160.dp).border(1.dp, Color.LightGray.copy(alpha=0.5f), CircleShape))
            
            // Center Pulse
            val pulseAlpha = if (radarState.isScanning) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                alpha
            } else 1f
            val centerColor = if (radarState.isScanning) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.primaryContainer

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(centerColor.copy(alpha = pulseAlpha), CircleShape)
                    .then(if (isRadarExpanded) Modifier.clickable { 
                        if (!permissionsState.allGranted) {
                            showPermissionsModal = true
                        } else {
                            radarViewModel.onEvent(com.example.airvibe.feature.radar.presentation.RadarUiEvent.ToggleScan)
                        }
                    } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (isRadarExpanded) {
                    Icon(imageVector = Icons.Rounded.Search, contentDescription = "Escanear", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
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
                    Icon(imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = "Reducir radar")
                }
            }

            // Real Radar Nodes
            radarState.displayNodes.forEach { node ->
                RadarNodeBubble(
                    node = node,
                    onClick = { radarViewModel.onEvent(com.example.airvibe.feature.radar.presentation.RadarUiEvent.NodeClicked(node.id)) },
                    isSelected = radarState.selectedNode?.id == node.id,
                    selectedProfile = if (radarState.selectedNode?.id == node.id) radarState.selectedProfile else null,
                    onConnect = {
                        radarViewModel.onEvent(com.example.airvibe.feature.radar.presentation.RadarUiEvent.DismissPreview)
                        onOpenChat(node.id)
                    },
                    onDismissPreview = { radarViewModel.onEvent(com.example.airvibe.feature.radar.presentation.RadarUiEvent.DismissPreview) }
                )
            }

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
                    val peerColor = if (radarState.proximityCount > 0) androidx.compose.ui.graphics.Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color(0xFFFFA84D)
                    Box(modifier = Modifier.size(8.dp).background(peerColor, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${radarState.proximityCount} peers cercanos", style = MaterialTheme.typography.labelMedium)
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
                            .combinedClickable(
                                onClick = { onOpenChat(chat.nodeId) },
                                onLongClick = { chatToDelete = chat }
                            )
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
                                Icon(imageVector = Icons.Rounded.Person, contentDescription = "Grupo", tint = MaterialTheme.colorScheme.primary)
                            } else if (!chat.avatarBase64.isNullOrEmpty()) {
                                com.example.airvibe.core.designsystem.components.AvatarMonogram(
                                    name = chat.name,
                                    size = 56.dp,
                                    imageModel = chat.avatarBase64
                                )
                            } else {
                                com.example.airvibe.core.designsystem.components.AvatarMonogram(
                                    name = chat.name,
                                    size = 56.dp
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
                                            color = MaterialTheme.colorScheme.onPrimary,
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
                        Icon(imageVector = Icons.Rounded.Lock, contentDescription = "Cifrado", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Cifrado de extremo a extremo en local", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        if (showPermissionsModal) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showPermissionsModal = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                PermissionsScreen(
                    state = permissionsState,
                    onMenuClick = onMenuClick,
                    onDismiss = { showPermissionsModal = false }
                )
            }
        }
        
        if (chatToDelete != null) {
            AlertDialog(
                onDismissRequest = { chatToDelete = null },
                title = { Text("Eliminar Chat") },
                text = { Text("¿Estás seguro de que deseas eliminar el chat con ${chatToDelete?.name}? Esto borrará todo el historial.") },
                confirmButton = {
                    TextButton(onClick = {
                        chatToDelete?.let { viewModel.deleteConversation(it.nodeId) }
                        chatToDelete = null
                    }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { chatToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
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
    avatarBase64 = avatarBase64,
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
        sameYesterday -> "Ayer"
        sameWeek -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(this))
        sameYear -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(this))
        else -> SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date(this))
    }
}

@Composable
private fun radarChatsViewModel(): ConversationsListViewModel {
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as Application
    val factory = androidx.compose.runtime.remember {
        ConversationsListViewModel.Factory(appContext = application)
    }
    return androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
}

@Composable
private fun radarViewModel(): com.example.airvibe.feature.radar.presentation.RadarViewModel {
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as Application
    val factory = androidx.compose.runtime.remember(application) {
        com.example.airvibe.feature.radar.presentation.RadarViewModel.Factory(appContext = application)
    }
    return androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
}

@Composable
fun RadarMapBackground(modifier: Modifier = Modifier.fillMaxSize()) {
    val surfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
    val gridColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Base surface
        drawRect(surfaceColor)
        
        // 1. Grid
        val gridSize = 60.dp.toPx()
        
        // Horizontal lines
        var y = 0f
        while (y < height) {
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(width, y),
                strokeWidth = 3.dp.toPx() // Thicker, noticeable line
            )
            y += gridSize
        }
        
        // Vertical lines
        var x = 0f
        while (x < width) {
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, height),
                strokeWidth = 3.dp.toPx()
            )
            x += gridSize
        }

        // 2. Elegant Vignette (Fades perfectly to the app's surface color at the edges)
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(Color.Transparent, surfaceColor.copy(alpha = 0.7f), surfaceColor),
                center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f),
                radius = minOf(width, height) / 1.3f
            )
        )
    }
}


