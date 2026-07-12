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

/**
 * Notificaciones de invitación a salas de proximidad.
 * No abre un chat 1-a-1 invasivo: invita a unirse a una sala.
 */
object RoomInviteNotificationManager {

    const val CHANNEL_ID = "airvibe.room_invites"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.room_invite_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.room_invite_channel_description)
            enableLights(true)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun postInvite(
        context: Context,
        roomId: String,
        hostName: String,
        roomTitle: String,
    ) {
        ensureChannel(context)
        val manager = context.getSystemService<NotificationManager>() ?: return
        val notification = build(context, roomId, hostName, roomTitle)
        val notificationId = roomId.hashCode()
        try {
            manager.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Falta POST_NOTIFICATIONS
        }
    }

    private fun build(
        context: Context,
        roomId: String,
        hostName: String,
        roomTitle: String,
    ): Notification {
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

        val title = context.getString(R.string.room_invite_notification_title, hostName)
        val body = context.getString(R.string.room_invite_notification_body, roomTitle)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_radar_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_radar_notification,
                context.getString(R.string.room_invite_action_join),
                contentIntent,
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}
