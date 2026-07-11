package com.example.airvibe.feature.radar.data.device.nearby

import com.example.airvibe.feature.radar.domain.scanner.DistanceLevel
import kotlin.math.abs

/**
 * Estima la proximidad de un endpoint cuando Nearby Connections
 * no expone RSSI directo. Usa el orden de descubrimiento y una
 * variación estable por [endpointId] para distribuir nodos en el
 * radar de forma creíble durante la demo.
 */
internal object ProximityDistanceEstimator {

    private val discoveryOrder = LinkedHashMap<String, Int>()
    private var orderCounter = 0

    fun onEndpointFound(endpointId: String): DistanceLevel {
        if (!discoveryOrder.containsKey(endpointId)) {
            discoveryOrder[endpointId] = orderCounter++
        }
        return estimate(endpointId)
    }

    fun onEndpointLost(endpointId: String) {
        discoveryOrder.remove(endpointId)
    }

    fun reset() {
        discoveryOrder.clear()
        orderCounter = 0
    }

    fun estimate(endpointId: String): DistanceLevel {
        val order = discoveryOrder[endpointId] ?: orderCounter
        val jitter = stableJitter(endpointId)
        val score = (order * 0.14f) + jitter

        return when {
            score < 0.20f -> DistanceLevel.VERY_CLOSE
            score < 0.38f -> DistanceLevel.CLOSE
            score < 0.55f -> DistanceLevel.NEAR
            score < 0.72f -> DistanceLevel.FAR
            else -> DistanceLevel.VERY_FAR
        }
    }

    private fun stableJitter(endpointId: String): Float {
        val hash = abs(endpointId.hashCode() % 100) / 100f
        return hash * 0.22f
    }
}
