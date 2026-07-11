package com.example.airvibe.feature.radar.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile

@Composable
fun OwnProfileSheet(
    profile: ScannerProfile,
    onSave: (displayName: String, status: String, tags: List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayName by remember(profile.id) { mutableStateOf(profile.displayName) }
    var status by remember(profile.id) { mutableStateOf(profile.status) }
    var tags by remember(profile.id) {
        mutableStateOf(profile.tags.joinToString(", "))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Mi perfil en el radar",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Estos datos se anuncian por Bluetooth a los dispositivos cercanos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        GlassTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = "Nombre visible",
            modifier = Modifier.fillMaxWidth(),
        )
        GlassTextField(
            value = status,
            onValueChange = { status = it },
            label = "Estado / profesión",
            modifier = Modifier.fillMaxWidth(),
        )
        GlassTextField(
            value = tags,
            onValueChange = { tags = it },
            label = "Etiquetas (separadas por coma)",
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LiquidGlassButton(
                text = "Cancelar",
                onClick = onDismiss,
                variant = LiquidGlassVariant.Secondary,
                modifier = Modifier.weight(1f),
            )
            LiquidGlassButton(
                text = "Guardar",
                onClick = {
                    val parsedTags = tags.split(',')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    onSave(displayName, status, parsedTags)
                },
                variant = LiquidGlassVariant.Primary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
