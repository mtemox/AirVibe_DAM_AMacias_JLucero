package com.example.airvibe.feature.groups.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airvibe.R
import com.example.airvibe.core.designsystem.modifiers.glassBlur
import com.example.airvibe.core.designsystem.modifiers.glassShadow
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.core.ui.feedback.rememberUserMessages
import com.example.airvibe.feature.chat.domain.model.ProximityRoom
import com.example.airvibe.feature.chat.presentation.RoomsListViewModel
import com.example.airvibe.feature.radar.presentation.components.BroadcastSheet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MockGroup(
    val id: String,
    val name: String,
    val time: String,
    val members: String,
    val icon: ImageVector? = null,
    val iconBgColor: Color = Color.Transparent,
    val iconTintColor: Color = Color.White,
    val hasUnread: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    onOpenRoom: (String) -> Unit = {},
    viewModel: RoomsListViewModel = viewModel(),
) {
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val isCreating by viewModel.isCreating.collectAsStateWithLifecycle()
    val newRoomId by viewModel.newRoomId.collectAsStateWithLifecycle()
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val groups = remember(rooms, primaryColor, primaryContainerColor) {
        rooms.map { it.toMockGroup(primaryColor, primaryContainerColor) }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isCreateSheetVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val (snackbarHostState, snackbarFlow) = rememberUserMessages()

    LaunchedEffect(newRoomId) {
        val id = newRoomId
        if (!id.isNullOrBlank()) {
            viewModel.consumeNewRoomId()
            isCreateSheetVisible = false
            // Pequeño delay para que el snackbar de "Sala creada"
            // sea visible antes de cambiar de pantalla.
            kotlinx.coroutines.delay(450)
            onOpenRoom(id)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isCreateSheetVisible = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Agregar")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // Top App Bar Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { /* TODO */ }) {
                    Icon(imageVector = Icons.Rounded.Menu, contentDescription = "Menú", tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = "AirVibe",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )
                IconButton(onClick = { /* TODO */ }) {
                    Icon(imageVector = Icons.Rounded.Settings, contentDescription = "Ajustes", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Busca grupos o pregunta a Meta AI") },
                    leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.outline) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )
            }

            // Groups List
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = groups, key = { it.id }) { group ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onOpenRoom(group.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar/Icon
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(group.iconBgColor)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (group.icon != null) {
                                    Icon(imageVector = group.icon, contentDescription = null, tint = group.iconTintColor, modifier = Modifier.size(28.dp))
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

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = group.time,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Rounded.Group, contentDescription = "Miembros", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = group.members,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Unread Indicator
                            if (group.hasUnread) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }

        rememberUserMessages(
            flow = snackbarFlow,
            host = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp),
        )
        }
    }

    if (isCreateSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { if (!isCreating) isCreateSheetVisible = false },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            scrimColor = Color.Black.copy(alpha = 0.45f),
            dragHandle = null,
        ) {
            val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            BroadcastSheet(
                isBroadcasting = isCreating,
                lastBroadcastCount = 0,
                onBroadcast = { name -> viewModel.createRoom(name) },
                onDismiss = { if (!isCreating) isCreateSheetVisible = false },
                modifier = Modifier
                    .clip(sheetShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AirVibeTheme.glass.surfaceFillStrong,
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    )
                    .glassShadow(
                        color = AirVibeTheme.glass.shadowColor,
                        cornerRadius = 0.dp,
                    )
                    .glassBlur(radius = 28.dp, shape = sheetShape)
                    .fillMaxWidth(),
            )
        }
    }
}

private fun ProximityRoom.toMockGroup(
    primaryColor: Color,
    primaryContainerColor: Color,
): MockGroup = MockGroup(
    id = id,
    name = title,
    time = createdAt.toGroupTimestamp(),
    members = if (isHost) "Eres el anfitrión" else "Anfitrión: $hostName",
    icon = Icons.Rounded.Group,
    iconBgColor = primaryContainerColor,
    iconTintColor = primaryColor,
    hasUnread = !joined,
)

private fun Long.toGroupTimestamp(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    if (diff < 0L) return ""

    val nowCal = Calendar.getInstance().apply { timeInMillis = now }
    val thenCal = Calendar.getInstance().apply { timeInMillis = this@toGroupTimestamp }

    val sameDay = nowCal.get(Calendar.YEAR) == thenCal.get(Calendar.YEAR) &&
        nowCal.get(Calendar.DAY_OF_YEAR) == thenCal.get(Calendar.DAY_OF_YEAR)
    val yesterday = nowCal.apply { add(Calendar.DAY_OF_YEAR, -1) }
    val sameYesterday = yesterday.get(Calendar.YEAR) == thenCal.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == thenCal.get(Calendar.DAY_OF_YEAR)
    val sameYear = nowCal.get(Calendar.YEAR) == thenCal.get(Calendar.YEAR)

    return when {
        sameDay -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(this))
        sameYesterday -> "Ayer"
        sameYear -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(this))
        else -> SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date(this))
    }
}
