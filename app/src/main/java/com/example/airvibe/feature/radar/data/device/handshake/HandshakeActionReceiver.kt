package com.example.airvibe.feature.radar.data.device.handshake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.airvibe.core.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Feature 3 — Handshake.
 * `BroadcastReceiver` invocado cuando el usuario pulsa las
 * acciones **Aceptar** o **Rechazar** de la notificación de
 * handshake. Delega en el repositorio + gateway de chat.
 */
class HandshakeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val handshakeId = intent.getStringExtra(EXTRA_HANDSHAKE_ID) ?: return
        val accept = when (intent.action) {
            ACTION_ACCEPT -> true
            ACTION_REJECT -> false
            else -> return
        }
        val pending = goAsync()
        scope.launch {
            try {
                handle(context.applicationContext, handshakeId, accept)
            } catch (t: Throwable) {
                Log.w(TAG, "handshake action failed: ${t.message}", t)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(appContext: Context, handshakeId: String, accept: Boolean) {
        val locator = ServiceLocator
        val handshake = locator.radarRepository.getHandshakeById(handshakeId) ?: return
        val result = locator.radarRepository.respondToHandshake(handshakeId, accept = accept)
        locator.handshakeRequestNotificationManager.dismiss(handshakeId)
        locator.requestContactsSync()
        val response = when (accept) {
            true -> {
                locator.chatGatewayImpl.sendHandshakeAccept(
                    targetNodeId = handshake.peerNodeId,
                    handshakeId = handshake.handshakeId,
                    key = handshake.handshakeKey,
                )
            }
            false -> {
                locator.chatGatewayImpl.sendHandshakeReject(
                    targetNodeId = handshake.peerNodeId,
                    handshakeId = handshake.handshakeId,
                )
            }
        }
        Log.d(TAG, "Handshake $handshakeId ($accept) responded=$response, contact=${result?.status}")
    }

    companion object {
        const val ACTION_ACCEPT = "com.example.airvibe.action.HANDSHAKE_ACCEPT"
        const val ACTION_REJECT = "com.example.airvibe.action.HANDSHAKE_REJECT"
        const val EXTRA_HANDSHAKE_ID = "airvibe.extra.HANDSHAKE_ID"
        private const val TAG = "HandshakeActionRx"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
