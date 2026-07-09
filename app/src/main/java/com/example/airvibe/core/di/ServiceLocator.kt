package com.example.airvibe.core.di

import android.content.Context
import com.example.airvibe.core.network.SupabaseClientFactory
import com.example.airvibe.feature.auth.data.repository.SupabaseAuthRepository
import com.example.airvibe.feature.auth.domain.repository.AuthRepository
import com.example.airvibe.feature.radar.data.device.nearby.NearbyRadarScanner
import com.example.airvibe.feature.radar.data.device.service.AirVibeScannerService
import com.example.airvibe.feature.radar.data.device.service.DefaultScannerProfile
import com.example.airvibe.feature.radar.data.local.dao.RadarDao
import com.example.airvibe.feature.radar.data.local.database.AirVibeDatabase
import com.example.airvibe.feature.radar.data.remote.SupabaseNodeDataSource
import com.example.airvibe.feature.radar.data.repository.RadarRepositoryImpl
import com.example.airvibe.feature.radar.data.sync.AirVibeWorkManagerConfiguration
import com.example.airvibe.feature.radar.data.sync.SyncScheduler
import com.example.airvibe.feature.radar.domain.remote.RemoteNodeDataSource
import com.example.airvibe.feature.radar.domain.repository.RadarRepository
import com.example.airvibe.feature.radar.domain.scanner.RadarScanner
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile
import io.github.jan.supabase.SupabaseClient

/**
 * Service locator central. Ahora provee:
 *  - Room (BD local).
 *  - Supabase (cliente único con Auth + Postgrest).
 *  - Repositorios de dominio (radar, auth).
 *  - Scanner Nearby + fachada del Foreground Service.
 *  - DataSource remoto + scheduler de WorkManager.
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

    val scannerProfileProvider: () -> ScannerProfile = { DefaultScannerProfile.profile }

    val radarScanner: RadarScanner by lazy {
        NearbyRadarScanner(
            context = requireNotNull(appContext) {
                "ServiceLocator.init(context) must be called before accessing the scanner."
            },
            repository = radarRepository,
        )
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
