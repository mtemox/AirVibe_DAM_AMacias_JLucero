package com.example.airvibe.feature.radar.domain.scanner

import com.example.airvibe.feature.radar.domain.model.RadarNodeKind

/**
 * Perfil ligero que el usuario **anuncia** a los demás dispositivos
 * mediante Nearby Connections. Se serializa a bytes para transmitirse
 * como payload de descubrimiento.
 *
 * Mantenemos un tamaño reducido (apenas los campos visibles en la
 * preview) para respetar el límite de Nearby y minimizar el consumo
 * de batería durante el escaneo.
 */
data class ScannerProfile(
    val id: String,
    val displayName: String,
    val status: String,
    val kind: RadarNodeKind,
    val tags: List<String> = emptyList(),
)
