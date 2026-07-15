package com.example.airvibe.feature.main.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.airvibe.feature.main.presentation.MainTab

class CurvedCutoutShape(
    private val cutoutCenterX: Float,
    private val cornerRadius: Float,
    private val holeRadius: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            // Empieza en la esquina superior izquierda
            moveTo(0f, cornerRadius)
            quadraticBezierTo(0f, 0f, cornerRadius, 0f)

            // Si el cutoutCenterX es 0, todavía no dibujamos el hueco
            if (cutoutCenterX > 0f) {
                val holeWidth = holeRadius * 3f
                val startCutout = cutoutCenterX - holeWidth / 2
                val endCutout = cutoutCenterX + holeWidth / 2

                // Línea hasta antes del hueco
                lineTo(startCutout, 0f)

                // Curva izquierda hacia abajo (el hueco)
                cubicTo(
                    startCutout + holeRadius * 0.5f, 0f,
                    cutoutCenterX - holeRadius * 0.8f, holeRadius * 1.3f,
                    cutoutCenterX, holeRadius * 1.3f
                )

                // Curva derecha hacia arriba
                cubicTo(
                    cutoutCenterX + holeRadius * 0.8f, holeRadius * 1.3f,
                    endCutout - holeRadius * 0.5f, 0f,
                    endCutout, 0f
                )
            }

            // Línea hasta la esquina superior derecha
            lineTo(size.width - cornerRadius, 0f)
            quadraticBezierTo(size.width, 0f, size.width, cornerRadius)

            // Línea hasta la esquina inferior derecha
            lineTo(size.width, size.height)

            // Línea hasta la esquina inferior izquierda
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun AnimatedNavigationBar(
    tabs: Array<MainTab>,
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var barWidth by remember { mutableStateOf(0f) }
    val itemWidth = if (tabs.isNotEmpty() && barWidth > 0) barWidth / tabs.size else 0f

    val selectedIndex = tabs.indexOf(currentTab)
    val targetCenterX = if (itemWidth > 0) (selectedIndex + 0.5f) * itemWidth else 0f

    val animatedCenterX by animateFloatAsState(
        targetValue = targetCenterX,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "CutoutCenterAnimation"
    )

    val holeRadius = 32.dp
    val holeRadiusPx = with(LocalDensity.current) { holeRadius.toPx() }
    val cornerRadius = 24.dp
    val cornerRadiusPx = with(LocalDensity.current) { cornerRadius.toPx() }

    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPaddingPx = with(LocalDensity.current) { bottomPadding.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp + bottomPadding) // Altura suficiente para la barra y la bola flotante
            .onSizeChanged { barWidth = it.width.toFloat() },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Fondo de la barra de navegación con recorte animado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp + bottomPadding)
                .graphicsLayer {
                    shape = CurvedCutoutShape(animatedCenterX, cornerRadiusPx, holeRadiusPx)
                    clip = true
                    shadowElevation = 16.dp.toPx()
                }
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = currentTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isSelected,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Círculo flotante activo
        if (barWidth > 0) {
            val circleSize = 56.dp
            val circleSizePx = with(LocalDensity.current) { circleSize.toPx() }
            
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (animatedCenterX - barWidth / 2).toInt(), // offset from center
                            y = (-holeRadiusPx * 1.3f - bottomPaddingPx).toInt()
                        )
                    }
                    .size(circleSize)
                    .graphicsLayer {
                        shape = CircleShape
                        clip = true
                    }
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { /* Ya está seleccionado */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tabs[selectedIndex].icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Texto del tab activo flotando debajo de la bola
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (animatedCenterX - barWidth / 2).toInt(),
                            y = (-holeRadiusPx * 0.4f - bottomPaddingPx).toInt()
                        )
                    }
            ) {
                Text(
                    text = tabs[selectedIndex].title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
