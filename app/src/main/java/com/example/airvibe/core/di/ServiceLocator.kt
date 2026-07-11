package com.example.airvibe.core.di

import android.content.Context
import com.example.airvibe.core.network.SupabaseClientFactory
import com.example.airvibe.feature.auth.data.repository.SupabaseAuthRepository
import com.example.airvibe.feature.auth.domain.repository.AuthRepository
import com.example.airvibe.feature.chat.data.device.nearby.NearbyChatMessageGateway
import com.example.airvibe.feature.chat.data.notification.MatchEngine
import com.example.airvibe.feature.chat.data.notification.MatchNotificationManager
import com.example.airvibe.feature.chat.data.repository.ChatRepositoryImpl
import com.example.airvibe.feature.chat.data.repository.MatchPreferencesRepositoryImpl
import com.example.airvibe.feature.chat.domain.repository.ChatRepository
import com.example.airvibe.feature.chat.domain.repository.MatchPreferencesRepository
import com.example.airvibe.feature.chat.domain.scanner.ChatMessageGateway
import com.example.airvibe.feature.radar.data.device.identity.DeviceIdentityProvider
import com.example.airvibe.feature.radar.data.device.nearby.NearbyRadarScanner
import com.example.airvibe.feature.radar.data.device.service.AirVibeScannerService
import com.example.airvibe.feature.radar.data.local.dao.RadarDao
import com.example.airvibe.feature.radar.data.local.database.AirVibeDatabase
import com.example.airvibe.feature.radar.data.remote.SupabaseNodeDataSource
import com.example.airvibe.feature.radar.data.repository.RadarRepositoryImpl
import com.example.airvibe.feature.radar.data.repository.ScannerProfileRepositoryImpl
import com.example.airvibe.feature.radar.data.sync.AirVibeWorkManagerConfiguration
import com.example.airvibe.feature.radar.data.sync.SyncScheduler
import com.example.airvibe.feature.radar.domain.remote.RemoteNodeDataSource
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
    val chatDao by lazy { database.chatDao() }

    val supabaseClient: SupabaseClient by lazy { SupabaseClientFactory.create() }

    val radarRepository: RadarRepository by lazy { RadarRepositoryImpl(radarDao) }

    val authRepository: AuthRepository by lazy {
        SupabaseAuthRepository(
            supabase = supabaseClient,
            onSignedIn = {
                // Al iniciar sesión, dispara una sincronización
                // inmediata para empujar el contenido local.
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

    val scannerProfileRepository: ScannerProfileRepository by lazy {
        ScannerProfileRepositoryImpl(
            context = requireNotNull(appContext) { "ServiceLocator.init(context) required." },
            deviceIdentity = deviceIdentityProvider,
        )
    }

    val scannerProfileProvider: () -> ScannerProfile = { scannerProfileRepository.current() }

    val matchPreferencesRepository: MatchPreferencesRepository by lazy {
        requireNotNull(appContext) {
            "ServiceLocator.init(context) must be called before accessing match prefs."
        }
        MatchPreferencesRepositoryImpl(appContext!!)
    }

    val matchEngine: MatchEngine by lazy {
        MatchEngine(preferences = matchPreferencesRepository)
    }

    val chatRepositoryImpl: ChatRepositoryImpl by lazy {
        ChatRepositoryImpl(
            chatDao = chatDao,
            gateway = object : ChatMessageGateway {
                override suspend fun sendMessage(targetNodeId: String, text: String): Boolean =
                    chatGateway.sendMessage(targetNodeId, text)
                override suspend fun broadcast(text: String): Int =
                    chatGateway.broadcast(text)
                override fun onIncomingPayload(endpointId: String, bytes: ByteArray): Boolean =
                    chatGateway.onIncomingPayload(endpointId, bytes)
            },
            localUserIdProvider = { scannerProfileProvider().id },
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
            localUserIdProvider = { scannerProfileProvider().id },
            connectedEndpointsProvider = { radarScannerConnectedEndpoints() },
            radarRepository = radarRepository,
            onPeerBound = { nodeId, endpointId ->
                val scanner = radarScanner
                if (scanner is NearbyRadarScanner) {
                    scanner.ensurePeerNode(nodeId, endpointId)
                }
            },
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
