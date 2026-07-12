package com.example.airvibe.feature.radar.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.components.GlassTextField
import com.example.airvibe.core.designsystem.components.LiquidGlassButton
import com.example.airvibe.core.designsystem.components.LiquidGlassVariant

@Composable
fun BroadcastSheet(
    isBroadcasting: Boolean,
    lastBroadcastCount: Int,
    onBroadcast: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var message by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Crear sala cercana",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Los usuarios cercanos recibirán una notificación para unirse. No se envía un mensaje privado invasivo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        GlassTextField(
            value = message,
            onValueChange = { message = it },
            label = "Nombre o tema de la sala",
            modifier = Modifier.fillMaxWidth(),
        )

        if (lastBroadcastCount > 0) {
            Text(
                text = "Invitación enviada a $lastBroadcastCount dispositivo(s).",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LiquidGlassButton(
            text = if (isBroadcasting) "Creando sala..." else "Crear sala",
            onClick = {
                val trimmed = message.trim()
                if (trimmed.isNotEmpty()) onBroadcast(trimmed)
            },
            variant = LiquidGlassVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        )

        LiquidGlassButton(
            text = "Cerrar",
            onClick = onDismiss,
            variant = LiquidGlassVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
