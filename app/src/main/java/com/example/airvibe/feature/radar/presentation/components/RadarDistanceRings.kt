package com.example.airvibe.feature.radar.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp
import kotlin.math.min

/** Etiquetas de distancia aproximada (metros) sobre los anillos del radar. */
@Composable
fun RadarDistanceRings(
    modifier: Modifier = Modifier,
    ringColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
) {
    val labels = listOf("~3 m", "~6 m", "~9 m", "~12 m")
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = min(size.width, size.height) * 0.46f
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(170, 79, 70, 229)
            textSize = 10.sp.toPx()
            isAntiAlias = true
        }
        labels.forEachIndexed { index, label ->
            val fraction = (index + 1) / labels.size.toFloat()
            val radius = maxR * fraction
            drawCircle(
                color = ringColor,
                radius = radius,
                center = center,
            )
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    label,
                    center.x + 6f,
                    center.y - radius + 12f,
                    paint,
                )
            }
        }
    }
}

/** Convierte distancia normalizada del radar a metros aproximados. */
fun proximityMeters(distanceNormalized: Float): Int {
    val clamped = distanceNormalized.coerceIn(0f, 1f)
    return ((1f - clamped) * 12f + 1.5f).toInt()
}
