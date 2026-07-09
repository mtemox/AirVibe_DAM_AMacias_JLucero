package com.example.airvibe

import android.app.Application
import androidx.work.Configuration
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.radar.data.sync.AirVibeWorkManagerConfiguration

/**
 * Punto de entrada de la aplicación. Inicializa el [ServiceLocator]
 * para que las dependencias (Room, Supabase, WorkManager) estén
 * listas antes de que cualquier ViewModel las solicite.
 *
 * Implementa [Configuration.Provider] para que WorkManager
 * aplique la configuración de [AirVibeWorkManagerConfiguration]
 * al auto-inicializar.
 */
class AirVibeApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = AirVibeWorkManagerConfiguration.workManagerConfiguration

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
