package com.example.airvibe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.feature.auth.domain.model.AuthStatus
import com.example.airvibe.feature.auth.presentation.AuthScreen
import com.example.airvibe.feature.radar.presentation.RadarScreen
import com.example.airvibe.core.di.ServiceLocator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirVibeApp()
        }
    }
}

/**
 * Router raíz de la app. Decide entre la pantalla de autenticación
 * o la del radar en función de la sesión actual. Se mantiene un
 * state-based router para evitar agregar una dependencia de
 * Navigation Compose en este paso.
 */
@Composable
private fun AirVibeApp() {
    val authStatus by ServiceLocator.authRepository.status.collectAsStateWithLifecycle()
    val darkTheme = remember { false } // el tema oscuro es opt-in desde Settings (futuro)
    AirVibeTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (authStatus) {
                AuthStatus.Loading -> SplashPlaceholder()
                AuthStatus.SignedOut -> AuthScreen()
                AuthStatus.SignedIn -> RadarScreen()
            }
        }
    }
}

@Composable
private fun SplashPlaceholder() {
    // Mantener simple: el tema ya pinta un fondo degradado.
    // En pasos futuros se puede agregar un spinner glass.
    Surface(modifier = Modifier.fillMaxSize()) { }
}
