package com.example.airvibe.feature.radar.data.sync

import android.content.Context
import androidx.work.Configuration

/**
 * Proveedor de configuración de WorkManager. WorkManager lo
 * detecta automáticamente al inicializarse (vía
 * `androidx.startup`) y aplica la [Configuration] que
 * exponemos.
 */
object AirVibeWorkManagerConfiguration : Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    /** Inicializa el holder de contexto que usa [SyncScheduler]. */
    fun bind(context: Context) {
        SyncSchedulerApp.initialize(context)
    }
}
