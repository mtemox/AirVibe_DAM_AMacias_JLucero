package com.example.airvibe.feature.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.BroadcastOnPersonal
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(Color.Transparent),
    ) {
        // Pill container for text input and secondary actions
        Row(
            modifier = Modifier
                .weight(1f)
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Mood,
                contentDescription = "Emoji",
                tint = Color(0xFF747686),
                modifier = Modifier.size(28.dp)
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "Mensaje",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.dp.value.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        ),
                        color = Color(0xFF747686).copy(alpha = 0.8f),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = false,
                    textStyle = LocalTextStyle.current.copy(
                        color = Color(0xFF1A1C1C),
                        fontSize = 18.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF075E54)),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Default,
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (showBroadcast) {
                Icon(
                    imageVector = Icons.Rounded.BroadcastOnPersonal,
                    contentDescription = "Broadcast",
                    tint = if (isBroadcasting) Color(0xFF075E54) else Color(0xFF747686),
                    modifier = Modifier
                        .size(28.dp)
                        .clickableNoRipple(enabled = enabled && !isBroadcasting, onClick = onBroadcast)
                )
            }

            Icon(
                imageVector = Icons.Rounded.AttachFile,
                contentDescription = "Attach",
                tint = Color(0xFF747686),
                modifier = Modifier.size(28.dp).padding(start = 4.dp)
            )

            Icon(
                imageVector = Icons.Rounded.PhotoCamera,
                contentDescription = "Camera",
                tint = Color(0xFF747686),
                modifier = Modifier.size(28.dp).padding(start = 4.dp)
            )
        }

        // Action button (Mic or Send)
        val hasText = value.isNotBlank()
        val actionIcon = if (hasText) Icons.AutoMirrored.Rounded.Send else Icons.Rounded.Mic
        val actionDescription = if (hasText) "Enviar" else "Grabar"
        
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF075E54))
                .clickableNoRipple(enabled = enabled, onClick = {
                    if (hasText) onSend() else {}
                }),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = actionIcon,
                contentDescription = actionDescription,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
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
