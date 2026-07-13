package com.example.airvibe.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun WaveHeader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    brush: Brush? = null,
    waveHeight: Dp = 420.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(waveHeight)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height

            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(width, 0f)
                lineTo(width, height * 0.7f) // Bajar el inicio en la derecha
                cubicTo(
                    x1 = width * 0.75f, y1 = height * 1.2f, // Punto de control 1 más profundo
                    x2 = width * 0.2f, y2 = height * 0.4f, // Punto de control 2 
                    x3 = 0f, y3 = height * 0.95f // Bajar el fin en la izquierda
                )
                close()
            }
            
            clipPath(path) {
                if (brush != null) {
                    drawRect(brush = brush, size = size)
                } else {
                    drawRect(color = color, size = size)
                }

                // Dibujar patrón de figuras (tipo curvas topográficas)
                val patternPath = Path().apply {
                    var yOffset = -height * 0.2f
                    while (yOffset < height * 1.5f) {
                        moveTo(-width * 0.2f, yOffset)
                        cubicTo(
                            width * 0.3f, yOffset - height * 0.3f,
                            width * 0.7f, yOffset + height * 0.4f,
                            width * 1.2f, yOffset - height * 0.1f
                        )
                        yOffset += height * 0.12f
                    }
                    var xOffset = -width * 0.5f
                    while (xOffset < width * 1.5f) {
                        moveTo(xOffset, -height * 0.2f)
                        cubicTo(
                            xOffset + width * 0.3f, height * 0.2f,
                            xOffset - width * 0.2f, height * 0.6f,
                            xOffset + width * 0.4f, height * 1.2f
                        )
                        xOffset += width * 0.2f
                    }
                }
                drawPath(
                    path = patternPath,
                    color = Color.White.copy(alpha = 0.12f),
                    style = Stroke(width = 4f)
                )
            }
        }
        content()
    }
}
