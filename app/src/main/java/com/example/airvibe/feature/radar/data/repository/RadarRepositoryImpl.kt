package com.example.airvibe.feature.radar.data.repository

import com.example.airvibe.feature.radar.data.local.dao.RadarDao
import com.example.airvibe.feature.radar.data.mapper.NodeMapper.toDomain
import com.example.airvibe.feature.radar.data.mapper.NodeMapper.toEntity
import com.example.airvibe.feature.radar.data.mapper.NodeMapper.toProfile
import com.example.airvibe.feature.radar.data.seed.RadarSeedData
import com.example.airvibe.feature.radar.domain.model.PersonProfile
import com.example.airvibe.feature.radar.domain.model.RadarNode
import com.example.airvibe.feature.radar.domain.repository.RadarRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Implementación offline-first del repositorio de radar.
 *
 * Esta clase es la **única fuente de verdad** (Single Source of Truth)
 * para la capa de presentación. Internamente:
 *
 *  1. Garantiza que la base de datos tenga un seed inicial la primera
 *     vez que se invoca.
 *  2. Expone un [Flow] derivado del DAO; cualquier cambio en Room se
 *     propaga automáticamente a la UI.
 *  3. Mapea las entidades persistidas a modelos de dominio puros.
 *  4. Recibe los peers descubiertos por el [RadarScanner] a través
 *     de [upsertNode] / [removeNode] / [clearDiscoveredNodes].
 *
 * En pasos posteriores se añadirá un `SyncWorker` que observe las
 * filas con `isSynced = 0` y las empuje a Supabase cuando haya red.
 */
class RadarRepositoryImpl(
    private val radarDao: RadarDao,
) : RadarRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            radarDao.seedIfEmpty(RadarSeedData.seedEntities())
        }
    }

    override fun observeRadarNodes(): Flow<List<RadarNode>> =
        radarDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getProfile(nodeId: String): PersonProfile? {
        val entity = radarDao.getById(nodeId) ?: return null
        return entity.toProfile()
    }

    override suspend fun toggleFavorite(nodeId: String): Boolean {
        val entity = radarDao.getById(nodeId) ?: return false
        val newValue = !entity.isFavorite
        radarDao.setFavorite(id = nodeId, favorite = newValue)
        return newValue
    }

    override suspend fun upsertNode(node: RadarNode) {
        val now = System.currentTimeMillis()
        val existing = radarDao.getById(node.id)
        val entity = node.toEntity(
            isFavorite = existing?.isFavorite ?: false,
            isSynced = false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        radarDao.upsertAll(listOf(entity))
    }

    override suspend fun removeNode(nodeId: String) {
        radarDao.deleteById(nodeId)
    }

    override suspend fun clearDiscoveredNodes() {
        radarDao.clearDiscovered()
    }

    override suspend fun removePendingNodes() {
        radarDao.deletePendingNodes()
    }
}
