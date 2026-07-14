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

/**
 * Etiquetas de distancia aproximada (metros) sobre los anillos del
 * radar. Se muestran en el rango nominal de Bluetooth de corto
 * alcance: 5 m (muy cerca) → 80 m (borde del alcance). Esto
 * permite al usuario hacerse una idea espacial real del entorno
 * físico, no una escala de "laboratorio".
 */
private val RING_LABELS = listOf("~5 m", "~20 m", "~50 m", "~80 m")

/** Convierte distancia normalizada del radar a metros aproximados. */
fun proximityMeters(distanceNormalized: Float): Int {
    val clamped = distanceNormalized.coerceIn(0f, 1f)
    // 0.0 (centro) ≈ 3 m · 1.0 (borde) ≈ 90 m. Modelo lineal
    // consistente con la escala 5–80 m de los anillos visibles.
    return ((1f - clamped) * 87f + 3f).toInt()
}

@Composable
fun RadarDistanceRings(
    modifier: Modifier = Modifier,
    ringColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
) {
    val labels = RING_LABELS
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
