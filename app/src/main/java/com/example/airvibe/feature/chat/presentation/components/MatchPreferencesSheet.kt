package com.example.airvibe.feature.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.components.GlassCard
import com.example.airvibe.core.designsystem.components.GlassPill
import com.example.airvibe.core.designsystem.components.GlassTextField
import com.example.airvibe.core.designsystem.components.LiquidGlassButton
import com.example.airvibe.core.designsystem.components.LiquidGlassVariant
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.feature.chat.domain.model.MatchCriteria
import com.example.airvibe.feature.chat.domain.repository.MatchPreferencesRepository
import kotlinx.coroutines.launch

/**
 * Bottom sheet con los **filtros de matching** que el usuario
 * define. Al cerrarlo, persiste los criterios vía el
 * [MatchPreferencesRepository].
 *
 * Diseño: glass card con un switch maestro, una lista de chips
 * (keywords) y un campo de texto + botón "Agregar" para
 * incorporar nuevas palabras.
 */
@Composable
fun MatchPreferencesSheet(
    repository: MatchPreferencesRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = AirVibeTheme.glass
    val scope = rememberCoroutineScope()

    var criteria by remember { mutableStateOf(repository.current()) }
    var newKeyword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        repository.observe().collect { criteria = it }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 40.dp, height = 4.dp)
                .clip(CircleShape)
                .background(tokens.outerBorder),
        )

        Text(
            text = "Alertas inteligentes",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Te avisaremos cuando encontremos un peer cercano que coincida con estas palabras clave.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Matching activo",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Recibe notificaciones push cuando se descubra un match.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = criteria.enabled,
                    onCheckedChange = { value ->
                        val updated = criteria.copy(enabled = value)
                        criteria = updated
                        scope.launch { repository.set(updated) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }

        Text(
            text = "Palabras clave (${criteria.keywords.size})",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (criteria.keywords.isEmpty()) {
            Text(
                text = "Aún no agregaste palabras. Ej: \"Albañil\", \"Electricista\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(criteria.keywords, key = { it }) { keyword ->
                    KeywordChip(
                        text = keyword,
                        onRemove = {
                            val updated = criteria.copy(
                                keywords = criteria.keywords - keyword,
                            )
                            criteria = updated
                            scope.launch { repository.set(updated) }
                        },
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                GlassTextField(
                    value = newKeyword,
                    onValueChange = { newKeyword = it },
                    placeholder = "Agregar palabra…",
                    singleLine = true,
                )
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .clickableNoRipple {
                        val trimmed = newKeyword.trim()
                        if (trimmed.isNotEmpty() && trimmed !in criteria.keywords) {
                            val updated = criteria.copy(
                                keywords = criteria.keywords + trimmed,
                            )
                            criteria = updated
                            newKeyword = ""
                            scope.launch { repository.set(updated) }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Agregar",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LiquidGlassButton(
            text = "Listo",
            onClick = onDismiss,
            variant = LiquidGlassVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun KeywordChip(
    text: String,
    onRemove: () -> Unit,
) {
    val tokens = AirVibeTheme.glass
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tokens.surfaceFillStrong)
            .border(width = 1.dp, color = tokens.outerBorder, shape = RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        GlassPill(text = "#$text", tint = MaterialTheme.colorScheme.primary)
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                .clickableNoRipple(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Quitar $text",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.clickable(
        interactionSource = source,
        indication = null,
        onClick = onClick,
    )
}
