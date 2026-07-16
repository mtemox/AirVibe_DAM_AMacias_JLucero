package com.example.airvibe.core.di

import android.content.Context
import com.example.airvibe.core.network.SupabaseClientFactory
import com.example.airvibe.feature.auth.data.repository.SupabaseAuthRepository
import com.example.airvibe.feature.auth.domain.repository.AuthRepository
import com.example.airvibe.feature.chat.data.device.nearby.NearbyChatMessageGateway
import com.example.airvibe.feature.chat.data.local.dao.ProximityRoomDao
import com.example.airvibe.feature.chat.data.remote.SupabaseChatMessageDataSource
import com.example.airvibe.feature.chat.data.remote.SupabaseProximityRoomDataSource
import com.example.airvibe.feature.chat.data.remote.SupabaseRoomMessageDataSource
import com.example.airvibe.feature.chat.data.repository.ProximityRoomRepositoryImpl
import com.example.airvibe.feature.chat.domain.repository.ProximityRoomRepository
import com.example.airvibe.feature.chat.data.notification.MatchEngine
import com.example.airvibe.feature.chat.data.notification.MatchNotificationManager
import com.example.airvibe.feature.chat.data.repository.ChatRepositoryImpl
import com.example.airvibe.feature.chat.data.repository.MatchPreferencesRepositoryImpl
import com.example.airvibe.feature.chat.domain.repository.ChatRepository
import com.example.airvibe.feature.chat.domain.repository.MatchPreferencesRepository
import com.example.airvibe.core.preferences.domain.AppPreferencesRepository
import com.example.airvibe.core.preferences.data.AppPreferencesRepositoryImpl
import com.example.airvibe.feature.chat.domain.scanner.ChatMessageGateway
import com.example.airvibe.feature.radar.data.device.handshake.HandshakeRequestNotificationManager
import com.example.airvibe.feature.radar.data.device.identity.DeviceIdentityProvider
import com.example.airvibe.feature.radar.data.device.nearby.NearbyRadarScanner
import com.example.airvibe.feature.radar.data.device.service.AirVibeScannerService
import com.example.airvibe.feature.radar.data.local.dao.HandshakeRequestDao
import com.example.airvibe.feature.radar.data.local.dao.ProfileViewDao
import com.example.airvibe.feature.radar.data.local.dao.RadarDao
import com.example.airvibe.feature.radar.data.local.database.AirVibeDatabase
import com.example.airvibe.feature.radar.data.remote.SupabaseNodeDataSource
import com.example.airvibe.feature.radar.data.remote.SupabaseProfileDataSource
import com.example.airvibe.feature.radar.data.remote.SupabaseAvatarDataSource
import com.example.airvibe.feature.radar.data.remote.SupabaseSavedContactDataSource
import com.example.airvibe.feature.radar.data.remote.SupabaseTelemetryDataSource
import com.example.airvibe.feature.radar.data.sync.CloudSyncService
import com.example.airvibe.feature.radar.data.repository.PremiumRepositoryImpl
import com.example.airvibe.feature.radar.data.repository.RadarRepositoryImpl
import com.example.airvibe.feature.radar.data.repository.ScannerProfileRepositoryImpl
import com.example.airvibe.feature.radar.data.sync.AirVibeWorkManagerConfiguration
import com.example.airvibe.feature.radar.data.sync.SyncScheduler
import com.example.airvibe.feature.radar.domain.remote.RemoteNodeDataSource
import com.example.airvibe.feature.radar.domain.repository.PremiumRepository
import com.example.airvibe.feature.radar.domain.repository.RadarRepository
import com.example.airvibe.feature.radar.domain.repository.ScannerProfileRepository
import com.example.airvibe.feature.radar.domain.scanner.RadarScanner
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile
import io.github.jan.supabase.SupabaseClient

/**
 * Service locator central.
 *
 * Paso 5: ahora también provee:
 *  - [ChatRepository] + [ChatMessageGateway] (offline-first).
 *  - [MatchPreferencesRepository] (filtros del usuario).
 *  - [MatchEngine] + [MatchNotificationManager] (alertas
 *    inteligentes en background).
 *
 * El orden de inicialización es importante: el [MatchEngine] se
 * construye después del [MatchPreferencesRepository] y antes del
 * [RadarScanner] para que éste último pueda pasarlo por
 * constructor.
 */
