package com.example.airvibe.feature.radar.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.airvibe.core.di.ServiceLocator
import com.example.airvibe.feature.radar.data.mapper.toRemoteNode
import com.example.airvibe.feature.radar.domain.remote.RemoteNodeDataSource
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Worker que sincroniza la base de datos local con Supabase.
 *
 * Flujo:
 *  1. Restaura salas, mensajes de sala y amigos desde la nube.
 *  2. Sube nodos del radar pendientes.
 *  3. Sube amigos, salas y mensajes de sala pendientes.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = ServiceLocator.authRepository.currentUser.value?.id
            ?: return Result.success()

        val cloudSync = ServiceLocator.cloudSyncService

        // Pull primero para recuperar datos tras reinstalación
        val restoreOutcome = cloudSync.restoreFromRemote(userId)
        if (restoreOutcome.isFailure) {
            val error = restoreOutcome.exceptionOrNull()
            if (error.isRetryable()) return Result.retry()
        }

        // Push radar nodes
        val radarDao = ServiceLocator.radarDao
        val remote: RemoteNodeDataSource = ServiceLocator.remoteNodeDataSource
        val pendingNodes = radarDao.observePendingSync().first()
        if (pendingNodes.isNotEmpty()) {
            val remoteNodes = pendingNodes.map { it.toRemoteNode() }
            val nodeOutcome = remote.upsert(remoteNodes)
            val syncedIds = nodeOutcome.getOrNull()
            if (syncedIds != null) {
                radarDao.markAsSynced(syncedIds)
            } else {
                val error = nodeOutcome.exceptionOrNull()
                if (error.isRetryable()) return Result.retry()
            }
        }

        // Push contacts, rooms, room messages
        val pushOutcome = cloudSync.pushPending(userId)
        return if (pushOutcome.isSuccess) {
            Result.success()
        } else {
            val error = pushOutcome.exceptionOrNull()
            if (error.isRetryable()) Result.retry() else Result.failure()
        }
    }
}

private fun Throwable?.isRetryable(): Boolean = when (this) {
    null -> false
    is java.io.IOException -> true
    else -> {
        val msg = message.orEmpty().lowercase()
        "timeout" in msg ||
            "network" in msg ||
            "unavailable" in msg ||
            "5" in msg.take(1)
    }
}

object SyncScheduler {

    const val UNIQUE_ONE_TIME = "airvibe.sync.once"
    const val UNIQUE_PERIODIC = "airvibe.sync.periodic"

    private val constraints: Constraints
        get() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

    fun requestNow() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(SyncSchedulerApp.context)
            .enqueueUniqueWork(UNIQUE_ONE_TIME, ExistingWorkPolicy.REPLACE, request)
    }

    fun ensurePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(SyncSchedulerApp.context)
            .enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }

    fun cancelAll() {
        val wm = WorkManager.getInstance(SyncSchedulerApp.context)
        wm.cancelUniqueWork(UNIQUE_ONE_TIME)
        wm.cancelUniqueWork(UNIQUE_PERIODIC)
    }
}

internal object SyncSchedulerApp {
    @Volatile
    private var _context: Context? = null

    val context: Context
        get() = _context ?: throw IllegalStateException(
            "SyncSchedulerApp.context must be initialized in Application.onCreate",
        )

    fun initialize(context: Context) {
        _context = context.applicationContext
    }
}
