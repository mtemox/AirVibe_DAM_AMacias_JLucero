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
import com.example.airvibe.feature.chat.domain.model.MatchResult
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Gestor de notificaciones de **matching inteligente**.
 *
 * Se suscribe al [MatchEngine.events] y, cuando llega un
 * [MatchResult.Match], postea una notificación push local con
 * un `PendingIntent` que abre [MainActivity] con un deep-link
 * hacia la conversación del peer.
 *
 * Las notificaciones se publican en un canal independiente
 * ([CHANNEL_ID]) con importancia alta para que el usuario las
 * vea aunque la app esté en background.
 */
class MatchNotificationManager(
    private val context: Context,
    private val engine: MatchEngine,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        ensureChannel(context)
        engine.start()
        scope.launch { observe(engine.events) }
        started = true
    }

    fun stop() {
        engine.stop()
        started = false
    }

    private suspend fun observe(flow: SharedFlow<MatchResult>) {
        flow.collect { result ->
            if (result is MatchResult.Match) {
                postMatchNotification(result.profile, result.matchedKeyword)
            }
        }
    }

    private fun postMatchNotification(profile: ScannerProfile, keyword: String) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val notification = build(profile, keyword)
        // Usamos el id del peer como notificationId para que una
        // nueva coincidencia sobre el mismo peer REEMPLACE la
        // anterior en lugar de apilar.
        val notificationId = profile.id.hashCode()
        try {
            manager.notify(notificationId, notification)
        } catch (t: SecurityException) {
            // Faltaría POST_NOTIFICATIONS. La UI debe pedirlo en
            // el modal de permisos.
        }
    }

    private fun build(profile: ScannerProfile, keyword: String): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_CHAT_WITH_NODE_ID, profile.id)
            putExtra(MainActivity.EXTRA_OPEN_CHAT_FROM_MATCH, true)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(
            context,
            profile.id.hashCode(),
            openIntent,
            pendingFlags,
        )

        val title = context.getString(R.string.match_notification_title)
        val body = context.getString(
            R.string.match_notification_body,
            profile.displayName,
            keyword.replaceFirstChar { it.uppercase() },
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_radar_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROMO)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "airvibe.matches"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService<NotificationManager>() ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.match_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.match_channel_description)
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }
}
