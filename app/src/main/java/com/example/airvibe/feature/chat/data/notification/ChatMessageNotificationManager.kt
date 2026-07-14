package com.example.airvibe.feature.chat.data.notification

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
import com.example.airvibe.feature.chat.domain.state.ActiveChatState

/**
 * Gestor de notificaciones para Mensajes Entrantes (Directos y Grupos).
 */
class ChatMessageNotificationManager(
    private val context: Context,
) {

    init {
        ensureChannel(context)
    }

    fun postDirectMessage(senderNodeId: String, senderName: String, text: String) {
        if (ActiveChatState.isAppInForeground.value && ActiveChatState.currentPeerId.value == senderNodeId) {
            return // El usuario ya está en este chat y la app está en primer plano
        }

        val manager = context.getSystemService<NotificationManager>() ?: return
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_CHAT_WITH_NODE_ID, senderNodeId)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(
            context,
            senderNodeId.hashCode(),
            openIntent,
            pendingFlags,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_radar_notification)
            .setContentTitle(senderName)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        try {
            manager.notify(senderNodeId.hashCode(), notification)
        } catch (t: SecurityException) {
            // Permiso POST_NOTIFICATIONS no concedido
        }
    }

    fun postRoomMessage(roomId: String, senderName: String, roomTitle: String, text: String) {
        if (ActiveChatState.isAppInForeground.value && ActiveChatState.currentRoomId.value == roomId) {
            return // El usuario ya está en esta sala y la app está en primer plano
        }

        val manager = context.getSystemService<NotificationManager>() ?: return
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_ROOM_ID, roomId)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(
            context,
            roomId.hashCode(),
            openIntent,
            pendingFlags,
        )

        val displayTitle = if (roomTitle.isNotBlank()) "$roomTitle ($senderName)" else senderName
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_radar_notification)
            .setContentTitle(displayTitle)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        try {
            manager.notify(roomId.hashCode(), notification)
        } catch (t: SecurityException) {
            // Permiso POST_NOTIFICATIONS no concedido
        }
    }

    companion object {
        const val CHANNEL_ID = "airvibe.messages"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService<NotificationManager>() ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mensajes de Chat",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notificaciones para mensajes entrantes directos y grupos"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            manager.createNotificationChannel(channel)
        }
    }
}
