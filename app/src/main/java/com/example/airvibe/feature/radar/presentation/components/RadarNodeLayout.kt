package com.example.airvibe.feature.radar.presentation.components

import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import com.example.airvibe.feature.radar.domain.model.RadarNode
import kotlin.math.cos
import kotlin.math.sin

/** Convierte posición polar del nodo a [Alignment] dentro del radar. */
fun radarAlignmentFor(node: RadarNode): Alignment {
    val radial = node.distanceNormalized.coerceIn(0f, 1f).coerceAtLeast(0.28f) * 0.82f
    val radians = Math.toRadians(node.angleDegrees.toDouble() - 90.0)
    return BiasAlignment(
        horizontalBias = (cos(radians) * radial).toFloat(),
        verticalBias = (sin(radians) * radial).toFloat(),
    )
}
