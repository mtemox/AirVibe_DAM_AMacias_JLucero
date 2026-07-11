package com.example.airvibe.feature.radar.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.airvibe.core.designsystem.components.AvatarMonogram
import com.example.airvibe.core.designsystem.components.GlassCard
import com.example.airvibe.core.designsystem.components.GlassPill
import com.example.airvibe.core.designsystem.components.LiquidGlassButton
import com.example.airvibe.core.designsystem.components.LiquidGlassVariant
import com.example.airvibe.core.designsystem.components.StatusDot
import com.example.airvibe.core.designsystem.theme.AirVibeTheme
import com.example.airvibe.feature.radar.domain.model.PersonProfile
import com.example.airvibe.feature.radar.domain.model.PresenceStatus
import com.example.airvibe.feature.radar.domain.model.RadarNodeKind

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfilePreviewContent(
    profile: PersonProfile,
    nodeKind: RadarNodeKind,
    accentColor: Color,
    onConnect: () -> Unit,
    onAddContact: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = AirVibeTheme.glass

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(tokens.outerBorder),
        )
        Spacer(modifier = Modifier.height(18.dp))

        // Hero glass card con datos principales
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            contentPadding = PaddingValues(20.dp),
            tint = accentColor.copy(alpha = 0.08f),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AvatarMonogram(
                        name = profile.displayName,
                        size = 64.dp,
                    )
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(2.dp),
                    ) {
                        StatusDot(
                            color = presenceColor(profile.presence),
                            pulse = profile.presence == PresenceStatus.Online ||
                                profile.presence == PresenceStatus.Available,
                            size = 14.dp,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    GlassPill(
                        text = nodeKind.displayName,
                        tint = accentColor,
                    )
                    Text(
                        text = profile.headline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        StatusDot(color = presenceColor(profile.presence), size = 8.dp)
                        Text(
                            text = profile.presence.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "a ${profile.distanceMeters} m",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(tokens.surfaceFillStrong)
                        .border(width = 1.dp, color = tokens.outerBorder, shape = CircleShape)
                        .clickableNoRipple(onToggleFavorite),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (profile.isFavorite) {
                            Icons.Rounded.Star
                        } else {
                            Icons.Rounded.StarBorder
                        },
                        contentDescription = "Favorito",
                        tint = if (profile.isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val bioText = bioForPreview(profile)

        // Bio + etiquetas
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(18.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Sobre ${profile.displayName.substringBefore(' ')}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (bioText.isNotBlank()) {
                    Text(
                        text = bioText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (profile.tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        profile.tags.take(6).forEach { tag ->
                            GlassPill(text = "#$tag", tint = accentColor)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LiquidGlassButton(
                text = "Conectar",
                onClick = onConnect,
                icon = Icons.AutoMirrored.Rounded.Chat,
                variant = LiquidGlassVariant.Primary,
                modifier = Modifier.weight(1f),
            )
            LiquidGlassButton(
                text = "Agregar",
                onClick = onAddContact,
                icon = Icons.Rounded.PersonAddAlt,
                variant = LiquidGlassVariant.Secondary,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

/** Evita repetir en bio lo que ya se muestra como chips de etiquetas. */
private fun bioForPreview(profile: PersonProfile): String {
    var text = profile.bio.trim()
    profile.tags.forEach { tag ->
        text = text.replace(" · $tag", "")
            .replace("· $tag", "")
            .replace(tag, "")
    }
    text = text.trim(' ', '·', ',', ';')
    if (text.isBlank() || text == profile.headline) return profile.headline
    return text
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = source,
        indication = null,
        onClick = onClick,
    )
}

@Composable
private fun presenceColor(presence: PresenceStatus): Color = when (presence) {
    PresenceStatus.Online -> MaterialTheme.colorScheme.tertiary
    PresenceStatus.Available -> MaterialTheme.colorScheme.tertiary
    PresenceStatus.Looking -> MaterialTheme.colorScheme.primary
    PresenceStatus.Busy -> MaterialTheme.colorScheme.error
    PresenceStatus.Away -> MaterialTheme.colorScheme.outline
}
