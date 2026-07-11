package com.example.airvibe.feature.radar.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.airvibe.core.di.ScannerLifecycle
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.auth.domain.repository.AuthRepository
import com.example.airvibe.feature.chat.domain.repository.ChatRepository
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
            RadarUiEvent.OpenMatchFilters -> _uiState.update { it.copy(isMatchFiltersVisible = true) }
            RadarUiEvent.DismissMatchFilters -> _uiState.update { it.copy(isMatchFiltersVisible = false) }
            RadarUiEvent.OpenOwnProfile -> _uiState.update { it.copy(isOwnProfileVisible = true) }
            RadarUiEvent.DismissOwnProfile -> _uiState.update { it.copy(isOwnProfileVisible = false) }
            RadarUiEvent.OpenBroadcast -> _uiState.update { it.copy(isBroadcastVisible = true) }
            RadarUiEvent.DismissBroadcast -> _uiState.update {
                it.copy(isBroadcastVisible = false, lastBroadcastCount = 0)
            }
            is RadarUiEvent.SendBroadcast -> sendBroadcast(event.text)
        }
    }

    private fun showPreview(nodeId: String) {
        if (nodeId.startsWith("pending-")) return
        if (nodeId.startsWith(RadarSeedData.SEED_ID_PREFIX) && _uiState.value.hideDemoNodes) return
        val node = _uiState.value.displayNodes.firstOrNull { it.id == nodeId }
            ?: _uiState.value.nodes.firstOrNull { it.id == nodeId }
            ?: return
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
            repository.toggleFavorite(nodeId)
        }
    }

    private fun toggleFavorite() {
        addToContacts()
    }

    private fun updateOwnProfile(profile: ScannerProfile) {
        viewModelScope.launch { scanner.updateProfile(profile) }
    }

    private fun saveOwnProfile(displayName: String, status: String, tags: List<String>) {
        viewModelScope.launch {
            profileRepository.update(displayName, status, tags)
            val updated = profileRepository.current()
            runCatching { scanner.updateProfile(updated) }
            if (_uiState.value.isScanning) {
                runCatching {
                    scannerLifecycle.execute(ScannerLifecycle.Action.Stop)
                    scannerLifecycle.execute(ScannerLifecycle.Action.Start)
                }
            }
            _uiState.update { it.copy(isOwnProfileVisible = false) }
        }
    }

    private fun sendBroadcast(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBroadcasting = true) }
            val count = runCatching { chatRepository.broadcast(text) }.getOrDefault(0)
            _uiState.update {
                it.copy(
                    isBroadcasting = false,
                    lastBroadcastCount = count,
                )
            }
        }
    }

    fun onOwnProfileSave(displayName: String, status: String, tags: List<String>) {
        saveOwnProfile(displayName, status, tags)
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

    class Factory(
        private val appContext: Context,
        private val repository: RadarRepository = ServiceLocator.radarRepository,
        private val scanner: RadarScanner = ServiceLocator.radarScanner,
        private val scannerLifecycle: ScannerLifecycle = ServiceLocator.scannerLifecycle,
        private val profileRepository: ScannerProfileRepository = ServiceLocator.scannerProfileRepository,
        private val authRepository: AuthRepository = ServiceLocator.authRepository,
        private val matchPreferences: MatchPreferencesRepository = ServiceLocator.matchPreferencesRepository,
        private val chatRepository: ChatRepository = ServiceLocator.chatRepository,
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
                onSignOut = onSignOut,
            ) as T
        }
    }
}
