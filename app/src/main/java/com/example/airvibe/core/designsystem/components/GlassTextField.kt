package com.example.airvibe.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.modifiers.glassBlur
import com.example.airvibe.core.designsystem.modifiers.glassShadow
import com.example.airvibe.core.designsystem.theme.AirVibeTheme

/**
 * Campo de texto con efecto glass que sigue el lenguaje visual de
 * los `GlassCard` y los `LiquidGlassButton`. Acepta un slot para
 * mostrar un icono a la izquierda, un label flotante y un pie de
 * ayuda.
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    contentDescription: String? = null,
) {
    val tokens = AirVibeTheme.glass
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier.semantics { contentDescription?.let { this.contentDescription = it } },
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassShadow(
                    color = if (errorText != null) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                    } else {
                        tokens.shadowColor
                    },
                    cornerRadius = 18.dp,
                )
                .glassBlur(radius = 16.dp, shape = shape)
                .clip(shape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            tokens.surfaceFillStrong,
                            tokens.surfaceFill,
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = if (errorText != null) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
                    } else {
                        tokens.outerBorder
                    },
                    shape = shape,
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (errorText != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(18.dp),
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                        imeAction = imeAction,
                        capitalization = when (keyboardType) {
                            KeyboardType.Email -> KeyboardCapitalization.None
                            KeyboardType.Password -> KeyboardCapitalization.None
                            else -> KeyboardCapitalization.Sentences
                        },
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp),
                    decorationBox = { inner ->
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        inner()
                    },
                )
            }
        }

        if (helperText != null && errorText == null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (errorText != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Iconos sugeridos para los campos del formulario de autenticación. */
object AuthFieldIcons {
    val email: ImageVector = Icons.Rounded.AlternateEmail
    val password: ImageVector = Icons.Rounded.Lock
    val name: ImageVector = Icons.Rounded.Person
}
