package com.example.airvibe.feature.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BroadcastOnPersonal
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.modifiers.glassBlur
import com.example.airvibe.core.designsystem.theme.AirVibeTheme

/**
 * Barra inferior del chat con el campo de texto y dos acciones:
 *
 *  - **Enviar** (icono avión de papel) → mensaje 1-a-1.
 *  - **Broadcast** (icono antena) → envía a TODOS los peers
 *    conectados (tipo invitación a grupo).
 *
 * Sigue la estética glass: fondo translúcido, highlight y sombra
 * suave para elevarse sobre la lista de mensajes.
 */
@Composable
fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onBroadcast: () -> Unit,
    enabled: Boolean,
    isSending: Boolean,
    isBroadcasting: Boolean,
    showBroadcast: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val tokens = AirVibeTheme.glass
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .glassBlur(radius = 18.dp, shape = shape)
            .clip(shape)
            .background(tokens.surfaceFillStrong)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (showBroadcast) {
                ComposerActionIcon(
                    icon = Icons.Rounded.BroadcastOnPersonal,
                    enabled = enabled && !isBroadcasting,
                    highlighted = isBroadcasting,
                    contentDescription = "Enviar a todos",
                    onClick = onBroadcast,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(tokens.surfaceFill)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "Escribe un mensaje…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = false,
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            ComposerActionIcon(
                icon = Icons.AutoMirrored.Rounded.Send,
                enabled = enabled && value.isNotBlank(),
                highlighted = isSending,
                contentDescription = "Enviar mensaje",
                onClick = onSend,
            )
        }
    }
}

@Composable
private fun ComposerActionIcon(
    icon: ImageVector,
    enabled: Boolean,
    highlighted: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = AirVibeTheme.glass
    val bg = when {
        !enabled -> tokens.surfaceFill
        highlighted -> scheme.primary
        else -> scheme.primary.copy(alpha = 0.9f)
    }
    val fg = if (enabled) scheme.onPrimary else scheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
            .clickableNoRipple(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = fg,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun Modifier.clickableNoRipple(enabled: Boolean, onClick: () -> Unit): Modifier {
    val source = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.clickable(
        interactionSource = source,
        indication = null,
        enabled = enabled,
        role = Role.Button,
        onClick = onClick,
    )
}
