package com.example.airvibe.feature.auth.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.airvibe.R
import com.example.airvibe.core.designsystem.components.WaveHeader

import androidx.compose.ui.graphics.Brush

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableIntStateOf(0) }
    
    val pages = listOf(
        OnboardingPage(
            title = "Comunícate sin señal",
            description = "Mantente conectado en eventos, festivales o lugares sin red. AirVibe crea una malla local para ti.",
            icon = Icons.Rounded.Person
        ),
        OnboardingPage(
            title = "Chat y Foros Seguros",
            description = "Participa en foros públicos o comunícate de forma privada con personas cercanas.",
            icon = Icons.Rounded.Chat
        ),
        OnboardingPage(
            title = "Alertas de Emergencia",
            description = "Envía y recibe alertas críticas incluso cuando los servicios celulares fallan.",
            icon = Icons.Rounded.Warning
        )
    )
    
    val page = pages[currentPage]
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            WaveHeader(
                waveHeight = 320.dp, // Acortar la ola SOLO en esta pantalla
                brush = Brush.verticalGradient(
                    0.0f to androidx.compose.ui.graphics.Color(0xFF305CDE),
                    0.45f to androidx.compose.ui.graphics.Color(0xFF305CDE),
                    0.65f to androidx.compose.ui.graphics.Color(0xFFFF6868),
                    0.85f to androidx.compose.ui.graphics.Color(0xFFFFA84D),
                    1.0f to androidx.compose.ui.graphics.Color(0xFFFFDE21)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().statusBarsPadding()
                ) {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Omitir",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 32.dp, end = 32.dp, top = 64.dp), // Empujar hacia abajo
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = "AirVibe",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = androidx.compose.ui.unit.TextUnit(56f, androidx.compose.ui.unit.TextUnitType.Sp)
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-80).dp)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Illustration and Content Area
                AnimatedContent(targetState = currentPage, label = "page_content", modifier = Modifier.weight(1f)) { targetPage ->
                    val targetPageData = pages[targetPage]
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Pulsing Icon
                        Box(contentAlignment = Alignment.Center) {
                            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 1f, targetValue = 1.2f,
                                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                    animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                ), label = "scale"
                            )
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.2f, targetValue = 0.6f,
                                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                    animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                ), label = "alpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .scale(scale)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha), CircleShape)
                            )
                            Icon(
                                imageVector = targetPageData.icon,
                                contentDescription = targetPageData.title,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        Text(
                            text = targetPageData.title,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = targetPageData.description,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Page Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.forEachIndexed { index, _ ->
                        val isSelected = index == currentPage
                        val width = if (isSelected) 32.dp else 8.dp
                        val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        Box(modifier = Modifier.size(width = width, height = 8.dp).clip(CircleShape).background(color))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Next Button
                Button(
                    onClick = {
                        if (currentPage < pages.lastIndex) {
                            currentPage++
                        } else {
                            onNext()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 16.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Siguiente",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = "Siguiente",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
