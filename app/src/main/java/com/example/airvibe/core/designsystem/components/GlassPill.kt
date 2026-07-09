package com.example.airvibe.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.theme.AirVibeTheme

/**
 * Etiqueta flotante translúcida estilo "chip". Ideal para mostrar
 * estados, categorías o propiedades en la UI.
 */
@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    cornerRadius: Dp = 999.dp,
) {
    val tokens = AirVibeTheme.glass
    val shape = RoundedCornerShape(cornerRadius)
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tokens.surfaceFillStrong,
                        tokens.surfaceFill,
                    ),
                ),
            )
            .border(width = 1.dp, color = tint.copy(alpha = 0.25f), shape = shape)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(modifier = Modifier.padding(end = 6.dp)) { leading() }
        }
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
