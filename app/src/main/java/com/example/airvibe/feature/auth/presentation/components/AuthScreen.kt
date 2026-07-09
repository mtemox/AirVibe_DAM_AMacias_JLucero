package com.example.airvibe.feature.auth.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.airvibe.R
import com.example.airvibe.core.designsystem.components.AuthFieldIcons
import com.example.airvibe.core.designsystem.components.GlassTextField
import com.example.airvibe.core.designsystem.components.LiquidGlassButton
import com.example.airvibe.core.designsystem.components.LiquidGlassVariant
import com.example.airvibe.core.designsystem.modifiers.glassBlur
import com.example.airvibe.core.designsystem.modifiers.glassShadow
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.feature.auth.presentation.AuthMode
import com.example.airvibe.feature.auth.presentation.AuthUiEvent
import com.example.airvibe.feature.auth.presentation.AuthUiState

/**
 * Pantalla de autenticación. Mantiene la estética glass del resto
 * de la app: un único card translúcido con dos campos (email +
 * password) y un toggle para cambiar entre "Iniciar sesión" y
 * "Crear cuenta".
 */
@Composable
fun AuthScreenContent(
    state: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = AirVibeTheme.glass

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFEEF2FF),
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            AuthCard(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AuthCard(
    state: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = AirVibeTheme.glass
    Column(
        modifier = modifier
            .glassShadow(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                cornerRadius = 32.dp,
            )
            .glassBlur(radius = 28.dp, shape = RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tokens.surfaceFillStrong,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = tokens.outerBorder,
                shape = RoundedCornerShape(32.dp),
            )
            .padding(22.dp),
    ) {
        AuthHeader()
        Spacer(modifier = Modifier.height(22.dp))

        AuthModeToggle(
            mode = state.mode,
            onModeChange = { onEvent(AuthUiEvent.ModeChanged(it)) },
        )

        Spacer(modifier = Modifier.height(22.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.mode == AuthMode.SignUp) {
                GlassTextField(
                    value = state.displayName,
                    onValueChange = { onEvent(AuthUiEvent.DisplayNameChanged(it)) },
                    label = "Nombre",
                    placeholder = "¿Cómo te mostramos en el radar?",
                    leadingIcon = AuthFieldIcons.name,
                    imeAction = ImeAction.Next,
                )
            }

            GlassTextField(
                value = state.email,
                onValueChange = { onEvent(AuthUiEvent.EmailChanged(it)) },
                label = "Email",
                placeholder = "tu@correo.com",
                leadingIcon = AuthFieldIcons.email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )

            GlassTextField(
                value = state.password,
                onValueChange = { onEvent(AuthUiEvent.PasswordChanged(it)) },
                label = "Contraseña",
                placeholder = "Mínimo 6 caracteres",
                leadingIcon = AuthFieldIcons.password,
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = { onEvent(AuthUiEvent.Submit) },
            )
        }

        AnimatedVisibility(
            visible = state.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            ErrorBanner(
                message = state.errorMessage.orEmpty(),
                onDismiss = { onEvent(AuthUiEvent.DismissError) },
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        LiquidGlassButton(
            text = when (state.mode) {
                AuthMode.SignIn -> "Iniciar sesión"
                AuthMode.SignUp -> "Crear cuenta"
            },
            onClick = { onEvent(AuthUiEvent.Submit) },
            icon = when (state.mode) {
                AuthMode.SignIn -> Icons.Rounded.Login
                AuthMode.SignUp -> Icons.Rounded.PersonAddAlt
            },
            variant = LiquidGlassVariant.Primary,
            enabled = state.canSubmit && !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (state.isSubmitting) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun AuthHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .glassBlur(radius = 20.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.30f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Radar,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Tu red de contactos sin internet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AuthModeToggle(
    mode: AuthMode,
    onModeChange: (AuthMode) -> Unit,
) {
    val tokens = AirVibeTheme.glass
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.surfaceFill)
            .border(width = 1.dp, color = tokens.outerBorder, shape = shape)
            .padding(4.dp),
    ) {
        ModeChip(
            text = "Iniciar sesión",
            selected = mode == AuthMode.SignIn,
            onClick = { onModeChange(AuthMode.SignIn) },
            modifier = Modifier.weight(1f),
        )
        ModeChip(
            text = "Crear cuenta",
            selected = mode == AuthMode.SignUp,
            onClick = { onModeChange(AuthMode.SignUp) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = fg,
        )
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                shape = shape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Cerrar",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
