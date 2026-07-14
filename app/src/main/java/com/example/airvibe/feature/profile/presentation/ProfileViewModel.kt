package com.example.airvibe.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.auth.domain.model.AuthUser
import com.example.airvibe.feature.auth.domain.repository.AuthRepository
import com.example.airvibe.feature.chat.domain.repository.ChatRepository
import com.example.airvibe.feature.chat.domain.repository.ProximityRoomRepository
import com.example.airvibe.feature.radar.data.local.dao.SavedContactDao
import com.example.airvibe.feature.radar.data.sync.CloudSyncService
import com.example.airvibe.feature.radar.domain.model.VisibilityStats
import com.example.airvibe.feature.radar.domain.repository.RadarRepository
import com.example.airvibe.feature.radar.domain.repository.ScannerProfileRepository
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileStats(
    val trips: Int = 0,
    val rating: Int = 0,
    val friends: Int = 0,
)

/**
 * Feature 5 — Estado de la sección "Visibilidad Premium" del
 * perfil. Combina datos remotos (vistas/taps agregados en
 * `visibility_daily`) y datos locales (eventos aún no
 * sincronizados).
 */
data class ProfileVisibilityState(
    val stats: VisibilityStats = VisibilityStats.Empty,
    val isLoading: Boolean = false,
    val isPremium: Boolean = false,
    val errorMessage: String? = null,
)

class ProfileViewModel(
    private val profileRepository: ScannerProfileRepository = ServiceLocator.scannerProfileRepository,
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val radarRepository: RadarRepository = ServiceLocator.radarRepository,
    private val cloudSyncService: CloudSyncService = ServiceLocator.cloudSyncService,
    chatRepository: ChatRepository = ServiceLocator.chatRepository,
    roomRepository: ProximityRoomRepository = ServiceLocator.proximityRoomRepository,
    savedContactDao: SavedContactDao = ServiceLocator.savedContactDao,
) : ViewModel() {

    val profile: StateFlow<ScannerProfile> = profileRepository.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = profileRepository.current(),
        )

    val stats: StateFlow<ProfileStats> = combine(
        chatRepository.observeConversations(),
        roomRepository.observeActiveRooms(),
        savedContactDao.observeAll(),
    ) { chats, rooms, contacts ->
        ProfileStats(
            trips = chats.size,
            rating = rooms.size,
            friends = contacts.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileStats(),
    )

    // --------- Feature 5: Visibilidad Premium ---------

    private val _visibility = MutableStateFlow(ProfileVisibilityState())
    val visibility: StateFlow<ProfileVisibilityState> = _visibility.asStateFlow()

    init {
        observeAuthForVisibility()
    }

    private fun observeAuthForVisibility() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                val premium = isPremiumProfile()
                _visibility.update {
                    it.copy(
                        isPremium = premium,
                        errorMessage = null,
                    )
                }
                if (premium) refreshVisibility()
            }
        }
    }

    /**
     * Consideramos "Premium" si el usuario tiene sesión iniciada
     * (auth.uid() conocido). El flag `is_premium` se actualiza
     * server-side mediante RevenueCat (fuera de scope).
     */
    private fun isPremiumProfile(): Boolean = authRepository.currentUser.value != null

    fun refreshVisibility() {
        if (!_visibility.value.isPremium) return
        _visibility.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val userId = (authRepository.currentUser.value as? AuthUser)?.id
                ?: profileRepository.current().id
            // `pullVisibility` ya devuelve Result; no envolver
            // doblemente con runCatching.
            val stats: VisibilityStats = runCatching {
                cloudSyncService.pullVisibility(userId).getOrDefault(VisibilityStats.Empty)
            }.getOrDefault(VisibilityStats.Empty)
            _visibility.update {
                it.copy(
                    isLoading = false,
                    stats = stats,
                    errorMessage = it.errorMessage,
                )
            }
        }
    }

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    fun updateProfile(
        draft: com.example.airvibe.feature.radar.presentation.components.OwnProfileDraft
    ) {
        if (_isUpdating.value) return
        val trimmedName = draft.displayName.trim()
        val trimmedStatus = draft.status.trim()
        if (trimmedName.isEmpty() || trimmedStatus.isEmpty()) return
        _isUpdating.value = true
        viewModelScope.launch {
            val cleanedTags = draft.tags
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            runCatching {
                profileRepository.update(trimmedName, trimmedStatus, cleanedTags)
                profileRepository.updateKind(draft.kind)
                profileRepository.updatePresence(draft.presence)
                profileRepository.updateHeadline(draft.headline)
                profileRepository.updateBio(draft.bio)
                profileRepository.updatePremium(draft.isPremium, draft.premiumCatalog)
            }
                .onSuccess {
                    runCatching { profileRepository.syncToRemote(profileRepository.current()) }
                    runCatching { ServiceLocator.requestContactsSync() }
                }
            _isUpdating.value = false
        }
    }
}
