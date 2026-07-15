package com.example.airvibe.core.ui.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.theme.AirVibeTheme

/**
 * Snackbar personalizado que respeta la línea visual glass de la
 * app: bordes redondeados pronunciados, fondo con el cristal del
 * tema, borde sutil coloreado según la severidad y un pequeño
 * icono redondo a la izquierda del mensaje.
 *
 * Se monta dentro de un `SnackbarHost` (ver `rememberUserMessages`).
 */
@Composable
fun UserFeedbackSnackbar(
    data: SnackbarData,
    modifier: Modifier = Modifier,
) {
    val severity = (data.visuals as? UserMessageVisuals)?.severity ?: Severity.Info
    val palette = paletteFor(severity)
    val icon = when (severity) {
        Severity.Success -> Icons.Rounded.CheckCircle
        Severity.Error -> Icons.Rounded.ErrorOutline
        Severity.Info -> Icons.Rounded.Info
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Snackbar(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.background)
                    .border(
                        width = 1.dp,
                        color = palette.border,
                        shape = RoundedCornerShape(18.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(palette.iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.iconTint,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private data class FeedbackPalette(
    val background: Color,
    val border: Color,
    val iconBg: Color,
    val iconTint: Color,
)

@Composable
private fun paletteFor(severity: Severity): FeedbackPalette {
    val tokens = AirVibeTheme.glass
    return when (severity) {
        Severity.Success -> FeedbackPalette(
            background = tokens.surfaceFillStrong,
            border = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            iconTint = MaterialTheme.colorScheme.primary,
        )
        Severity.Error -> FeedbackPalette(
            background = tokens.surfaceFillStrong,
            border = MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
            iconBg = MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
            iconTint = MaterialTheme.colorScheme.error,
        )
        Severity.Info -> FeedbackPalette(
            background = tokens.surfaceFillStrong,
            border = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            iconBg = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
