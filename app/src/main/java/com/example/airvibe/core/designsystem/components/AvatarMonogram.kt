package com.example.airvibe.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Avatar circular con iniciales. Si [accentBrush] se omite, se genera
 * un degradado determinístico a partir del nombre.
 */
@Composable
fun AvatarMonogram(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    accentBrush: Brush? = null,
    imageModel: Any? = null,
) {
    val initials = remember(name) { computeInitials(name) }
    val brush = accentBrush ?: remember(name) { gradientFor(name) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush = brush),
        contentAlignment = Alignment.Center,
    ) {
        if (imageModel != null) {
            val decodedModel = remember(imageModel) {
                if (imageModel is String && !imageModel.startsWith("http")) {
                    try {
                        android.util.Base64.decode(imageModel, android.util.Base64.DEFAULT)
                    } catch (e: Exception) {
                        imageModel
                    }
                } else {
                    imageModel
                }
            }
            
            coil.compose.AsyncImage(
                model = decodedModel,
                contentDescription = name,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (size.value * 0.38f).sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

private fun computeInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    val parts = trimmed.split(Regex("\\s+"))
    return when {
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().first().toString() + parts.last().first().toString()).uppercase()
    }
}

private fun gradientFor(seed: String): Brush {
    val palette = listOf(
        Color(0xFF6366F1) to Color(0xFF8B5CF6),
        Color(0xFF06B6D4) to Color(0xFF3B82F6),
        Color(0xFF10B981) to Color(0xFF14B8A6),
        Color(0xFFF59E0B) to Color(0xFFEF4444),
        Color(0xFFEC4899) to Color(0xFF8B5CF6),
        Color(0xFF0EA5E9) to Color(0xFF6366F1),
        Color(0xFF22D3EE) to Color(0xFF6366F1),
    )
    val index = (seed.hashCode().toLong() and 0x7FFFFFFF).toInt() % palette.size
    val (a, b) = palette[index]
    return Brush.linearGradient(colors = listOf(a, b))
}
