package com.example.airvibe.feature.radar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.radar.domain.model.PersonProfile
import com.example.airvibe.feature.radar.domain.repository.RadarRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FriendsViewModel(
    private val repository: RadarRepository = ServiceLocator.radarRepository,
) : ViewModel() {
    val friends: StateFlow<List<PersonProfile>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteFriend(nodeId: String) {
        viewModelScope.launch {
            repository.deleteContact(nodeId)
        }
    }
}
