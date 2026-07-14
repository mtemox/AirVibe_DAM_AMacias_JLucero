package com.example.airvibe.feature.radar.data.device.handshake

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.example.airvibe.MainActivity
import com.example.airvibe.R
import com.example.airvibe.feature.radar.domain.model.HandshakeRequest

/**
 * Feature 3 — Handshake.
 * Publica una **notificación nativa local** cuando llega una
 * solicitud de conexión de un peer cercano. La notificación
 * expone dos acciones:
 *
 *  - **Aceptar** → broadcast a [HandshakeActionReceiver] que
 *    termina de procesar el handshake (responde al peer +
 *    guarda el contacto local).
 *  - **Rechazar** → broadcast análogo, marca la solicitud
 *    como `Rejected` y notifica al peer.
 *
 * El `PendingIntent` principal abre `MainActivity` con
 * `EXTRA_OPEN_HANDSHAKE_REQUEST` para mostrar el sheet de
 * decisión dentro de la app.
 */
class HandshakeRequestNotificationManager(
    private val context: Context,
) {

    fun postIncoming(request: HandshakeRequest) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        ensureChannel(context)
        val notification = build(request) ?: return
        try {
            manager.notify(notificationId(request.handshakeId), notification)
        } catch (security: SecurityException) {
            // Faltaría POST_NOTIFICATIONS. La UI debe pedirlo
            // en el modal de permisos.
        }
    }

    fun dismiss(handshakeId: String) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        manager.cancel(notificationId(handshakeId))
    }

    fun notificationId(handshakeId: String): Int =
        // Mantener el id estable por handshake para que una
        // actualización reemplace la notificación anterior.
        HANDSHAKE_NOTIFICATION_ID_BASE + handshakeId.hashCode()

    private fun build(request: HandshakeRequest): Notification? {
        val title = context.getString(
            R.string.handshake_notification_title,
            request.peerDisplayName,
        )
        val body = request.peerHeadline.ifBlank { request.peerStatus }
            .ifBlank { context.getString(R.string.handshake_notification_default_body) }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_HANDSHAKE_REQUEST, request.handshakeId)
        }
        val contentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(
            context,
            request.handshakeId.hashCode(),
            openIntent,
            contentFlags,
        )

        val acceptIntent = Intent(context, HandshakeActionReceiver::class.java).apply {
            action = HandshakeActionReceiver.ACTION_ACCEPT
            putExtra(HandshakeActionReceiver.EXTRA_HANDSHAKE_ID, request.handshakeId)
        }
        val acceptPending = PendingIntent.getBroadcast(
            context,
            ("accept-" + request.handshakeId).hashCode(),
            acceptIntent,
            contentFlags,
        )

        val rejectIntent = Intent(context, HandshakeActionReceiver::class.java).apply {
            action = HandshakeActionReceiver.ACTION_REJECT
            putExtra(HandshakeActionReceiver.EXTRA_HANDSHAKE_ID, request.handshakeId)
        }
        val rejectPending = PendingIntent.getBroadcast(
            context,
            ("reject-" + request.handshakeId).hashCode(),
            rejectIntent,
            contentFlags,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_radar_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_radar_notification,
                    context.getString(R.string.handshake_action_accept),
                    acceptPending,
                ).build(),
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_radar_notification,
                    context.getString(R.string.handshake_action_reject),
                    rejectPending,
                ).build(),
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "airvibe.handshake"
        private const val HANDSHAKE_NOTIFICATION_ID_BASE = 1_000_000

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService<NotificationManager>() ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.handshake_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.handshake_channel_description)
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }
}
