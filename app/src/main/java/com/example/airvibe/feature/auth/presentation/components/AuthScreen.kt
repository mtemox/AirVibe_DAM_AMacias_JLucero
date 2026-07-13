package com.example.airvibe.feature.auth.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.airvibe.R
import com.example.airvibe.core.designsystem.components.WaveHeader
import com.example.airvibe.feature.auth.presentation.AuthMode
import com.example.airvibe.feature.auth.presentation.AuthUiEvent
import com.example.airvibe.feature.auth.presentation.AuthUiState
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.slideInVertically

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun AuthScreenContent(
    state: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val patternPath = androidx.compose.ui.graphics.Path().apply {
                var yOffset = height * 0.5f
                while (yOffset < height * 1.5f) {
                    moveTo(-width * 0.2f, yOffset)
                    cubicTo(
                        width * 0.3f, yOffset - height * 0.2f,
                        width * 0.7f, yOffset + height * 0.3f,
                        width * 1.2f, yOffset - height * 0.1f
                    )
                    yOffset += height * 0.12f
                }
                var xOffset = -width * 0.5f
                while (xOffset < width * 1.5f) {
                    moveTo(xOffset, height * 0.5f)
                    cubicTo(
                        xOffset + width * 0.3f, height * 0.7f,
                        xOffset - width * 0.2f, height * 0.9f,
                        xOffset + width * 0.4f, height * 1.2f
                    )
                    xOffset += width * 0.2f
                }
            }
            drawPath(
                path = patternPath,
                color = Color.LightGray.copy(alpha = 0.4f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            WaveHeader(
                waveHeight = 580.dp, // Extender la ola hacia más abajo
                brush = Brush.verticalGradient(
                    0.0f to Color(0xFF305CDE),  // Azul oscuro
                    0.45f to Color(0xFF305CDE), // Sigue azul oscuro
                    0.65f to Color(0xFFFF6868), // Rojo (Tomate) directamente
                    0.85f to Color(0xFFFFA84D), // Naranja
                    1.0f to Color(0xFFFFDE21)   // Amarillo
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(start = 32.dp, end = 32.dp, top = 64.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = if (state.mode == AuthMode.SignUp) "Crear\nCuenta" else "Bienvenido\nde nuevo",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = androidx.compose.ui.unit.TextUnit(58f, androidx.compose.ui.unit.TextUnitType.Sp),
                            lineHeight = androidx.compose.ui.unit.TextUnit(62f, androidx.compose.ui.unit.TextUnitType.Sp)
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (state.mode == AuthMode.SignUp) "Únete al radar sin internet." else "Conéctate a tu radar local.",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                }
            }

            Column(modifier = Modifier.offset(y = (-55).dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Conexión segura vía malla Bluetooth y Wi-Fi",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(initialOffsetY = { it / 4 }) + androidx.compose.animation.fadeIn()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = state.mode == AuthMode.SignUp,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            FluidTextField(
                                value = state.displayName,
                                onValueChange = { onEvent(AuthUiEvent.DisplayNameChanged(it)) },
                                label = "Nombre completo",
                                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                                imeAction = ImeAction.Next,
                            )
                        }

                FluidTextField(
                    value = state.email,
                    onValueChange = { onEvent(AuthUiEvent.EmailChanged(it)) },
                    label = "Correo electrónico",
                    leadingIcon = { Icon(Icons.Rounded.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                )

                var passwordVisible by remember { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FluidTextField(
                        value = state.password,
                        onValueChange = { onEvent(AuthUiEvent.PasswordChanged(it)) },
                        label = "Contraseña",
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                        imeAction = ImeAction.Done,
                        onImeAction = { onEvent(AuthUiEvent.Submit) },
                    )
                    
                    if (state.mode == AuthMode.SignIn) {
                        TextButton(
                            onClick = { /* TODO */ },
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("¿Olvidaste tu contraseña?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = state.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        ErrorBanner(
                            message = state.errorMessage.orEmpty(),
                            onDismiss = { onEvent(AuthUiEvent.DismissError) },
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onEvent(AuthUiEvent.Submit) },
                        enabled = state.canSubmit && !state.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(
                                text = if (state.mode == AuthMode.SignIn) "Iniciar sesión" else "Registrarse",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onEvent(
                                AuthUiEvent.ModeChanged(
                                    if (state.mode == AuthMode.SignIn) AuthMode.SignUp else AuthMode.SignIn
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = if (state.mode == AuthMode.SignIn) "Regístrate" else "Iniciar sesión",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
            }
            }
        }
    }
}

@Composable
private fun FluidTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityToggle: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = MaterialTheme.colorScheme.outline) },
        leadingIcon = leadingIcon,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(painterResource(android.R.drawable.ic_menu_view), contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onAny = { onImeAction() }
        ),
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Cerrar",
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .size(16.dp)
                .clickable(onClick = onDismiss),
        )
    }
}
