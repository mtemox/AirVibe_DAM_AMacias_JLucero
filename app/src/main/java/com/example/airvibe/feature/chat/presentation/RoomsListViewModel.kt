package com.example.airvibe.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.chat.domain.model.ProximityRoom
import com.example.airvibe.feature.chat.domain.repository.ProximityRoomRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RoomsListViewModel(
    roomRepository: ProximityRoomRepository = ServiceLocator.proximityRoomRepository,
) : ViewModel() {
    val rooms: StateFlow<List<ProximityRoom>> = roomRepository.observeActiveRooms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
