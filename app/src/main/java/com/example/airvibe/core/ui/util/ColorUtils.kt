package com.example.airvibe.core.ui.util

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Ajusta la luminosidad de un color HSL.
 *
 * @param factor < 1 oscurece, > 1 aclara. 1 no hace nada.
 */
fun Color.lighten(factor: Float): Color {
    val safe = factor.coerceIn(0f, 2f)
    return Color(
        red = min(1f, red * safe),
        green = min(1f, green * safe),
        blue = min(1f, blue * safe),
        alpha = alpha,
    )
}

fun Color.darken(factor: Float): Color {
    val safe = factor.coerceIn(0f, 2f)
    val inv = max(0f, 1f - safe.coerceAtMost(1f))
    return Color(
        red = red * inv,
        green = green * inv,
        blue = blue * inv,
        alpha = alpha,
    )
}
