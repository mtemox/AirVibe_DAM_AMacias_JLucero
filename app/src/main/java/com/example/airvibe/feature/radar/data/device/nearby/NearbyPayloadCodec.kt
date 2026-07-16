package com.example.airvibe.feature.radar.data.device.nearby

import com.example.airvibe.feature.radar.domain.model.PresenceStatus
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile
import com.example.airvibe.feature.radar.domain.scanner.toNodeKindOrDefault

/**
 * Codificador / decodificador del payload que AirVibe intercambia
 * mediante Nearby Connections. El formato elegido es CSV con un
 * delimitador poco común (`\u001F`) para minimizar el tamaño y
 * evitar colisiones con el contenido del campo `status` o `name`.
 *
 * Layout del payload (v3):
 *
 *     v3|<id>|<displayName>|<status>|<kind>|<presence>|<headline>|<bio>|<isPremium>|<catalog>|<tag1\u001Ftag2\u001F…>
 *
 * El prefijo de versión permite compatibilidad hacia atrás:
 *  - v1: solo `id, displayName, status, kind, tags` (presence = Online)
 *  - v2: añade `presence` antes de tags
 *  - v3: añade `headline, bio, isPremium, catalog` (Payload
 *    extendido de usuarios Premium). El catálogo se omite del
 *    payload cuando el usuario NO es Premium.
 *
 * Los clientes que reciban un payload v1/v2 no mostrarán el badge
 * Premium ni el catálogo, pero seguirán interoperando sin errores.
 *
 * El límite de Nearby Connections para `BYTES` es de ~32 KB por
 * payload, suficiente para textos de bio + catálogo de hasta ~30 KB
 * sin riesgo de recorte.
 */
internal object NearbyPayloadCodec {

    private const val SCHEMA_VERSION = "v5"
    private const val LEGACY_V4 = "v4"
    private const val LEGACY_V3 = "v3"
    private const val LEGACY_V2 = "v2"
    private const val LEGACY_V1 = "v1"
    private const val DELIMITER = "\u001F"
    private const val FIELD_SEPARATOR = "|"

    /** Límite conservador del catálogo para evitar payloads gigantes. */
    private const val MAX_CATALOG_BYTES = 8 * 1024

    /**
     * Serializa un [ScannerProfile] a bytes UTF-8 (esquema v5).
     * Si el usuario no es Premium, el campo `catalog` se omite para
     * ahorrar ancho de banda.
     *
     * v5 añade `authUserId` al final para telemetría de visibilidad
     * Premium (permite que los eventos se registren con el UUID real
     * de Supabase en lugar del device-id).
     */
    fun encode(profile: ScannerProfile): ByteArray {
        val tags = profile.tags.joinToString(DELIMITER)
        val safeHeadline = sanitize(profile.headline)
        val safeBio = sanitize(profile.bio)
        val catalog = if (profile.isPremium) {
            sanitize(profile.premiumCatalog.orEmpty()).take(MAX_CATALOG_BYTES)
        } else {
            ""
        }
        val safeAvatarUrl = sanitize(profile.avatarUrl.orEmpty())
        val safeAvatarBase64 = sanitize(profile.avatarBase64.orEmpty())
        val safeAuthUserId = profile.authUserId.orEmpty()
        
        val payload = listOf(
            SCHEMA_VERSION,
            profile.id,
            profile.displayName,
            profile.status,
            profile.kind.name,
            profile.presence.name,
            safeHeadline,
            safeBio,
            if (profile.isPremium) "1" else "0",
            catalog,
            safeAvatarUrl,
            safeAvatarBase64,
            tags,
            safeAuthUserId,
        ).joinToString(FIELD_SEPARATOR)
        return payload.toByteArray(Charsets.UTF_8)
    }

    /**
     * Deserializa un payload recibido. Acepta v1, v2, v3, v4 y v5. Devuelve
     * `null` si el contenido no respeta ninguno de los esquemas
     * conocidos.
     */
    fun decode(payload: ByteArray): ScannerProfile? {
        val text = runCatching { String(payload, Charsets.UTF_8) }.getOrNull() ?: return null
        val parts = text.split(FIELD_SEPARATOR)
        if (parts.size < 5) return null
        return when (parts[0]) {
            SCHEMA_VERSION -> decodeV5(parts)
            LEGACY_V4 -> decodeV4(parts)
            LEGACY_V3 -> decodeV3(parts)
            LEGACY_V2 -> decodeV2(parts)
            LEGACY_V1 -> decodeV1(parts)
            else -> null
        }
    }

