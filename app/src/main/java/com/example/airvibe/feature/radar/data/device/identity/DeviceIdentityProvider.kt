package com.example.airvibe.feature.radar.data.device.identity

import android.content.Context
import java.util.UUID

/**
 * Genera y persiste un identificador estable por instalación.
 * Cada dispositivo debe anunciar un [ScannerProfile.id] distinto
 * para que dos teléfonos no colisionen en Room ni en el chat P2P.
 */
class DeviceIdentityProvider(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun deviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val fresh = "device-${UUID.randomUUID()}"
        prefs.edit().putString(KEY_DEVICE_ID, fresh).apply()
        return fresh
    }

    companion object {
        private const val PREFS_NAME = "airvibe.device_identity"
        private const val KEY_DEVICE_ID = "device.id"
    }
}
