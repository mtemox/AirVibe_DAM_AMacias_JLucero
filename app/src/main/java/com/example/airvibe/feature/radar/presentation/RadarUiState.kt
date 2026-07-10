package com.example.airvibe.feature.radar.presentation

import com.example.airvibe.feature.chat.domain.model.MatchCriteria
import com.example.airvibe.feature.radar.domain.model.PersonProfile
import com.example.airvibe.feature.radar.domain.model.RadarNode
import com.example.airvibe.feature.radar.domain.scanner.ScannerError
import com.example.airvibe.feature.radar.domain.scanner.ScannerState

/**
 * Estado inmutable que consume la pantalla del radar. La UI nunca
 * modifica este objeto: lo recibe del ViewModel y dispara eventos
 * a través de [RadarUiEvent].
 */
data class RadarUiState(
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val nodes: List<RadarNode> = emptyList(),
    val selectedNode: RadarNode? = null,
    val selectedProfile: PersonProfile? = null,
    val isSheetVisible: Boolean = false,
    val errorMessage: String? = null,
    val scannerState: ScannerState = ScannerState.Idle,
    val discoveredPeers: Int = 0,
    val pendingPermissionRequest: Boolean = false,
    val matchCriteria: MatchCriteria = MatchCriteria(),
    val isMatchFiltersVisible: Boolean = false,
    val unreadChatCount: Int = 0,
) {
    val hasNodes: Boolean get() = nodes.isNotEmpty()
    val activeNodeCount: Int get() = nodes.count { it.presence != com.example.airvibe.feature.radar.domain.model.PresenceStatus.Away }
    val scannerError: ScannerError? = (scannerState as? ScannerState.Error)?.reason
}
