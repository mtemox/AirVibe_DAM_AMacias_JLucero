package com.example.airvibe.feature.radar.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.airvibe.core.di.ScannerLifecycle
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.auth.domain.repository.AuthRepository
import com.example.airvibe.feature.chat.domain.repository.ChatRepository
import com.example.airvibe.feature.chat.domain.scanner.ChatMessageGateway
import com.example.airvibe.feature.chat.domain.repository.MatchPreferencesRepository
import com.example.airvibe.feature.radar.data.seed.RadarSeedData
import com.example.airvibe.feature.radar.domain.model.PersonProfile
import com.example.airvibe.feature.radar.presentation.components.proximityMeters
import com.example.airvibe.feature.radar.domain.repository.RadarRepository
import com.example.airvibe.feature.radar.domain.repository.ScannerProfileRepository
import com.example.airvibe.feature.radar.domain.scanner.RadarScanner
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile
import com.example.airvibe.feature.radar.domain.scanner.ScannerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RadarViewModel(
    private val repository: RadarRepository,
    private val scanner: RadarScanner,
    private val scannerLifecycle: ScannerLifecycle,
    private val appContext: Context,
    private val profileRepository: ScannerProfileRepository,
    private val authRepository: AuthRepository,
    private val matchPreferences: MatchPreferencesRepository,
    private val chatRepository: ChatRepository,
    private val chatGateway: ChatMessageGateway = ServiceLocator.chatGateway,
    private val avatarRemoteDataSource: com.example.airvibe.feature.radar.data.remote.SupabaseAvatarDataSource = ServiceLocator.avatarRemoteDataSource,
    private val onSignOut: suspend () -> Result<Unit> = { ServiceLocator.authRepository.signOut() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadarUiState())
    val uiState: StateFlow<RadarUiState> = _uiState.asStateFlow()

    init {
        observeNodes()
        observeLiveNodes()
        observeScannerState()
        observeMatchPreferences()
        observeUnreadChats()
        observeOwnProfile()
        observeScannerService()
        observeIncomingHandshakes()
        syncAuthDisplayName()
    }

    private fun observeNodes() {
        viewModelScope.launch {
            repository.observeRadarNodes()
                .onEach { nodes ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            nodes = nodes,
                        )
                    }
                }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Error desconocido",
                        )
                    }
                }
                .collect()
        }
    }

    private fun observeLiveNodes() {
        viewModelScope.launch {
            scanner.liveNodes
                .onEach { live ->
                    _uiState.update { it.copy(liveNodes = live) }
                }
                .collect()
        }
        // Feature 5: cada vez que cambia el set de nodos en
        // vivo, registramos un evento de "View" por cada peer
        // (no-self, no-seed, no-pending). El SyncWorker se
        // encargará de empujar el buffer a Supabase.
        viewModelScope.launch {
            var lastSeen: Set<String> = emptySet()
            scanner.liveNodes
                .onEach { live ->
                    val validNodes = live
                        .filter { !it.id.startsWith("pending-") && !it.id.startsWith(RadarSeedData.SEED_ID_PREFIX) }
                    val current = validNodes.map { it.id }.toSet()
                    val localNodeId = profileRepository.current().id
                    val newOnes = current - lastSeen
                    newOnes.forEach { nodeId ->
                        if (nodeId != localNodeId) {
                            // Solo registrar telemetría para peers Premium con authUserId válido
                            val node = validNodes.find { it.id == nodeId }
                            if (node != null && node.isPremium && !node.authUserId.isNullOrBlank()) {
                                repository.recordProfileEvent(
                                    targetUserId = node.authUserId,
                                    sourceNodeId = localNodeId,
                                    kind = com.example.airvibe.feature.radar.data.local.entity.ProfileViewEntity.KIND_VIEW,
                                )
                            }
                        }
                    }
                    lastSeen = current
                }
                .collect()
        }
    }

    private fun observeScannerState() {
        viewModelScope.launch {
            scanner.state
                .onEach { scannerState ->
                    val discovered = (scannerState as? ScannerState.Active)?.discovered ?: 0
                    val isScanning = scannerState is ScannerState.Active ||
                        scannerState is ScannerState.Starting
                    val needsPermissions = scannerState is ScannerState.Error &&
                        scannerState.reason == com.example.airvibe.feature.radar.domain.scanner.ScannerError.MissingPermissions
                    val scanErrorMessage = (scannerState as? ScannerState.Error)?.let { error ->
                        when (error.reason) {
                            com.example.airvibe.feature.radar.domain.scanner.ScannerError.MissingPermissions ->
                                "Faltan permisos de Bluetooth o Wi-Fi para escanear."
                            com.example.airvibe.feature.radar.domain.scanner.ScannerError.BluetoothUnavailable ->
                                "Activa el Bluetooth del dispositivo."
                            com.example.airvibe.feature.radar.domain.scanner.ScannerError.LocationUnavailable ->
                                "Activa la ubicación para mejorar el escaneo."
                            is com.example.airvibe.feature.radar.domain.scanner.ScannerError.Unknown ->
                                error.reason.message.ifBlank { "No se pudo iniciar el escaneo." }
                        }
                    }
                    _uiState.update {
                        it.copy(
                            scannerState = scannerState,
                            isScanning = isScanning,
                            discoveredPeers = discovered,
                            hideDemoNodes = isScanning || discovered > 0,
                            pendingPermissionRequest = when {
                                isScanning -> false
                                needsPermissions -> true
                                else -> it.pendingPermissionRequest
                            },
                            errorMessage = scanErrorMessage ?: if (isScanning) null else it.errorMessage,
                        )
                    }
                }
                .collect()
        }
    }

    private fun observeScannerService() {
        viewModelScope.launch {
            com.example.airvibe.feature.radar.data.device.service.ScannerServiceState.isRunning
                .onEach { running ->
                    _uiState.update { it.copy(scannerServiceRunning = running) }
                }
                .collect()
        }
    }

    private fun observeIncomingHandshakes() {
        viewModelScope.launch {
            repository.observeIncomingHandshakes()
                .onEach { requests ->
                    val current = _uiState.value
                    val nextActive = current.activeHandshake
                        ?.takeIf { active -> requests.any { it.handshakeId == active.handshakeId } }
                        ?: requests.firstOrNull()
                    _uiState.update {
                        it.copy(
                            incomingHandshakes = requests,
                            activeHandshake = nextActive,
                        )
                    }
                }
                .collect()
        }
    }

    private fun observeMatchPreferences() {
        viewModelScope.launch {
            matchPreferences.observe()
                .onEach { criteria -> _uiState.update { it.copy(matchCriteria = criteria) } }
                .collect()
        }
    }

    private fun observeUnreadChats() {
        viewModelScope.launch {
            chatRepository.observeUnreadConversationCount()
                .onEach { count ->
                    _uiState.update { it.copy(unreadChatCount = count) }
                }
                .collect()
        }
    }

    private fun observeOwnProfile() {
        viewModelScope.launch {
            profileRepository.observe()
                .onEach { profile ->
                    _uiState.update { it.copy(ownProfile = profile) }
                }
                .collect()
        }
    }

    private fun syncAuthDisplayName() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                profileRepository.applyAuthDisplayName(user?.displayName)
            }
        }
    }

    fun onEvent(event: RadarUiEvent) {
        when (event) {
            is RadarUiEvent.NodeClicked -> showPreview(event.nodeId)
            RadarUiEvent.DismissPreview -> dismissPreview()
            RadarUiEvent.ToggleScan -> toggleScan()
            RadarUiEvent.StartScanning -> startScanning()
            RadarUiEvent.StopScanning -> stopScanning()
            RadarUiEvent.Refresh -> refresh()
            RadarUiEvent.Connect -> connect()
            RadarUiEvent.AddToContacts -> addToContacts()
            RadarUiEvent.ToggleFavorite -> toggleFavorite()
            RadarUiEvent.DismissPermissions -> dismissPermissions()
            RadarUiEvent.RequestPermissions -> _uiState.update { it.copy(pendingPermissionRequest = true) }
            RadarUiEvent.SignOut -> signOut()
            is RadarUiEvent.UpdateOwnProfile -> updateOwnProfile(event.profile)
            RadarUiEvent.OpenChats -> Unit
            RadarUiEvent.OpenFriends -> Unit
            RadarUiEvent.OpenMatchFilters -> _uiState.update { it.copy(isMatchFiltersVisible = true) }
            RadarUiEvent.DismissMatchFilters -> _uiState.update { it.copy(isMatchFiltersVisible = false) }
            RadarUiEvent.OpenOwnProfile -> _uiState.update { it.copy(isOwnProfileVisible = true) }
            RadarUiEvent.DismissOwnProfile -> _uiState.update { it.copy(isOwnProfileVisible = false) }
            RadarUiEvent.OpenBroadcast -> _uiState.update { it.copy(isBroadcastVisible = true) }
            RadarUiEvent.DismissBroadcast -> _uiState.update {
                it.copy(isBroadcastVisible = false, lastBroadcastCount = 0, lastBroadcastRoomId = null)
            }
            is RadarUiEvent.SendBroadcast -> sendBroadcast(event.text)
            RadarUiEvent.ConsumeBroadcastRoomNav -> _uiState.update {
                it.copy(lastBroadcastRoomId = null, isBroadcastVisible = false)
            }
            RadarUiEvent.ConsumeContactAddedMessage -> _uiState.update {
                it.copy(contactAddedMessage = null)
            }
            is RadarUiEvent.SendHandshakeRequest -> sendHandshakeRequest(event.nodeId)
            RadarUiEvent.ConsumeHandshakeSentMessage -> _uiState.update {
                it.copy(handshakeSentMessage = null)
            }
            is RadarUiEvent.OpenHandshakeRequest -> openHandshake(event.handshakeId)
            RadarUiEvent.DismissHandshakeRequest -> _uiState.update {
                it.copy(isHandshakeSheetVisible = false)
            }
            is RadarUiEvent.AcceptHandshakeRequest -> respondToHandshake(event.handshakeId, accept = true)
            is RadarUiEvent.RejectHandshakeRequest -> respondToHandshake(event.handshakeId, accept = false)
        }
    }

    private fun showPreview(nodeId: String) {
        if (nodeId.startsWith("pending-")) return
        if (nodeId.startsWith(RadarSeedData.SEED_ID_PREFIX) && _uiState.value.hideDemoNodes) return
        val node = _uiState.value.displayNodes.firstOrNull { it.id == nodeId }
            ?: _uiState.value.nodes.firstOrNull { it.id == nodeId }
            ?: return
        // Feature 5: registrar un "Tap" para el dashboard de
        // visibilidad del peer. Solo para Premium con authUserId.
        viewModelScope.launch {
            val localNodeId = profileRepository.current().id
            if (nodeId != localNodeId && node.isPremium && !node.authUserId.isNullOrBlank()) {
                repository.recordProfileEvent(
                    targetUserId = node.authUserId,
                    sourceNodeId = localNodeId,
                    kind = com.example.airvibe.feature.radar.data.local.entity.ProfileViewEntity.KIND_TAP,
                )
            }
        }
        viewModelScope.launch {
            val profile = repository.getProfile(nodeId)
                ?: PersonProfile.fromNode(node, distanceMeters = proximityMeters(node.distanceNormalized))
            _uiState.update {
                it.copy(
                    selectedNode = node,
                    selectedProfile = profile,
                    isSheetVisible = true,
                )
            }
        }
    }

    private fun dismissPreview() {
        _uiState.update {
            it.copy(
                isSheetVisible = false,
                selectedNode = null,
                selectedProfile = null,
            )
        }
    }

    private fun toggleScan() {
        if (_uiState.value.isScanning) {
            stopScanning()
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        viewModelScope.launch {
            _uiState.update { it.copy(hasAutoStarted = true) }
            runCatching {
                scannerLifecycle.execute(ScannerLifecycle.Action.Start)
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        pendingPermissionRequest = true,
                        errorMessage = "Activa los permisos del radar para escanear.",
                    )
                }
            }
        }
    }

    private fun stopScanning() {
        viewModelScope.launch {
            runCatching {
                scannerLifecycle.execute(ScannerLifecycle.Action.Stop)
            }
        }
    }

    private fun dismissPermissions() {
        _uiState.update { it.copy(pendingPermissionRequest = false) }
    }

    private fun refresh() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun connect() {
        // La navegación al chat la resuelve RadarScreen vía onOpenChat.
    }

    private fun addToContacts() {
        viewModelScope.launch {
            val nodeId = _uiState.value.selectedNode?.id ?: return@launch
            val added = repository.addFavorite(nodeId)
            if (added) {
                chatGateway.sendFriendAdd(nodeId)
                ServiceLocator.requestContactsSync()
                val profile = repository.getProfile(nodeId)
                _uiState.update {
                    it.copy(
                        selectedProfile = profile ?: it.selectedProfile,
                        contactAddedMessage = "${it.selectedNode?.displayName ?: "Contacto"} agregado a amigos",
                    )
                }
            }
        }
    }

    private fun toggleFavorite() {
        viewModelScope.launch {
            val nodeId = _uiState.value.selectedNode?.id ?: return@launch
            repository.toggleFavorite(nodeId)
            ServiceLocator.requestContactsSync()
            val profile = repository.getProfile(nodeId)
            _uiState.update { it.copy(selectedProfile = profile ?: it.selectedProfile) }
        }
    }

    private fun updateOwnProfile(profile: ScannerProfile) {
        viewModelScope.launch { scanner.updateProfile(profile) }
    }

    private fun saveOwnProfile(
        displayName: String,
        status: String,
        tags: List<String>,
        kind: com.example.airvibe.feature.radar.domain.model.RadarNodeKind,
        presence: com.example.airvibe.feature.radar.domain.model.PresenceStatus,
        headline: String,
        bio: String,
        isPremium: Boolean,
        premiumCatalog: String?,
        avatarUri: android.net.Uri?,
    ) {
        viewModelScope.launch {
            if (avatarUri != null) {
                val userId = profileRepository.current().id
                val base64 = com.example.airvibe.core.util.ImageCompressor.compressToBase64(appContext, avatarUri)
                val bytesForUpload = com.example.airvibe.core.util.ImageCompressor.compressForUpload(appContext, avatarUri)
                var avatarUrl: String? = null
                
                if (bytesForUpload != null) {
                    val uploadResult = avatarRemoteDataSource.uploadAvatar(userId, bytesForUpload)
                    if (uploadResult.isSuccess) {
                        avatarUrl = uploadResult.getOrNull()
                    }
                }
                
                profileRepository.updateAvatar(avatarUrl, base64)
            }
            
            profileRepository.update(displayName, status, tags)
            profileRepository.updateKind(kind)
            profileRepository.updatePresence(presence)
            profileRepository.updateHeadline(headline)
            profileRepository.updateBio(bio)
            profileRepository.updatePremium(isPremium, premiumCatalog)
            val updated = profileRepository.current()
            runCatching { scanner.updateProfile(updated) }
            runCatching { profileRepository.syncToRemote(updated) }
            ServiceLocator.requestContactsSync()
            _uiState.update { it.copy(isOwnProfileVisible = false) }
        }
    }

    private fun sendBroadcast(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBroadcasting = true) }
            val result = runCatching { chatRepository.broadcast(text) }
                .getOrElse { com.example.airvibe.feature.chat.domain.repository.BroadcastResult(0, "") }
            // Feature 5: registrar un evento Broadcast. La
            // telemetría se envía al SyncWorker en background.
            val localNodeId = profileRepository.current().id
            if (result.roomId.isNotBlank()) {
                repository.recordProfileEvent(
                    targetUserId = result.roomId,
                    sourceNodeId = localNodeId,
                    kind = com.example.airvibe.feature.radar.data.local.entity.ProfileViewEntity.KIND_BROADCAST,
                )
            }
            _uiState.update {
                it.copy(
                    isBroadcasting = false,
                    lastBroadcastCount = result.recipientCount,
                    lastBroadcastRoomId = result.roomId.takeIf { id -> id.isNotBlank() },
                )
            }
        }
    }

    fun onOwnProfileSave(
        displayName: String,
        status: String,
        tags: List<String>,
        kind: com.example.airvibe.feature.radar.domain.model.RadarNodeKind,
        presence: com.example.airvibe.feature.radar.domain.model.PresenceStatus,
        headline: String,
        bio: String,
        isPremium: Boolean,
        premiumCatalog: String?,
        avatarUri: android.net.Uri?,
    ) {
        saveOwnProfile(displayName, status, tags, kind, presence, headline, bio, isPremium, premiumCatalog, avatarUri)
    }

    private fun signOut() {
        viewModelScope.launch {
            runCatching {
                scannerLifecycle.execute(ScannerLifecycle.Action.Stop)
            }
            runCatching {
                scanner.stop()
            }
            val result = onSignOut()
            result.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "No se pudo cerrar sesión")
                }
            }
        }
    }

    // -------- Feature 3: Handshake --------

    /**
     * Envía una solicitud de handshake al peer correspondiente.
     * Persiste una copia local (dirección `Outgoing`) y dispara
     * un mensaje al peer para que reciba la notificación nativa.
     */
    private fun sendHandshakeRequest(nodeId: String) {
        if (nodeId.isBlank() || nodeId.startsWith("pending-") ||
            nodeId.startsWith(com.example.airvibe.feature.radar.data.seed.RadarSeedData.SEED_ID_PREFIX)
        ) return
        val profile = _uiState.value.ownProfile ?: return
        val handshakeId = java.util.UUID.randomUUID().toString()
        val key = (profile.id + ":" + handshakeId).take(64)
        viewModelScope.launch {
            val direction = com.example.airvibe.feature.radar.domain.model.HandshakeRequest.Direction.Outgoing
            val request = com.example.airvibe.feature.radar.domain.model.HandshakeRequest(
                id = 0L,
                handshakeId = handshakeId,
                peerNodeId = nodeId,
                peerDisplayName = "",
                peerHeadline = "",
                peerStatus = "",
                peerPresence = com.example.airvibe.feature.radar.domain.model.PresenceStatus.Online,
                peerTags = emptyList(),
                handshakeKey = key,
                direction = direction,
                status = com.example.airvibe.feature.radar.domain.model.HandshakeRequest.Status.Pending,
                createdAt = System.currentTimeMillis(),
                respondedAt = null,
            )
            repository.upsertHandshakeRequest(request)
            val sent = runCatching {
                chatGateway.sendHandshakeRequest(
                    targetNodeId = nodeId,
                    handshakeId = handshakeId,
                    key = key,
                )
            }.getOrDefault(false)
            _uiState.update {
                it.copy(
                    handshakeSentMessage = if (sent) {
                        "Solicitud enviada a ${it.nodes.firstOrNull { n -> n.id == nodeId }?.displayName ?: "el peer"}"
                    } else {
                        "No se pudo enviar la solicitud (peer sin conexión)."
                    },
                )
            }
        }
    }

    private fun openHandshake(handshakeId: String) {
        viewModelScope.launch {
            val request = repository.getHandshakeById(handshakeId) ?: return@launch
            _uiState.update {
                it.copy(
                    activeHandshake = request,
                    isHandshakeSheetVisible = true,
                )
            }
        }
    }

    private fun respondToHandshake(handshakeId: String, accept: Boolean) {
        viewModelScope.launch {
            val result = repository.respondToHandshake(handshakeId, accept = accept)
            if (result != null) {
                runCatching {
                    if (accept) {
                        chatGateway.sendHandshakeAccept(
                            targetNodeId = result.peerNodeId,
                            handshakeId = result.handshakeId,
                            key = result.handshakeKey,
                        )
                    } else {
                        chatGateway.sendHandshakeReject(
                            targetNodeId = result.peerNodeId,
                            handshakeId = result.handshakeId,
                        )
                    }
                }
                ServiceLocator.requestContactsSync()
            }
            _uiState.update { state ->
                val remaining = state.incomingHandshakes.filterNot { it.handshakeId == handshakeId }
                state.copy(
                    incomingHandshakes = remaining,
                    activeHandshake = state.activeHandshake?.takeIf { it.handshakeId != handshakeId }
                        ?: remaining.firstOrNull(),
                    isHandshakeSheetVisible = state.activeHandshake?.takeIf { it.handshakeId != handshakeId }
                        ?.let { true } ?: false,
                    contactAddedMessage = if (accept && result != null) {
                        "Conectado con ${result.peerDisplayName}"
                    } else {
                        state.contactAddedMessage
                    },
                )
            }
        }
    }

    class Factory(
        private val appContext: Context,
        private val repository: RadarRepository = ServiceLocator.radarRepository,
        private val scanner: RadarScanner = ServiceLocator.radarScanner,
        private val scannerLifecycle: ScannerLifecycle = ServiceLocator.scannerLifecycle,
        private val profileRepository: ScannerProfileRepository = ServiceLocator.scannerProfileRepository,
        private val authRepository: AuthRepository = ServiceLocator.authRepository,
        private val matchPreferences: MatchPreferencesRepository = ServiceLocator.matchPreferencesRepository,
        private val chatRepository: ChatRepository = ServiceLocator.chatRepository,
        private val avatarRemoteDataSource: com.example.airvibe.feature.radar.data.remote.SupabaseAvatarDataSource = ServiceLocator.avatarRemoteDataSource,
        private val onSignOut: suspend () -> Result<Unit> = { ServiceLocator.authRepository.signOut() },
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RadarViewModel::class.java)) {
                "Unknown ViewModel class: $modelClass"
            }
            return RadarViewModel(
                repository = repository,
                scanner = scanner,
                scannerLifecycle = scannerLifecycle,
                appContext = appContext,
                profileRepository = profileRepository,
                authRepository = authRepository,
                matchPreferences = matchPreferences,
                chatRepository = chatRepository,
                avatarRemoteDataSource = avatarRemoteDataSource,
                onSignOut = onSignOut,
            ) as T
        }
    }
}
