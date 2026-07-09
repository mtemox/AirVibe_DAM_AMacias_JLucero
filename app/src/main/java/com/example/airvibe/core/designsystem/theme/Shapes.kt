package com.example.airvibe.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AirVibeShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(18.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object AirVibeRadii {
    val xs = 8.dp
    val sm = 12.dp
    val md = 18.dp
    val lg = 24.dp
    val xl = 32.dp
    val pill = 999.dp
}
