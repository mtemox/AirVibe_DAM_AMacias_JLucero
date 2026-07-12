package com.example.airvibe.feature.radar.data.repository



import com.example.airvibe.feature.radar.data.local.dao.RadarDao

import com.example.airvibe.feature.radar.data.local.dao.SavedContactDao

import com.example.airvibe.feature.radar.data.mapper.ContactMapper

import com.example.airvibe.feature.radar.data.mapper.ContactMapper.toEntity

import com.example.airvibe.feature.radar.data.mapper.ContactMapper.toProfile

import com.example.airvibe.feature.radar.data.mapper.ContactMapper.toSavedContact

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



class RadarRepositoryImpl(

    private val radarDao: RadarDao,

    private val savedContactDao: SavedContactDao,

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

        val live = radarDao.getById(nodeId)?.toProfile()

        if (live != null) {

            val saved = savedContactDao.getById(nodeId)

            return if (saved != null) live.copy(isFavorite = true) else live

        }

        return savedContactDao.getById(nodeId)?.toProfile()

    }



    override suspend fun toggleFavorite(nodeId: String): Boolean {

        val saved = savedContactDao.getById(nodeId)

        return if (saved != null) {

            savedContactDao.deleteById(nodeId)

            radarDao.getById(nodeId)?.let { radarDao.setFavorite(nodeId, false) }

            false

        } else {

            val entity = radarDao.getById(nodeId) ?: return false

            savedContactDao.upsert(entity.toSavedContact())

            radarDao.setFavorite(nodeId, true)

            true

        }

    }



    override suspend fun addFavorite(nodeId: String): Boolean {

        val existing = savedContactDao.getById(nodeId)

        if (existing != null) return true

        val entity = radarDao.getById(nodeId) ?: return false

        savedContactDao.upsert(entity.toSavedContact())

        radarDao.setFavorite(nodeId, true)

        return true

    }



    override suspend fun saveContact(profile: PersonProfile, addedByPeer: Boolean): Boolean {

        val existing = savedContactDao.getById(profile.id)

        val now = System.currentTimeMillis()

        val entity = profile.toEntity(

            addedByPeer = addedByPeer,

            isSynced = false,

            createdAt = existing?.createdAt ?: now,

            updatedAt = now,

            accentColorArgb = ContactMapper.accentArgbFromProfile(profile),

        )

        savedContactDao.upsert(entity)

        radarDao.getById(profile.id)?.let { radarDao.setFavorite(profile.id, true) }

        return true

    }



    override suspend fun isSavedContact(nodeId: String): Boolean =

        savedContactDao.exists(nodeId)



    override fun observeFavorites(): Flow<List<PersonProfile>> =

        savedContactDao.observeAll().map { entities ->

            entities.map { it.toProfile() }

        }



    override suspend fun upsertNode(node: RadarNode) {

        val now = System.currentTimeMillis()

        val existing = radarDao.getById(node.id)

        val isFavorite = existing?.isFavorite == true || savedContactDao.exists(node.id)

        val entity = node.toEntity(

            isFavorite = isFavorite,

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


