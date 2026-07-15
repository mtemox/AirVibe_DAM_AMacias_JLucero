package com.example.airvibe.feature.radar.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.airvibe.feature.radar.data.device.identity.DeviceIdentityProvider
import com.example.airvibe.feature.radar.data.remote.RemoteProfileDto
import com.example.airvibe.feature.radar.data.remote.SupabaseProfileDataSource
import com.example.airvibe.feature.radar.domain.model.PresenceStatus
import com.example.airvibe.feature.radar.domain.model.RadarNodeKind
import com.example.airvibe.feature.radar.domain.repository.ScannerProfileRepository
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class ScannerProfileRepositoryImpl(
    context: Context,
    private val deviceIdentity: DeviceIdentityProvider,
    private val profileRemote: SupabaseProfileDataSource? = null,
    private val currentAuthUserId: () -> String? = { null },
) : ScannerProfileRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val state = MutableStateFlow(readProfile())

    override fun observe(): Flow<ScannerProfile> = state.asStateFlow()

    override fun current(): ScannerProfile = state.value

    override suspend fun update(
        displayName: String,
        status: String,
        tags: List<String>,
    ) {
        val trimmedName = displayName.trim()
        val trimmedStatus = status.trim()
        require(trimmedName.isNotEmpty()) { "displayName cannot be blank" }
        require(trimmedStatus.isNotEmpty()) { "status cannot be blank" }

        prefs.edit().apply {
            putString(KEY_DISPLAY_NAME, trimmedName)
            putString(KEY_STATUS, trimmedStatus)
            putString(KEY_TAGS, encodeTags(tags))
            putBoolean(KEY_NAME_CUSTOMIZED, true)
            apply()
        }
        state.value = readProfile()
    }

    override suspend fun updateKind(kind: RadarNodeKind) {
        prefs.edit().putString(KEY_KIND, kind.name).apply()
        state.value = readProfile()
    }

    override suspend fun updatePresence(presence: PresenceStatus) {
        prefs.edit().putString(KEY_PRESENCE, presence.name).apply()
        state.value = readProfile()
    }

    override suspend fun updateHeadline(headline: String) {
        prefs.edit().putString(KEY_HEADLINE, headline.trim()).apply()
        state.value = readProfile()
    }

    override suspend fun updateBio(bio: String) {
        prefs.edit().putString(KEY_BIO, bio.trim()).apply()
        state.value = readProfile()
    }

    override suspend fun updatePremium(isPremium: Boolean, catalog: String?) {
        prefs.edit()
            .putBoolean(KEY_IS_PREMIUM, isPremium)
            .apply()
        if (catalog != null) {
            prefs.edit()
                .putString(KEY_PREMIUM_CATALOG, catalog.trim().take(MAX_CATALOG_BYTES))
                .apply()
        }
        state.value = readProfile()
    }

    override suspend fun updateAvatar(avatarUrl: String?, avatarBase64: String?) {
        prefs.edit().apply {
            if (avatarUrl != null) putString(KEY_AVATAR_URL, avatarUrl) else remove(KEY_AVATAR_URL)
            if (avatarBase64 != null) putString(KEY_AVATAR_BASE64, avatarBase64) else remove(KEY_AVATAR_BASE64)
            apply()
        }
        state.value = readProfile()
    }

    override suspend fun applyAuthDisplayName(displayName: String?) {
        if (prefs.getBoolean(KEY_NAME_CUSTOMIZED, false)) return
        val name = displayName?.trim().orEmpty()
        if (name.isEmpty()) return
        prefs.edit().putString(KEY_DISPLAY_NAME, name).apply()
        state.value = readProfile()
    }

    override suspend fun syncToRemote(profile: ScannerProfile) {
        val remote = profileRemote ?: return
        val userId = currentAuthUserId() ?: return
        remote.upsert(
            RemoteProfileDto(
                id = userId,
                fullName = profile.displayName,
                status = profile.status,
                headline = profile.headline,
                profession = profile.headline,
                bio = profile.bio,
                isPremium = profile.isPremium,
                premiumCatalog = profile.premiumCatalog,
                tags = profile.tags,
                avatarUrl = profile.avatarUrl,
                avatarBase64 = profile.avatarBase64,
            ),
        )
    }

    override suspend fun restoreFromRemote(userId: String) {
        val remote = profileRemote ?: return
        val dto = remote.fetch(userId).getOrNull() ?: return
        prefs.edit().apply {
            putString(KEY_DISPLAY_NAME, dto.fullName)
            dto.status?.takeIf { it.isNotBlank() }?.let { putString(KEY_STATUS, it) }
            if (dto.headline.isNotBlank()) putString(KEY_HEADLINE, dto.headline)
            dto.profession?.takeIf { it.isNotBlank() && dto.headline.isBlank() }
                ?.let { putString(KEY_HEADLINE, it) }
            if (dto.bio.isNotBlank()) putString(KEY_BIO, dto.bio)
            putBoolean(KEY_IS_PREMIUM, dto.isPremium)
            dto.premiumCatalog?.takeIf { it.isNotBlank() }
                ?.let { putString(KEY_PREMIUM_CATALOG, it) }
            dto.avatarUrl?.takeIf { it.isNotBlank() }
                ?.let { putString(KEY_AVATAR_URL, it) }
            dto.avatarBase64?.takeIf { it.isNotBlank() }
                ?.let { putString(KEY_AVATAR_BASE64, it) }
            if (dto.tags.isNotEmpty()) putString(KEY_TAGS, encodeTags(dto.tags))
            putBoolean(KEY_NAME_CUSTOMIZED, true)
            apply()
        }
        state.value = readProfile()
    }

    private fun readProfile(): ScannerProfile = ScannerProfile(
        id = deviceIdentity.deviceId(),
        displayName = prefs.getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME) ?: DEFAULT_DISPLAY_NAME,
        status = prefs.getString(KEY_STATUS, DEFAULT_STATUS) ?: DEFAULT_STATUS,
        kind = prefs.getString(KEY_KIND, null)?.let { decodeKind(it) } ?: RadarNodeKind.Person,
        presence = prefs.getString(KEY_PRESENCE, null)?.let { decodePresence(it) } ?: PresenceStatus.Online,
        headline = prefs.getString(KEY_HEADLINE, "").orEmpty(),
        bio = prefs.getString(KEY_BIO, "").orEmpty(),
        isPremium = prefs.getBoolean(KEY_IS_PREMIUM, false),
        premiumCatalog = prefs.getString(KEY_PREMIUM_CATALOG, null)?.takeIf { it.isNotBlank() },
        avatarUrl = prefs.getString(KEY_AVATAR_URL, null)?.takeIf { it.isNotBlank() },
        avatarBase64 = prefs.getString(KEY_AVATAR_BASE64, null)?.takeIf { it.isNotBlank() },
        tags = decodeTags(prefs.getString(KEY_TAGS, null)),
    )

    private fun encodeTags(tags: List<String>): String {
        val array = JSONArray()
        tags.map { it.trim() }.filter { it.isNotEmpty() }.forEach { array.put(it) }
        return array.toString()
    }

    private fun decodeTags(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return listOf("AirVibe")
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val tag = array.optString(i).trim()
                    if (tag.isNotEmpty()) add(tag)
                }
            }
        }.getOrDefault(listOf("AirVibe"))
    }

    private fun decodeKind(raw: String): RadarNodeKind =
        runCatching { RadarNodeKind.valueOf(raw) }.getOrDefault(RadarNodeKind.Person)

    private fun decodePresence(raw: String): PresenceStatus =
        runCatching { PresenceStatus.valueOf(raw) }.getOrDefault(PresenceStatus.Online)

    companion object {
        private const val PREFS_NAME = "airvibe.scanner_profile"
        private const val KEY_DISPLAY_NAME = "profile.displayName"
        private const val KEY_STATUS = "profile.status"
        private const val KEY_TAGS = "profile.tags"
        private const val KEY_KIND = "profile.kind"
        private const val KEY_PRESENCE = "profile.presence"
        private const val KEY_HEADLINE = "profile.headline"
        private const val KEY_BIO = "profile.bio"
        private const val KEY_IS_PREMIUM = "profile.isPremium"
        private const val KEY_PREMIUM_CATALOG = "profile.premiumCatalog"
        private const val KEY_AVATAR_URL = "profile.avatarUrl"
        private const val KEY_AVATAR_BASE64 = "profile.avatarBase64"
        private const val KEY_NAME_CUSTOMIZED = "profile.nameCustomized"
        private const val DEFAULT_DISPLAY_NAME = "Tú"
        private const val DEFAULT_STATUS = "Disponible en el radar"
        private const val MAX_CATALOG_BYTES = 8 * 1024
    }
}
