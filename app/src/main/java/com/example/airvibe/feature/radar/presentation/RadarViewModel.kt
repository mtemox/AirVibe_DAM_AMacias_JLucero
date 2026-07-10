package com.example.airvibe.feature.radar.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.airvibe.core.di.ScannerLifecycle
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.chat.domain.repository.ChatRepository
import com.example.airvibe.feature.chat.domain.repository.MatchPreferencesRepository
import com.example.airvibe.feature.radar.domain.repository.RadarRepository
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

/**
 * ViewModel de la pantalla del radar. Sigue el patrón MVVM con
 * exposición de un único [StateFlow] inmutable.
 *
 * Paso 5: además del radar, observa los criterios de matching
 * (para alimentar el sheet de filtros) y el conteo de chats
 * (para mostrar un badge en el botón de "Chats" de la top bar).
 */
class RadarViewModel(
    private val repository: RadarRepository,
    private val scanner: RadarScanner,
    private val scannerLifecycle: ScannerLifecycle,
    private val appContext: Context,
    private val profileProvider: () -> ScannerProfile,
    private val matchPreferences: MatchPreferencesRepository,
    private val chatRepository: ChatRepository,
    private val onSignOut: suspend () -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadarUiState())
    val uiState: StateFlow<RadarUiState> = _uiState.asStateFlow()

    init {
        observeNodes()
        observeScannerState()
        observeMatchPreferences()
        observeUnsyncedCount()
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

    private fun observeScannerState() {
        viewModelScope.launch {
            scanner.state
                .onEach { scannerState ->
                    val discovered = (scannerState as? ScannerState.Active)?.discovered ?: 0
                    _uiState.update {
                        it.copy(
                            scannerState = scannerState,
                            isScanning = scannerState is ScannerState.Active ||
                                scannerState is ScannerState.Starting,
                            discoveredPeers = discovered,
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

    private fun observeUnsyncedCount() {
        viewModelScope.launch {
            chatRepository.observeConversations()
                .onEach { list ->
                    _uiState.update { it.copy(unreadChatCount = list.size) }
                }
                .collect()
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
            RadarUiEvent.DismissPermissions -> dismissPermissions()
            RadarUiEvent.SignOut -> signOut()
            is RadarUiEvent.UpdateOwnProfile -> updateOwnProfile(event.profile)
            RadarUiEvent.OpenChats -> Unit
            RadarUiEvent.OpenMatchFilters -> _uiState.update { it.copy(isMatchFiltersVisible = true) }
            RadarUiEvent.DismissMatchFilters -> _uiState.update { it.copy(isMatchFiltersVisible = false) }
        }
    }

    private fun showPreview(nodeId: String) {
        val node = _uiState.value.nodes.firstOrNull { it.id == nodeId } ?: return
        viewModelScope.launch {
            val profile = repository.getProfile(nodeId)
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
            _uiState.update { it.copy(pendingPermissionRequest = false) }
            scannerLifecycle.execute(ScannerLifecycle.Action.Start)
            val started = scanner.start(profileProvider())
            if (!started) {
                _uiState.update { it.copy(pendingPermissionRequest = true) }
            }
        }
    }

    private fun stopScanning() {
        viewModelScope.launch {
            scanner.stop()
            scannerLifecycle.execute(ScannerLifecycle.Action.Stop)
        }
    }

    private fun dismissPermissions() {
        _uiState.update { it.copy(pendingPermissionRequest = false) }
    }

    private fun refresh() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun connect() {
        // Hook reservado para el paso 4 (establecer conexión P2P activa).
    }

    private fun addToContacts() {
        viewModelScope.launch {
            val nodeId = _uiState.value.selectedNode?.id ?: return@launch
            repository.toggleFavorite(nodeId)
        }
    }

    private fun updateOwnProfile(profile: ScannerProfile) {
        viewModelScope.launch { scanner.updateProfile(profile) }
    }

    private fun signOut() {
        viewModelScope.launch {
            // Detenemos el radar antes de cerrar sesión para no
            // seguir publicando presencia.
            scanner.stop()
            scannerLifecycle.execute(ScannerLifecycle.Action.Stop)
            onSignOut()
        }
    }

    /**
     * Factory parametrizable. El [appContext] debe ser el contexto
     * de la aplicación (evita leaks de Activity).
     */
    class Factory(
        private val appContext: Context,
        private val repository: RadarRepository = ServiceLocator.radarRepository,
        private val scanner: RadarScanner = ServiceLocator.radarScanner,
        private val scannerLifecycle: ScannerLifecycle = ServiceLocator.scannerLifecycle,
        private val profileProvider: () -> ScannerProfile = ServiceLocator.scannerProfileProvider,
        private val matchPreferences: MatchPreferencesRepository = ServiceLocator.matchPreferencesRepository,
        private val chatRepository: ChatRepository = ServiceLocator.chatRepository,
        private val onSignOut: suspend () -> Unit = { ServiceLocator.authRepository.signOut() },
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
                profileProvider = profileProvider,
                matchPreferences = matchPreferences,
                chatRepository = chatRepository,
                onSignOut = onSignOut,
            ) as T
        }
    }
}
