package com.example.airvibe.feature.radar.presentation

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airvibe.core.designsystem.components.AirVibeAmbientBackground
import com.example.airvibe.core.designsystem.modifiers.glassBlur
import com.example.airvibe.core.designsystem.modifiers.glassShadow
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.core.permissions.rememberRadarPermissionsState
import com.example.airvibe.feature.radar.domain.model.RadarNode
import com.example.airvibe.feature.radar.presentation.components.ProfilePreviewContent
import com.example.airvibe.feature.radar.presentation.components.RadarControlPanel
import com.example.airvibe.feature.radar.presentation.components.RadarNodeBubble
import com.example.airvibe.feature.radar.presentation.components.RadarSweep
import com.example.airvibe.feature.radar.presentation.components.RadarTopBar
import com.example.airvibe.feature.radar.presentation.components.permissions.PermissionsModal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    viewModel: RadarViewModel = radarViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val permissionsState = rememberRadarPermissionsState()

    // Si el scanner falla por permisos, abrimos automáticamente el modal.
    val shouldShowPermissions = remember(state.pendingPermissionRequest, permissionsState.allGranted) {
        state.pendingPermissionRequest && !permissionsState.allGranted
    }

    val currentUser by ServiceLocator.authRepository.currentUser.collectAsStateWithLifecycle()
    val displayName = currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.email?.substringBefore('@')
        ?: "Ariel Macias"

    Box(modifier = Modifier.fillMaxSize()) {
        AirVibeAmbientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            RadarTopBar(
                userName = displayName,
                activeCount = state.activeNodeCount,
                isScanning = state.isScanning,
                discoveredPeers = state.discoveredPeers,
                onSignOut = { viewModel.onEvent(RadarUiEvent.SignOut) },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                RadarCanvas(
                    nodes = state.nodes,
                    isScanning = state.isScanning,
                    onNodeClick = { viewModel.onEvent(RadarUiEvent.NodeClicked(it.id)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                RadarControlPanel(
                    onScanToggle = { viewModel.onEvent(RadarUiEvent.ToggleScan) },
                    onBroadcast = { viewModel.onEvent(RadarUiEvent.Refresh) },
                    onCenter = { viewModel.onEvent(RadarUiEvent.Refresh) },
                    onFilter = { viewModel.onEvent(RadarUiEvent.Refresh) },
                    isScanning = state.isScanning,
                )
            }
        }
    }

    PermissionsModal(
        state = permissionsState,
        onDismiss = { viewModel.onEvent(RadarUiEvent.DismissPermissions) },
    )

    val profile = state.selectedProfile
    val node = state.selectedNode
    AnimatedVisibility(
        visible = state.isSheetVisible && profile != null && node != null && !shouldShowPermissions,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        if (profile != null && node != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onEvent(RadarUiEvent.DismissPreview) },
                sheetState = sheetState,
                containerColor = Color.Transparent,
                scrimColor = Color.Black.copy(alpha = 0.45f),
                dragHandle = null,
            ) {
                val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ProfilePreviewContent(
                    profile = profile,
                    nodeKind = node.kind,
                    accentColor = node.accentColor,
                    onConnect = { viewModel.onEvent(RadarUiEvent.Connect) },
                    onAddContact = { viewModel.onEvent(RadarUiEvent.AddToContacts) },
                    onToggleFavorite = { viewModel.onEvent(RadarUiEvent.AddToContacts) },
                    modifier = Modifier
                        .padding(top = 24.dp)
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
}

@Composable
private fun radarViewModel(): RadarViewModel {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val factory = remember(application) {
        RadarViewModel.Factory(appContext = application)
    }
    return viewModel(factory = factory)
}

@Composable
private fun RadarCanvas(
    nodes: List<RadarNode>,
    isScanning: Boolean,
    onNodeClick: (RadarNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sizePx: IntSize = remember { IntSize.Zero }
    val canRenderNodes = sizePx.width > 0 && sizePx.height > 0

    Box(
        modifier = modifier.onSizeChanged { sizePx = it },
    ) {
        RadarSweep(
            modifier = Modifier.fillMaxSize(),
            sweepColor = MaterialTheme.colorScheme.primary,
        )

        if (canRenderNodes) {
            val side = minOf(sizePx.width, sizePx.height).toFloat()
            nodes.forEach { node ->
                RadarNodeBubble(
                    node = node,
                    canvasSizePx = side,
                    onClick = onNodeClick,
                )
            }
        }

        Box(
            modifier = Modifier.align(Alignment.Center),
        ) {
            CenterHub(isScanning = isScanning)
        }
    }
}

@Composable
private fun CenterHub(isScanning: Boolean) {
    Box(
        modifier = Modifier
            .size(86.dp)
            .glassShadow(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                cornerRadius = 43.dp,
            )
            .glassBlur(radius = 18.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Radar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(26.dp),
            )
            Text(
                text = "TÚ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
