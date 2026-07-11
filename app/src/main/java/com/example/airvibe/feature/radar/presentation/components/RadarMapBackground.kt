package com.example.airvibe.feature.radar.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min

/**
 * Fondo estilo mapa (inspirado en tiles OSM/Leaflet) dibujado
 * nativamente en Compose. No usa GPS: es una referencia visual
 * para leer la proximidad en el radar polar.
 */
@Composable
fun RadarMapBackground(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val mapBase = Color(0xFFE8E4D9)
        val park = Color(0xFFC8E6C9)
        val road = Color(0xFFFFFFFF)
        val roadEdge = Color(0xFFD0D0D0)
        val block = Color(0xFFF2EFE8)

        drawRect(color = mapBase)

        val blockW = size.width / 6f
        val blockH = size.height / 8f
        for (row in 0 until 8) {
            for (col in 0 until 6) {
                if ((row + col) % 3 == 0) continue
                drawRect(
                    color = block,
                    topLeft = Offset(col * blockW + 4f, row * blockH + 4f),
                    size = Size(blockW - 8f, blockH - 8f),
                )
            }
        }

        drawRect(
            color = park,
            topLeft = Offset(size.width * 0.55f, size.height * 0.12f),
            size = Size(size.width * 0.28f, size.height * 0.22f),
        )
        drawRect(
            color = park.copy(alpha = 0.85f),
            topLeft = Offset(size.width * 0.08f, size.height * 0.62f),
            size = Size(size.width * 0.22f, size.height * 0.18f),
        )

        val roadStroke = 10f
        for (i in 1 until 6) {
            val x = i * blockW
            drawLine(roadEdge, Offset(x, 0f), Offset(x, size.height), strokeWidth = roadStroke + 2f)
            drawLine(road, Offset(x, 0f), Offset(x, size.height), strokeWidth = roadStroke)
        }
        for (i in 1 until 8) {
            val y = i * blockH
            drawLine(roadEdge, Offset(0f, y), Offset(size.width, y), strokeWidth = roadStroke + 2f)
            drawLine(road, Offset(0f, y), Offset(size.width, y), strokeWidth = roadStroke)
        }

        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = min(size.width, size.height) * 0.46f
        listOf(0.33f, 0.66f, 1f).forEach { fraction ->
            drawCircle(
                color = Color(0xFF6366F1).copy(alpha = 0.12f),
                radius = maxR * fraction,
                center = center,
                style = Stroke(width = 1.5f),
            )
        }
    }
}
