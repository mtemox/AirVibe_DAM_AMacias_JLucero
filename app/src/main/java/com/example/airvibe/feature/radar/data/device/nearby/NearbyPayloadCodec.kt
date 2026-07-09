package com.example.airvibe.feature.radar.data.device.nearby

import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile
import com.example.airvibe.feature.radar.domain.scanner.toNodeKindOrDefault

/**
 * Codificador / decodificador del payload que AirVibe intercambia
 * mediante Nearby Connections. El formato elegido es CSV con un
 * delimitador poco común (`\u001F`) para minimizar el tamaño y
 * evitar colisiones con el contenido del campo `status` o `name`.
 *
 * Layout del payload:
 *
 *     v1|<id>|<displayName>|<status>|<kind>|<tag1\u001Ftag2\u001F…>
 *
 * El prefijo `v1` permite versionar el esquema en el futuro sin
 * romper a clientes antiguos.
 */
internal object NearbyPayloadCodec {

    private const val SCHEMA_VERSION = "v1"
    private const val DELIMITER = "\u001F"
    private const val FIELD_SEPARATOR = "|"

    /** Serializa un [ScannerProfile] a bytes UTF-8. */
    fun encode(profile: ScannerProfile): ByteArray {
        val tags = profile.tags.joinToString(DELIMITER)
        val payload = listOf(
            SCHEMA_VERSION,
            profile.id,
            profile.displayName,
            profile.status,
            profile.kind.name,
            tags,
        ).joinToString(FIELD_SEPARATOR)
        return payload.toByteArray(Charsets.UTF_8)
    }

    /**
     * Deserializa un payload recibido. Devuelve `null` si el
     * contenido no respeta el esquema, evitando crashes cuando un
     * peer externo envía basura.
     */
    fun decode(payload: ByteArray): ScannerProfile? {
        val text = runCatching { String(payload, Charsets.UTF_8) }.getOrNull() ?: return null
        val parts = text.split(FIELD_SEPARATOR)
        if (parts.size < 5) return null
        if (parts[0] != SCHEMA_VERSION) return null
        val tags = if (parts[5].isEmpty()) emptyList() else parts[5].split(DELIMITER)
        return ScannerProfile(
            id = parts[1],
            displayName = parts[2],
            status = parts[3],
            kind = parts[4].toNodeKindOrDefault(),
            tags = tags,
        )
    }
}
