package com.example.airvibe.feature.radar.presentation

import com.example.airvibe.feature.chat.domain.model.MatchCriteria
import com.example.airvibe.feature.radar.data.seed.RadarSeedData
import com.example.airvibe.feature.radar.domain.model.HandshakeRequest
import com.example.airvibe.feature.radar.domain.model.PersonProfile
import com.example.airvibe.feature.radar.domain.model.RadarNode
import com.example.airvibe.feature.radar.domain.model.RadarNodeKind
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
    val hasAutoStarted: Boolean = false,
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
    val isOwnProfileVisible: Boolean = false,
    val isBroadcastVisible: Boolean = false,
    val isBroadcasting: Boolean = false,
    val lastBroadcastCount: Int = 0,
    val lastBroadcastRoomId: String? = null,
    val contactAddedMessage: String? = null,
    val ownProfile: com.example.airvibe.feature.radar.domain.scanner.ScannerProfile? = null,
    val unreadChatCount: Int = 0,
    val hideDemoNodes: Boolean = false,
    val liveNodes: List<RadarNode> = emptyList(),
    val scannerServiceRunning: Boolean = false,
    // ---- Feature 3: Handshake ----
    val incomingHandshakes: List<HandshakeRequest> = emptyList(),
    val activeHandshake: HandshakeRequest? = null,
    val isHandshakeSheetVisible: Boolean = false,
    val handshakeSentMessage: String? = null,
) {
    val visibleNodes: List<RadarNode>
        get() = if (hideDemoNodes) {
            nodes.filterNot { it.id.startsWith(RadarSeedData.SEED_ID_PREFIX) }
        } else {
            nodes
        }

    /** Nodos en vivo del radar: solo peers detectados ahora, sin fantasmas de Room. */
    val displayNodes: List<RadarNode>
        get() {
            val ownId = ownProfile?.id
            return liveNodes
                .filter { ownId == null || it.id != ownId }
                .filterNot { it.id.startsWith("pending-") }
                .filterNot { hideDemoNodes && it.id.startsWith(RadarSeedData.SEED_ID_PREFIX) }
        }

    val hasNodes: Boolean get() = displayNodes.isNotEmpty()
    val activeNodeCount: Int get() = displayNodes.count {
        it.presence != com.example.airvibe.feature.radar.domain.model.PresenceStatus.Away
    }
    /** Conteo visible en la barra: nodos persistidos o endpoints en descubrimiento. */
    val proximityCount: Int get() = maxOf(activeNodeCount, discoveredPeers)
    val scannerError: ScannerError? = (scannerState as? ScannerState.Error)?.reason
}
