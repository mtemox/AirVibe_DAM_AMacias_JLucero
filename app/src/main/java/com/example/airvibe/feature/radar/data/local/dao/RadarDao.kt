package com.example.airvibe.feature.radar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.airvibe.feature.radar.data.local.entity.NodeEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO de la tabla `radar_nodes`. Expone un patrón reactivo basado en
 * [Flow] para que la UI observe cualquier cambio en la base de datos
 * sin necesidad de hacer polling manual.
 *
 * Las operaciones de escritura son `suspend` para ejecutarse en el
 * dispatcher IO; las lecturas reactivas permanecen como [Flow].
 */
@Dao
interface RadarDao {

    /** Stream reactivo con todos los nodos ordenados por nombre. */
    @Query("SELECT * FROM radar_nodes ORDER BY display_name ASC")
    fun observeAll(): Flow<List<NodeEntity>>

    /** Stream reactivo con los nodos aún pendientes de sincronizar. */
    @Query("SELECT * FROM radar_nodes WHERE is_synced = 0 ORDER BY updated_at ASC")
    fun observePendingSync(): Flow<List<NodeEntity>>

    /** Búsqueda puntual por identificador. */
    @Query("SELECT * FROM radar_nodes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NodeEntity?

    /** Conteo instantáneo. Útil para la estrategia de seed inicial. */
    @Query("SELECT COUNT(*) FROM radar_nodes")
    suspend fun count(): Int

    /** Inserta; si el id ya existe, lo reemplaza (alias de [upsertAll]). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nodes: List<NodeEntity>)

    @Update
    suspend fun update(node: NodeEntity)

    @Upsert
    suspend fun upsertAll(nodes: List<NodeEntity>)

    @Query("UPDATE radar_nodes SET is_favorite = :favorite, updated_at = :timestamp, is_synced = 0 WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE radar_nodes SET is_synced = 1, updated_at = :timestamp WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM radar_nodes WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Elimina únicamente los nodos cuyo identificador NO empiece
     * con `LOCAL_`. Sirve para limpiar peers descubiertos por
     * Bluetooth sin tocar los datos del seed.
     */
    @Query("DELETE FROM radar_nodes WHERE id NOT LIKE 'LOCAL\\_%' ESCAPE '\\'")
    suspend fun clearDiscovered()

    @Query("DELETE FROM radar_nodes WHERE id LIKE 'pending-%'")
    suspend fun deletePendingNodes()

    @Query("DELETE FROM radar_nodes")
    suspend fun clear()

    /**
     * Si la tabla está vacía, inserta el lote inicial (seed). Se
     * ejecuta en una transacción para que dos hilos no terminen
     * duplicando filas.
     */
    @Transaction
    suspend fun seedIfEmpty(seed: List<NodeEntity>) {
        if (count() == 0) insertAll(seed)
    }
}
