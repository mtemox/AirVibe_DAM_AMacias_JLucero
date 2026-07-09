package com.example.airvibe.core.designsystem.modifiers

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Aplica un blur nativo a través de [graphicsLayer] + [BlurEffect].
 * Solo funciona en API 31+. En versiones anteriores se ignora silenciosamente
 * para conservar la legibilidad del contenido.
 */
fun Modifier.glassBlur(
    radius: Dp = 24.dp,
    shape: Shape = RoundedCornerShape(24.dp),
    enabled: Boolean = true,
): Modifier {
    // En Compose, BlurEffect difumina el componente y sus hijos (el texto y los campos), no el fondo.
    // Para evitar que la pantalla se vea borrosa, deshabilitamos el BlurEffect.
    // El efecto "glass" se mantendrá gracias a los fondos semi-transparentes y bordes.
    return this.clip(shape)
}

/**
 * Borde con highlight superior tipo "liquid glass". Pinta un trazo fino
 * con gradiente vertical que simula la luz incidiendo sobre una superficie
 * esmerilada.
 */
fun Modifier.glassHighlightBorder(
    color: Color,
    strokeWidth: Dp = 1.dp,
    cornerRadius: Dp = 24.dp,
): Modifier = drawBehind {
    val stroke = strokeWidth.toPx()
    val radiusPx = cornerRadius.toPx()
    val inset = stroke / 2f
    val rect = Rect(
        offset = Offset(inset, inset),
        size = Size(size.width - stroke, size.height - stroke),
    )
    val outline = Outline.Rounded(RoundRect(rect, CornerRadius(radiusPx, radiusPx)))
    drawOutlineHighlight(
        outline = outline,
        color = color,
        strokeWidth = stroke,
    )
}

private fun DrawScope.drawOutlineHighlight(
    outline: Outline,
    color: Color,
    strokeWidth: Float,
) {
    val brush = Brush.verticalGradient(
        colors = listOf(
            color.copy(alpha = 0.65f),
            color.copy(alpha = 0.10f),
            color.copy(alpha = 0.20f),
        ),
        startY = 0f,
        endY = size.height,
    )
    when (outline) {
        is Outline.Rounded -> {
            val path = Path().apply {
                addRoundRect(outline.roundRect)
            }
            drawPath(
                path = path,
                brush = brush,
                style = Stroke(width = strokeWidth),
            )
        }

        is Outline.Generic -> drawPath(
            path = outline.path,
            brush = brush,
            style = Stroke(width = strokeWidth),
        )

        is Outline.Rectangle -> {
            val rect = outline.rect
            drawPath(
                path = Path().apply { addRect(rect) },
                brush = brush,
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

/**
 * Sombra suave y difusa para superficies flotantes, ideal para combinar
 * con efectos glassmórficos.
 */
fun Modifier.glassShadow(
    color: Color,
    blur: Dp = 32.dp,
    spread: Dp = 0.dp,
    offsetY: Dp = 12.dp,
    cornerRadius: Dp = 24.dp,
): Modifier = drawBehind {
    val spreadPx = spread.toPx()
    val rect = Rect(
        offset = Offset(-spreadPx, offsetY.toPx() - spreadPx),
        size = Size(size.width + spreadPx * 2, size.height + spreadPx * 2),
    )
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = rect.center,
            radius = blur.toPx() + rect.width / 2f,
        ),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        style = Fill,
    )
}

/**
 * Resplandor de fondo útil para diferenciar burbujas o pulsos activos.
 */
@Composable
fun Modifier.glow(
    color: Color,
    radius: Dp = 80.dp,
): Modifier {
    val remembered = remember(color, radius) {
        Modifier.drawBehind {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.45f), Color.Transparent),
                    center = center,
                    radius = radius.toPx(),
                ),
                radius = radius.toPx(),
                center = center,
            )
        }
    }
    return this then remembered
}
