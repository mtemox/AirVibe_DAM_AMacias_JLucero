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
 * `CoroutineWorker` que sincroniza la base de datos local con
 * Supabase. Se ejecuta en background de forma periódica
 * (típicamente cada 15 minutos) o cuando el usuario lo solicita
 * de forma explícita desde la UI.
 *
 * Flujo:
 *
 *  1. Lee todos los nodos donde `is_synced = 0` desde Room.
 *  2. Los convierte a [com.example.airvibe.feature.radar.domain.remote.RemoteNode]
 *     y los envía al backend con `upsert` + `onConflict = id`.
 *  3. Si la subida es exitosa, marca esos registros como
 *     `is_synced = 1` en Room.
 *
 * El worker está diseñado para ser **idempotente**: si se vuelve
 * a encolar, los nodos ya sincronizados se omiten automáticamente
 * (porque Room filtra por `is_synced = 0`).
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val radarDao = ServiceLocator.radarDao
        val remote: RemoteNodeDataSource = ServiceLocator.remoteNodeDataSource

        val pending = radarDao.observePendingSync().first()
        if (pending.isEmpty()) return Result.success()

        val remoteNodes = pending.map { it.toRemoteNode() }
        val outcome = remote.upsert(remoteNodes)
        val syncedIds = outcome.getOrNull()
        return if (syncedIds != null) {
            radarDao.markAsSynced(syncedIds)
            Result.success()
        } else {
            val error = outcome.exceptionOrNull()
            if (error.isRetryable()) Result.retry() else Result.failure()
        }
    }
}

/**
 * Helper para distinguir errores recuperables de los que no lo
 * son (por ejemplo, 4xx que no se van a resolver con un retry).
 */
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

/**
 * Planificador del trabajo de sincronización. Centraliza las
 * constraints y la política de encolamiento para que el resto de
 * la app solo tenga que llamar a [requestNow] o
 * [ensurePeriodic].
 */
object SyncScheduler {

    const val UNIQUE_ONE_TIME = "airvibe.sync.once"
    const val UNIQUE_PERIODIC = "airvibe.sync.periodic"

    /**
     * Constraints del worker. Pedimos Wi-Fi (UNMETERED) para no
     * consumir datos móviles; si esto es demasiado restrictivo
     * para el contexto del usuario (ej. sólo tiene datos), se
     * puede cambiar a [NetworkType.CONNECTED] sin más
     * consecuencias.
     */
    private val constraints: Constraints
        get() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

    /** Dispara una sincronización inmediata (ignora el periodic). */
    fun requestNow() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(SyncSchedulerApp.context)
            .enqueueUniqueWork(UNIQUE_ONE_TIME, ExistingWorkPolicy.REPLACE, request)
    }

    /**
     * Asegura que haya un trabajo periódico encolado. Usa
     * [ExistingPeriodicWorkPolicy.KEEP] para no reiniciar el
     * ciclo si ya existe uno.
     */
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

    /** Cancela los trabajos de sincronización. */
    fun cancelAll() {
        val wm = WorkManager.getInstance(SyncSchedulerApp.context)
        wm.cancelUniqueWork(UNIQUE_ONE_TIME)
        wm.cancelUniqueWork(UNIQUE_PERIODIC)
    }
}

/**
 * Holder del [Context] de la aplicación. Se inicializa desde
 * [com.example.airvibe.AirVibeApplication.onCreate]. Mantenerlo
 * aquí evita que `SyncScheduler` (un object) tenga que recibir
 * un Context en cada llamada.
 */
internal object SyncSchedulerApp {
    @Volatile
    var context: Context = throw IllegalStateException(
        "SyncSchedulerApp.context must be initialized in Application.onCreate",
    )
        private set

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }
}
