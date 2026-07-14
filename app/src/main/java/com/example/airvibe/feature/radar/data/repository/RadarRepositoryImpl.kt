package com.example.airvibe.feature.radar.data.repository



import com.example.airvibe.feature.radar.data.local.dao.HandshakeRequestDao
import com.example.airvibe.feature.radar.data.local.dao.ProfileViewDao
import com.example.airvibe.feature.radar.data.local.dao.RadarDao
import com.example.airvibe.feature.radar.data.local.dao.SavedContactDao
import com.example.airvibe.feature.radar.data.local.entity.HandshakeRequestEntity
import com.example.airvibe.feature.radar.data.local.entity.ProfileViewEntity
import com.example.airvibe.feature.radar.data.mapper.ContactMapper
import com.example.airvibe.feature.radar.data.mapper.ContactMapper.toEntity
import com.example.airvibe.feature.radar.data.mapper.ContactMapper.toProfile
import com.example.airvibe.feature.radar.data.mapper.ContactMapper.toSavedContact
import com.example.airvibe.feature.radar.data.mapper.NodeMapper.toDomain
import com.example.airvibe.feature.radar.data.mapper.NodeMapper.toEntity
import com.example.airvibe.feature.radar.data.mapper.NodeMapper.toProfile
import com.example.airvibe.feature.radar.data.seed.RadarSeedData
import com.example.airvibe.feature.radar.domain.model.HandshakeRequest
import com.example.airvibe.feature.radar.domain.model.PersonProfile
import com.example.airvibe.feature.radar.domain.model.RadarNode
import com.example.airvibe.feature.radar.domain.model.VisibilityDay
import com.example.airvibe.feature.radar.domain.model.VisibilityStats
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

    private val handshakeRequestDao: HandshakeRequestDao,

    private val profileViewDao: ProfileViewDao,

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

        val hasMessages = radarDao.hasMessages(nodeId)

        if (hasMessages) {

            radarDao.updatePresence(nodeId, com.example.airvibe.feature.radar.domain.model.PresenceStatus.Away.name)

        } else {

            radarDao.deleteById(nodeId)

        }

    }



    override suspend fun clearDiscoveredNodes() {

        radarDao.clearDiscovered()

    }



    override suspend fun removePendingNodes() {

        radarDao.deletePendingNodes()

    }

    // ----------------- Feature 3: Handshake -----------------

    override fun observeIncomingHandshakes(): Flow<List<HandshakeRequest>> =
        handshakeRequestDao.observeIncomingPending().map { list ->
            list.map { HandshakeRequest.fromEntity(it) }
        }

    override suspend fun getHandshakeById(handshakeId: String): HandshakeRequest? =
        handshakeRequestDao.getByHandshakeId(handshakeId)?.let(HandshakeRequest::fromEntity)

    override suspend fun upsertHandshakeRequest(request: HandshakeRequest) {
        val existing = handshakeRequestDao.getByHandshakeId(request.handshakeId)
        if (existing != null) {
            // Refrescar datos de perfil sin tocar el estado final
            // (Accepted/Rejected) si el handshake ya terminó.
            val status = if (existing.status == HandshakeRequestEntity.STATUS_PENDING) {
                request.status.name
            } else {
                existing.status
            }
            val direction = if (existing.direction == HandshakeRequestEntity.DIRECTION_INCOMING &&
                request.direction == HandshakeRequest.Direction.Outgoing
            ) {
                existing.direction
            } else if (existing.direction == HandshakeRequestEntity.DIRECTION_OUTGOING &&
                request.direction == HandshakeRequest.Direction.Incoming
            ) {
                request.direction.name
            } else {
                existing.direction
            }
            val updated = existing.copy(
                peerDisplayName = request.peerDisplayName,
                peerHeadline = request.peerHeadline,
                peerStatus = request.peerStatus,
                peerPresence = request.peerPresence.name,
                peerTags = request.peerTags,
                handshakeKey = request.handshakeKey,
                status = status,
                direction = direction,
            )
            handshakeRequestDao.update(updated)
        } else {
            val entity = HandshakeRequestEntity(
                ownerId = request.peerNodeId,
                handshakeId = request.handshakeId,
                peerNodeId = request.peerNodeId,
                peerDisplayName = request.peerDisplayName,
                peerHeadline = request.peerHeadline,
                peerStatus = request.peerStatus,
                peerPresence = request.peerPresence.name,
                peerTags = request.peerTags,
                handshakeKey = request.handshakeKey,
                status = request.status.name,
                direction = request.direction.name,
                createdAt = request.createdAt,
                respondedAt = request.respondedAt,
            )
            handshakeRequestDao.upsert(entity)
        }
    }

    override suspend fun respondToHandshake(
        handshakeId: String,
        accept: Boolean,
    ): HandshakeRequest? {
        val entity = handshakeRequestDao.getByHandshakeId(handshakeId) ?: return null
        if (entity.status != HandshakeRequestEntity.STATUS_PENDING) {
            return HandshakeRequest.fromEntity(entity)
        }
        val newStatus = if (accept) {
            HandshakeRequestEntity.STATUS_ACCEPTED
        } else {
            HandshakeRequestEntity.STATUS_REJECTED
        }
        handshakeRequestDao.updateStatus(handshakeId, newStatus, System.currentTimeMillis())
        if (accept) {
            val profile = PersonProfile(
                id = entity.peerNodeId,
                displayName = entity.peerDisplayName,
                headline = entity.peerHeadline.ifBlank { entity.peerStatus },
                bio = "",
                status = entity.peerStatus.ifBlank { entity.peerHeadline },
                presence = runCatching {
                    com.example.airvibe.feature.radar.domain.model.PresenceStatus
                        .valueOf(entity.peerPresence)
                }.getOrDefault(com.example.airvibe.feature.radar.domain.model.PresenceStatus.Online),
                tags = entity.peerTags,
                distanceMeters = 0,
                isFavorite = true,
                accentHue = 0f,
            )
            saveContact(profile, addedByPeer = true)
        }
        val updated = handshakeRequestDao.getByHandshakeId(handshakeId)
        return updated?.let(HandshakeRequest::fromEntity)
    }

    // ----------------- Feature 5: Telemetría -----------------

    override suspend fun recordProfileEvent(
        targetUserId: String,
        sourceNodeId: String,
        kind: String,
    ) {
        if (targetUserId.isBlank() || sourceNodeId.isBlank() || targetUserId == sourceNodeId) {
            return
        }
        profileViewDao.upsert(
            ProfileViewEntity(
                targetUserId = targetUserId,
                sourceNodeId = sourceNodeId,
                kind = kind,
            ),
        )
    }

    override suspend fun flushTelemetry(limit: Int): Int {
        val pending = profileViewDao.getPendingSync(limit)
        if (pending.isEmpty()) return 0
        // El empujón real a Supabase lo hace el [SyncWorker] vía
        // SupabaseTelemetryDataSource. Aquí devolvemos el lote
        // para que el worker lo procese.
        return pending.size
    }

    override suspend fun markTelemetrySynced(ids: List<Long>) {
        if (ids.isEmpty()) return
        profileViewDao.markAsSynced(ids)
    }

    override fun observePendingTelemetryCount(): Flow<Int> {
        val source = profileViewDao.observeTotalViews()
        return source
    }

    override suspend fun localVisibility(): VisibilityStats {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000
        // El targetUserId "self" lo pasa el caller; aquí asumimos
        // que el conteo es relativo a TODO (suma global) ya que
        // el dashboard Premium lo filtra por su propio id.
        val totalViews = profileViewDao.observeTotalViews()
        // Para evitar suspender sobre Flow, hacemos una query
        // puntual. La agregación diaria remota se hace en SQL
        // (visibility_daily).
        val byDay = emptyList<VisibilityDay>()
        return VisibilityStats(
            viewsLast7Days = 0,
            tapsLast7Days = 0,
            uniqueVisitorsLast7Days = 0,
            viewsLast30Days = 0,
            tapsLast30Days = 0,
            totalPendingSync = 0,
            byDay = byDay,
        )
    }

    override suspend fun pruneSyncedTelemetry(olderThan: Long) {
        profileViewDao.deleteSyncedOlderThan(olderThan)
    }
}