object ServiceLocator {

    @Volatile
    private var appContext: Context? = null

    /** Inicializa el locator; debe llamarse desde `Application.onCreate`. */
    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        // Inicializa el holder de contexto del SyncScheduler.
        AirVibeWorkManagerConfiguration.bind(context)
    }

    private val database: AirVibeDatabase by lazy {
        requireNotNull(appContext) {
            "ServiceLocator.init(context) must be called before accessing the database."
        }
        AirVibeDatabase.getInstance(appContext!!)
    }

    val radarDao: RadarDao by lazy { database.radarDao() }
    val savedContactDao by lazy { database.savedContactDao() }
    val chatDao by lazy { database.chatDao() }
    val proximityRoomDao: ProximityRoomDao by lazy { database.proximityRoomDao() }
    val handshakeRequestDao: HandshakeRequestDao by lazy { database.handshakeRequestDao() }
    val profileViewDao: ProfileViewDao by lazy { database.profileViewDao() }

    val supabaseClient: SupabaseClient by lazy { SupabaseClientFactory.create() }

    val radarRepository: RadarRepository by lazy {
        RadarRepositoryImpl(radarDao, savedContactDao, handshakeRequestDao, profileViewDao)
    }

    val appPreferencesRepository: AppPreferencesRepository by lazy {
        requireNotNull(appContext) {
            "ServiceLocator.init(context) must be called before accessing preferences."
        }
        AppPreferencesRepositoryImpl(appContext!!)
    }

    val authRepository: AuthRepository by lazy {
        SupabaseAuthRepository(
            supabase = supabaseClient,
            onSignedIn = {
                val userId = authRepository.currentUser.value?.id
                if (userId != null) {
                    runCatching { scannerProfileRepository.restoreFromRemote(userId) }
                    runCatching { cloudSyncService.restoreFromRemote(userId) }
                }
                SyncScheduler.requestNow()
                SyncScheduler.ensurePeriodic()
            },
            onSignedOut = {
                SyncScheduler.cancelAll()
            },
        )
    }

    val remoteNodeDataSource: RemoteNodeDataSource by lazy {
        SupabaseNodeDataSource(
            supabase = supabaseClient,
            radarDao = radarDao,
        )
    }

    val deviceIdentityProvider: DeviceIdentityProvider by lazy {
        DeviceIdentityProvider(requireNotNull(appContext) { "ServiceLocator.init(context) required." })
    }

    val profileRemoteDataSource: SupabaseProfileDataSource by lazy {
        SupabaseProfileDataSource(supabase = supabaseClient)
    }

    val avatarRemoteDataSource: SupabaseAvatarDataSource by lazy {
        SupabaseAvatarDataSource(supabase = supabaseClient)
    }

    val savedContactRemoteDataSource: SupabaseSavedContactDataSource by lazy {
        SupabaseSavedContactDataSource(supabase = supabaseClient)
    }

    val telemetryRemoteDataSource: SupabaseTelemetryDataSource by lazy {
        SupabaseTelemetryDataSource(supabase = supabaseClient)
    }

    val proximityRoomRemoteDataSource: SupabaseProximityRoomDataSource by lazy {
        SupabaseProximityRoomDataSource(supabase = supabaseClient)
    }

    val roomMessageRemoteDataSource: SupabaseRoomMessageDataSource by lazy {
        SupabaseRoomMessageDataSource(supabase = supabaseClient)
    }

    val chatMessageRemoteDataSource: SupabaseChatMessageDataSource by lazy {
        SupabaseChatMessageDataSource(supabase = supabaseClient)
    }

    private val chatNotificationsManager: com.example.airvibe.feature.chat.data.notification.ChatMessageNotificationManager by lazy {
        com.example.airvibe.feature.chat.data.notification.ChatMessageNotificationManager(
            requireNotNull(appContext) { "ServiceLocator.init(context) required." }
        )
    }

    val cloudSyncService: CloudSyncService by lazy {
        CloudSyncService(
            savedContactDao = savedContactDao,
            roomDao = proximityRoomDao,
            chatDao = chatDao,
            profileViewDao = profileViewDao,
            savedContactRemote = savedContactRemoteDataSource,
            roomRemote = proximityRoomRemoteDataSource,
            roomMessageRemote = roomMessageRemoteDataSource,
            chatMessageRemote = chatMessageRemoteDataSource,
            telemetryRemote = telemetryRemoteDataSource,
            chatNotifications = chatNotificationsManager,
        )
    }

    val scannerProfileRepository: ScannerProfileRepository by lazy {
        ScannerProfileRepositoryImpl(
            context = requireNotNull(appContext) { "ServiceLocator.init(context) required." },
            deviceIdentity = deviceIdentityProvider,
            profileRemote = profileRemoteDataSource,
            currentAuthUserId = { authRepository.currentUser.value?.id },
        )
    }

    val premiumRepository: PremiumRepository by lazy {
        PremiumRepositoryImpl(
            context = requireNotNull(appContext) { "ServiceLocator.init(context) required." },
            profileDataSource = profileRemoteDataSource,
            scannerProfileRepository = scannerProfileRepository,
            currentAuthUserId = { authRepository.currentUser.value?.id },
        )
    }

    val scannerProfileProvider: () -> ScannerProfile = { scannerProfileRepository.current() }

    val matchPreferencesRepository: MatchPreferencesRepository by lazy {
        requireNotNull(appContext) {
            "ServiceLocator.init(context) must be called before accessing match prefs."
        }
        MatchPreferencesRepositoryImpl(appContext!!)
    }

    /** Sincroniza contactos guardados pendientes tras login o al guardar. */
    fun requestContactsSync() = SyncScheduler.requestNow()

    val matchEngine: MatchEngine by lazy {
        MatchEngine(preferences = matchPreferencesRepository)
    }

    val proximityRoomRepository: ProximityRoomRepository by lazy {
        ProximityRoomRepositoryImpl(
            roomDao = proximityRoomDao,
            localUserIdProvider = { scannerProfileProvider().id },
            localDisplayNameProvider = { scannerProfileProvider().displayName },
            onDataChanged = { SyncScheduler.requestNow() },
        )
    }

    val chatRepositoryImpl: ChatRepositoryImpl by lazy {
        ChatRepositoryImpl(
            chatDao = chatDao,
            gateway = object : ChatMessageGateway {
                override suspend fun sendMessage(targetNodeId: String, text: String): Boolean =
                    chatGatewayImpl.sendMessage(targetNodeId, text)
                override suspend fun broadcast(text: String): Int =
                    chatGatewayImpl.broadcast(text)
                override suspend fun broadcastRoomInvite(text: String, roomId: String): Int =
                    chatGatewayImpl.broadcastRoomInvite(text, roomId)
                override suspend fun sendRoomMessage(roomId: String, text: String, messageId: String): Boolean =
                    chatGatewayImpl.sendRoomMessage(roomId, text, messageId)
                override suspend fun sendFriendAdd(targetNodeId: String): Boolean =
                    chatGatewayImpl.sendFriendAdd(targetNodeId)
                override suspend fun sendHandshakeRequest(
                    targetNodeId: String,
                    handshakeId: String,
                    key: String,
                ): Boolean = chatGatewayImpl.sendHandshakeRequest(targetNodeId, handshakeId, key)
                override suspend fun sendHandshakeAccept(
                    targetNodeId: String,
                    handshakeId: String,
                    key: String,
                ): Boolean = chatGatewayImpl.sendHandshakeAccept(targetNodeId, handshakeId, key)
                override suspend fun sendHandshakeReject(
                    targetNodeId: String,
                    handshakeId: String,
                ): Boolean = chatGatewayImpl.sendHandshakeReject(targetNodeId, handshakeId)
                override suspend fun sendRoomJoin(
                    hostNodeId: String,
                    roomId: String,
                    roomTitle: String,
                ): Boolean = chatGatewayImpl.sendRoomJoin(hostNodeId, roomId, roomTitle)
                override suspend fun sendRoomLeave(
                    hostNodeId: String,
                    roomId: String,
                ): Boolean = chatGatewayImpl.sendRoomLeave(hostNodeId, roomId)
                override suspend fun sendRoomDestroy(
                    roomId: String,
                ): Boolean = chatGatewayImpl.sendRoomDestroy(roomId)
                override suspend fun sendRoomAnnounce(
                    roomId: String,
                    messageId: String,
                    text: String,
                ): Boolean = chatGatewayImpl.sendRoomAnnounce(roomId, messageId, text)
                override fun roomEndpoints(roomId: String): Set<String> =
                    chatGatewayImpl.roomEndpoints(roomId)
                override fun onIncomingPayload(endpointId: String, bytes: ByteArray): Boolean =
                    chatGatewayImpl.onIncomingPayload(endpointId, bytes)
            },
            roomRepository = proximityRoomRepository,
            localUserIdProvider = { scannerProfileProvider().id },
            localDisplayNameProvider = { scannerProfileProvider().displayName },
            radarRepository = radarRepository,
        )
    }

    val chatRepository: ChatRepository by lazy { chatRepositoryImpl }

    val chatGatewayImpl: NearbyChatMessageGateway by lazy {
        requireNotNull(appContext) {
            "ServiceLocator.init(context) must be called before accessing the chat gateway."
        }
        NearbyChatMessageGateway(
            context = appContext!!,
            chatRepository = chatRepositoryImpl,
            roomRepository = proximityRoomRepository as ProximityRoomRepositoryImpl,
            localUserIdProvider = { scannerProfileProvider().id },
            localDisplayNameProvider = { scannerProfileProvider().displayName },
            localProfileProvider = { scannerProfileProvider() },
            connectedEndpointsProvider = { radarScannerConnectedEndpoints() },
            radarRepository = radarRepository,
            onPeerBound = { nodeId, endpointId ->
                val scanner = radarScanner
                if (scanner is NearbyRadarScanner) {
                    scanner.ensurePeerNode(nodeId, endpointId)
                }
            },
            handshakeNotifications = handshakeRequestNotificationManager,
            chatNotifications = chatMessageNotificationManager,
        )
    }

    val chatGateway: ChatMessageGateway by lazy { chatGatewayImpl }

    val radarScanner: RadarScanner by lazy {
        NearbyRadarScanner(
            context = requireNotNull(appContext) {
                "ServiceLocator.init(context) must be called before accessing the scanner."
            },
            repository = radarRepository,
            chatGateway = chatGatewayImpl,
            matchEngine = matchEngine,
        )
    }

    val matchNotificationManager: MatchNotificationManager by lazy {
        requireNotNull(appContext) {
            "ServiceLocator.init(context) must be called before accessing the match manager."
        }
        MatchNotificationManager(
            context = appContext!!,
            engine = matchEngine,
        )
    }

    val handshakeRequestNotificationManager: HandshakeRequestNotificationManager by lazy {
        requireNotNull(appContext) {
            "ServiceLocator.init(context) must be called before accessing the handshake manager."
        }
        HandshakeRequestNotificationManager(appContext!!)
    }

    val chatMessageNotificationManager: com.example.airvibe.feature.chat.data.notification.ChatMessageNotificationManager by lazy {
        requireNotNull(appContext) {
            "ServiceLocator.init(context) must be called before accessing the chat notification manager."
        }
        com.example.airvibe.feature.chat.data.notification.ChatMessageNotificationManager(appContext!!)
    }

    @Volatile
    private var matchManagerStarted: Boolean = false

    /**
     * Arranca el [matchNotificationManager] en background. Es
     * idempotente y lo invoca el [AirVibeScannerService] en
     * `onCreate`.
     */
    fun startMatchManager() {
        if (matchManagerStarted) return
        matchNotificationManager.start()
        matchManagerStarted = true
    }

    private fun radarScannerConnectedEndpoints(): Set<String> {
        val scanner = radarScanner
        return if (scanner is NearbyRadarScanner) {
            scanner.connectedEndpoints()
        } else {
            emptySet()
        }
    }

    val scannerLifecycle: ScannerLifecycle
        get() = ScannerLifecycle { action ->
            val ctx = requireNotNull(appContext) {
                "ServiceLocator.init(context) must be called before using the scanner service."
            }
            when (action) {
                ScannerLifecycle.Action.Start -> AirVibeScannerService.start(ctx)
                ScannerLifecycle.Action.Stop -> AirVibeScannerService.stop(ctx)
            }
        }
}

fun interface ScannerLifecycle {
    enum class Action { Start, Stop }
    fun execute(action: Action)
}