    private fun decodeV5(parts: List<String>): ScannerProfile? {
        if (parts.size < 14) return null
        val isPremium = parts[8] == "1"
        val catalog = parts[9].takeIf { it.isNotEmpty() && isPremium }
        val avatarUrl = parts[10].takeIf { it.isNotEmpty() }
        val avatarBase64 = parts[11].takeIf { it.isNotEmpty() }
        val tags = if (parts[12].isEmpty()) emptyList() else parts[12].split(DELIMITER)
        val authUserId = parts[13].takeIf { it.isNotEmpty() }
        return ScannerProfile(
            id = parts[1],
            displayName = parts[2],
            status = parts[3],
            kind = parts[4].toNodeKindOrDefault(),
            presence = runCatching { PresenceStatus.valueOf(parts[5]) }
                .getOrDefault(PresenceStatus.Online),
            headline = parts[6].unsanitize(),
            bio = parts[7].unsanitize(),
            isPremium = isPremium,
            premiumCatalog = catalog?.unsanitize(),
            avatarUrl = avatarUrl?.unsanitize(),
            avatarBase64 = avatarBase64?.unsanitize(),
            tags = tags,
            authUserId = authUserId,
        )
    }

    private fun decodeV4(parts: List<String>): ScannerProfile? {
        if (parts.size < 13) return null
        val isPremium = parts[8] == "1"
        val catalog = parts[9].takeIf { it.isNotEmpty() && isPremium }
        val avatarUrl = parts[10].takeIf { it.isNotEmpty() }
        val avatarBase64 = parts[11].takeIf { it.isNotEmpty() }
        val tags = if (parts[12].isEmpty()) emptyList() else parts[12].split(DELIMITER)
        return ScannerProfile(
            id = parts[1],
            displayName = parts[2],
            status = parts[3],
            kind = parts[4].toNodeKindOrDefault(),
            presence = runCatching { PresenceStatus.valueOf(parts[5]) }
                .getOrDefault(PresenceStatus.Online),
            headline = parts[6].unsanitize(),
            bio = parts[7].unsanitize(),
            isPremium = isPremium,
            premiumCatalog = catalog?.unsanitize(),
            avatarUrl = avatarUrl?.unsanitize(),
            avatarBase64 = avatarBase64?.unsanitize(),
            tags = tags,
            authUserId = null,
        )
    }

    private fun decodeV3(parts: List<String>): ScannerProfile? {
        if (parts.size < 11) return null
        val isPremium = parts[8] == "1"
        val catalog = parts[9].takeIf { it.isNotEmpty() && isPremium }
        val tags = if (parts[10].isEmpty()) emptyList() else parts[10].split(DELIMITER)
        return ScannerProfile(
            id = parts[1],
            displayName = parts[2],
            status = parts[3],
            kind = parts[4].toNodeKindOrDefault(),
            presence = runCatching { PresenceStatus.valueOf(parts[5]) }
                .getOrDefault(PresenceStatus.Online),
            headline = parts[6].unsanitize(),
            bio = parts[7].unsanitize(),
            isPremium = isPremium,
            premiumCatalog = catalog?.unsanitize(),
            tags = tags,
        )
    }

    private fun decodeV2(parts: List<String>): ScannerProfile? {
        if (parts.size < 6) return null
        val tags = if (parts[6].isEmpty()) emptyList() else parts[6].split(DELIMITER)
        return ScannerProfile(
            id = parts[1],
            displayName = parts[2],
            status = parts[3],
            kind = parts[4].toNodeKindOrDefault(),
            presence = runCatching { PresenceStatus.valueOf(parts[5]) }
                .getOrDefault(PresenceStatus.Online),
            tags = tags,
        )
    }

    private fun decodeV1(parts: List<String>): ScannerProfile? {
        val tags = if (parts.size >= 6 && parts[5].isNotEmpty()) parts[5].split(DELIMITER) else emptyList()
        return ScannerProfile(
            id = parts[1],
            displayName = parts[2],
            status = parts[3],
            kind = parts[4].toNodeKindOrDefault(),
            presence = PresenceStatus.Online,
            tags = tags,
        )
    }

    /** Reemplaza separadores y saltos de línea que romperían el codec. */
    private fun sanitize(text: String): String = text
        .replace(FIELD_SEPARATOR, " ")
        .replace(DELIMITER, " ")
        .replace("\n", " ")
        .replace("\r", " ")

    /** Marca tipográfica para volver a un texto legible en el cliente. */
    private fun String.unsanitize(): String = replace(FIELD_SEPARATOR, " ")
}
