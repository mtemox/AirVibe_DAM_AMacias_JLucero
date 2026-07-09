package com.example.airvibe.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.modifiers.glassBlur
import com.example.airvibe.core.designsystem.modifiers.glassHighlightBorder
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.core.designsystem.theme.LocalGlassTokens

/**
 * Variantes estilo shadcn/ui + iOS 18.
 */
enum class LiquidGlassVariant { Primary, Secondary, Ghost, Destructive }

@Composable
fun LiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    variant: LiquidGlassVariant = LiquidGlassVariant.Primary,
    size: ButtonSize = ButtonSize.Medium,
    cornerRadius: Dp = 999.dp,
    contentPadding: PaddingValues = size.contentPadding,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "buttonScale",
    )

    val tokens = AirVibeTheme.glass
    val colors = colorsFor(variant, tokens, enabled)
    val shape = RoundedCornerShape(cornerRadius)

    Row(
        modifier = modifier
            .scale(scale)
            .glassBlur(radius = 18.dp, shape = shape)
            .clip(shape)
            .background(brush = colors.background)
            .glassHighlightBorder(color = colors.border, cornerRadius = cornerRadius)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(size.iconSize),
            )
        }
        Text(
            text = text,
            color = colors.content,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(size.iconSize),
            )
        }
    }
}

enum class ButtonSize(val contentPadding: PaddingValues, val iconSize: Dp) {
    Small(PaddingValues(horizontal = 14.dp, vertical = 8.dp), 16.dp),
    Medium(PaddingValues(horizontal = 20.dp, vertical = 12.dp), 18.dp),
    Large(PaddingValues(horizontal = 24.dp, vertical = 16.dp), 20.dp),
}

private data class ButtonColors(
    val background: Brush,
    val content: Color,
    val border: Color,
)

@Composable
private fun colorsFor(
    variant: LiquidGlassVariant,
    tokens: com.example.airvibe.core.designsystem.theme.GlassTokens,
    enabled: Boolean,
): ButtonColors {
    val alpha = if (enabled) 1f else 0.45f
    val scheme = MaterialTheme.colorScheme
    return when (variant) {
        LiquidGlassVariant.Primary -> ButtonColors(
            background = Brush.horizontalGradient(
                colors = listOf(
                    scheme.primary.copy(alpha = alpha),
                    scheme.primary.copy(alpha = alpha * 0.85f),
                ),
            ),
            content = scheme.onPrimary.copy(alpha = alpha),
            border = Color.White.copy(alpha = 0.25f * alpha),
        )
        LiquidGlassVariant.Secondary -> ButtonColors(
            background = Brush.verticalGradient(
                colors = listOf(
                    tokens.surfaceFillStrong.copy(alpha = alpha),
                    tokens.surfaceFill.copy(alpha = alpha),
                ),
            ),
            content = scheme.onSurface.copy(alpha = alpha),
            border = tokens.outerBorder.copy(alpha = alpha),
        )
        LiquidGlassVariant.Ghost -> ButtonColors(
            background = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Transparent),
            ),
            content = scheme.onSurface.copy(alpha = 0.75f * alpha),
            border = Color.Transparent,
        )
        LiquidGlassVariant.Destructive -> ButtonColors(
            background = Brush.horizontalGradient(
                colors = listOf(
                    scheme.error.copy(alpha = alpha),
                    scheme.error.copy(alpha = alpha * 0.9f),
                ),
            ),
            content = scheme.onError.copy(alpha = alpha),
            border = Color.White.copy(alpha = 0.2f * alpha),
        )
    }
}
