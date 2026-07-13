package com.example.airvibe.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.components.GlassTextField
import com.example.airvibe.core.designsystem.components.LiquidGlassButton
import com.example.airvibe.core.designsystem.components.LiquidGlassVariant
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile

@Composable
fun EditProfileSheet(
    currentProfile: ScannerProfile,
    isUpdating: Boolean,
    onSave: (displayName: String, status: String, tags: List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayName by remember { mutableStateOf(currentProfile.displayName) }
    var status by remember { mutableStateOf(currentProfile.status) }
    var tagsRaw by remember { mutableStateOf(currentProfile.tags.joinToString(", ")) }

    val trimmedName = displayName.trim()
    val trimmedStatus = status.trim()
    val canSave = trimmedName.isNotEmpty() && trimmedStatus.isNotEmpty() && !isUpdating

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Editar perfil",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Tu nombre y estado se anunciarán por Bluetooth y se guardarán en Supabase.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        GlassTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = "Nombre",
            placeholder = "Cómo te verán los demás",
            leadingIcon = Icons.Rounded.Person,
            singleLine = true,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth(),
        )

        GlassTextField(
            value = status,
            onValueChange = { status = it },
            label = "Estado",
            placeholder = "Disponible, ocupado, etc.",
            leadingIcon = Icons.Rounded.Mood,
            singleLine = true,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth(),
        )

        GlassTextField(
            value = tagsRaw,
            onValueChange = { tagsRaw = it },
            label = "Tags",
            placeholder = "AirVibe, Música, Viajes (separados por coma)",
            leadingIcon = Icons.Rounded.LocalOffer,
            singleLine = true,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(4.dp))

        LiquidGlassButton(
            text = if (isUpdating) "Guardando..." else "Guardar cambios",
            onClick = {
                val parsedTags = tagsRaw
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                onSave(trimmedName, trimmedStatus, parsedTags)
            },
            enabled = canSave,
            variant = LiquidGlassVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        )

        LiquidGlassButton(
            text = "Cerrar",
            onClick = onDismiss,
            enabled = !isUpdating,
            variant = LiquidGlassVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
