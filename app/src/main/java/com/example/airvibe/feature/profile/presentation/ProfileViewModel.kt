package com.example.airvibe.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.chat.domain.repository.ChatRepository
import com.example.airvibe.feature.chat.domain.repository.ProximityRoomRepository
import com.example.airvibe.feature.radar.data.local.dao.SavedContactDao
import com.example.airvibe.feature.radar.domain.repository.ScannerProfileRepository
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileStats(
    val trips: Int = 0,
    val rating: Int = 0,
    val friends: Int = 0,
)

class ProfileViewModel(
    private val profileRepository: ScannerProfileRepository = ServiceLocator.scannerProfileRepository,
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

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    fun updateProfile(
        displayName: String,
        status: String,
        tags: List<String>,
    ) {
        if (_isUpdating.value) return
        val trimmedName = displayName.trim()
        val trimmedStatus = status.trim()
        if (trimmedName.isEmpty() || trimmedStatus.isEmpty()) return
        _isUpdating.value = true
        viewModelScope.launch {
            val cleanedTags = tags
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            runCatching { profileRepository.update(trimmedName, trimmedStatus, cleanedTags) }
                .onSuccess {
                    runCatching { profileRepository.syncToRemote(profileRepository.current()) }
                    runCatching { ServiceLocator.requestContactsSync() }
                }
            _isUpdating.value = false
        }
    }
}
