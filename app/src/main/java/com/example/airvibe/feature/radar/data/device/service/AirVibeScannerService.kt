package com.example.airvibe.feature.radar.data.device.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.chat.data.notification.MatchNotificationManager
import com.example.airvibe.feature.radar.domain.scanner.RadarScanner
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile
import com.example.airvibe.feature.radar.domain.scanner.ScannerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano responsable de mantener vivo el
 * [RadarScanner] mientras la app no está en foco.
 *
 * Responsabilidades:
 *  - Publicar la notificación persistente ("Radar AirVibe activo").
 *  - Reenviar [ACTION_START] / [ACTION_STOP] al scanner a través
 *    del [ServiceLocator].
 *  - Inicializar el [com.example.airvibe.feature.chat.data.notification.MatchNotificationManager]
 *    para que las **alertas inteligentes** de matching puedan
 *    dispararse aunque la app esté en background.
 *  - Detenerse a sí mismo cuando el scanner queda en [ScannerState.Idle].
 *
 * El Service NO contiene la lógica de Nearby: la implementación vive
 * en la capa `data/device/nearby`. Esto respeta la regla de
 * dependencias: el service (infraestructura) depende del dominio,
 * nunca al revés.
 */
class AirVibeScannerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var startedForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        runCatching { ScannerNotificationFactory.ensureChannel(this) }
        runCatching { MatchNotificationManager.ensureChannel(this) }
        runCatching { com.example.airvibe.feature.chat.data.notification.RoomInviteNotificationManager.ensureChannel(this) }
        runCatching { com.example.airvibe.feature.radar.data.device.handshake.HandshakeRequestNotificationManager.ensureChannel(this) }
        runCatching { ServiceLocator.startMatchManager() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_STOP -> handleStop()
            else -> {
                // El sistema puede relanzar el service con un intent
                // null tras un kill. Nos limitamos a mostrar la
                // notificación y dejar que el ViewModel decida.
                startInForeground(paused = true)
            }
        }
        return START_STICKY
    }

    private fun handleStart() {
        try {
            startInForeground(paused = false)
            ScannerServiceState.markRunning()
        } catch (e: SecurityException) {
            Log.e(TAG, "Foreground service start denied", e)
            ScannerServiceState.markStopped()
            stopSelf()
            return
        }
        scope.launch {
            val scanner: RadarScanner = ServiceLocator.radarScanner
            val profile = ServiceLocator.scannerProfileProvider()
            val started = scanner.start(profile)
            if (!started) {
                handleStop()
            }
        }
    }

    private fun handleStop() {
        scope.launch {
            ServiceLocator.radarScanner.stop()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        ScannerServiceState.markStopped()
        stopSelf()
    }

    private fun startInForeground(paused: Boolean) {
        val notification = ScannerNotificationFactory.build(this, paused = paused)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                ScannerNotificationFactory.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(ScannerNotificationFactory.NOTIFICATION_ID, notification)
        }
        startedForeground = true
    }

    override fun onDestroy() {
        super.onDestroy()
        startedForeground = false
        ScannerServiceState.markStopped()
        Log.d(TAG, "AirVibeScannerService destroyed")
    }

    companion object {
        private const val TAG = "AirVibeScannerService"

        const val ACTION_START = "com.example.airvibe.action.SCANNER_START"
        const val ACTION_STOP = "com.example.airvibe.action.SCANNER_STOP"

        fun start(context: Context) {
            val intent = Intent(context, AirVibeScannerService::class.java).setAction(ACTION_START)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: SecurityException) {
                Log.e(TAG, "Cannot start foreground service — missing permissions", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Cannot start foreground service", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AirVibeScannerService::class.java).setAction(ACTION_STOP)
            try {
                context.startService(intent)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Cannot stop scanner service", e)
            }
        }
    }
}

/**
 * Estado reactivo del Foreground Service, expuesto a través de un
 * [StateFlow] para que la UI pueda mostrar un indicador cuando el
 * service está activo.
 */
object ScannerServiceState {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun markRunning() { _isRunning.value = true }
    fun markStopped() { _isRunning.value = false }
}
