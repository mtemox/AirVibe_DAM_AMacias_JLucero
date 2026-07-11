package com.example.airvibe.feature.radar.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.airvibe.feature.radar.data.device.identity.DeviceIdentityProvider
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

    override suspend fun applyAuthDisplayName(displayName: String?) {
        if (prefs.getBoolean(KEY_NAME_CUSTOMIZED, false)) return
        val name = displayName?.trim().orEmpty()
        if (name.isEmpty()) return
        prefs.edit().putString(KEY_DISPLAY_NAME, name).apply()
        state.value = readProfile()
    }

    private fun readProfile(): ScannerProfile = ScannerProfile(
        id = deviceIdentity.deviceId(),
        displayName = prefs.getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME) ?: DEFAULT_DISPLAY_NAME,
        status = prefs.getString(KEY_STATUS, DEFAULT_STATUS) ?: DEFAULT_STATUS,
        kind = RadarNodeKind.Person,
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

    companion object {
        private const val PREFS_NAME = "airvibe.scanner_profile"
        private const val KEY_DISPLAY_NAME = "profile.displayName"
        private const val KEY_STATUS = "profile.status"
        private const val KEY_TAGS = "profile.tags"
        private const val KEY_NAME_CUSTOMIZED = "profile.nameCustomized"
        private const val DEFAULT_DISPLAY_NAME = "Tú"
        private const val DEFAULT_STATUS = "Disponible en el radar"
    }
}
