package com.example.airvibe.feature.radar.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import java.io.File
import com.example.airvibe.feature.radar.domain.model.PresenceStatus
import com.example.airvibe.feature.radar.domain.model.RadarNodeKind
import com.example.airvibe.feature.radar.domain.scanner.ScannerProfile

data class OwnProfileDraft(
    val displayName: String,
    val status: String,
    val tags: List<String>,
    val kind: RadarNodeKind,
    val presence: PresenceStatus,
    val headline: String = "",
    val bio: String = "",
    val isPremium: Boolean = false,
    val premiumCatalog: String? = null,
    val avatarUri: Uri? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnProfileSheet(
    profile: ScannerProfile,
    onSave: (OwnProfileDraft) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayName by remember(profile.id) { mutableStateOf(profile.displayName) }
    var status by remember(profile.id) { mutableStateOf(profile.status) }
    var tags by remember(profile.id) { mutableStateOf(profile.tags.joinToString(", ")) }
    var kind by remember(profile.id) { mutableStateOf(profile.kind) }
    var presence by remember(profile.id) { mutableStateOf(profile.presence) }
    var headline by remember(profile.id) { mutableStateOf(profile.headline) }
    var bio by remember(profile.id) { mutableStateOf(profile.bio) }
    var isPremium by remember(profile.id) { mutableStateOf(profile.isPremium) }
    var premiumCatalog by remember(profile.id) { mutableStateOf(profile.premiumCatalog.orEmpty()) }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    
    val context = LocalContext.current
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedAvatarUri = uri
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempCameraUri != null) {
                selectedAvatarUri = tempCameraUri
            }
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val tempFile = File.createTempFile("avatar_", ".jpg", context.cacheDir)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            }
        }
    )

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val tempFile = File.createTempFile("avatar_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Colores Vibe Pure
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundGray = MaterialTheme.colorScheme.background
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f) // Ocupa buena parte de la pantalla
            .background(backgroundGray)
            .navigationBarsPadding(),
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Cerrar", tint = onSurfaceColor)
                }
                Text(
                    text = "Editar Perfil",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = onSurfaceColor,
                )
            }
            TextButton(
                onClick = {
                    onSave(
                        OwnProfileDraft(
                            displayName = displayName,
                            status = status,
                            tags = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                            kind = kind,
                            presence = presence,
                            headline = headline,
                            bio = bio,
                            isPremium = isPremium,
                            premiumCatalog = premiumCatalog.takeIf { isPremium && it.isNotBlank() },
                            avatarUri = selectedAvatarUri,
                        )
                    )
                },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("Guardar", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }

        // Scrollable content
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Foto de Perfil
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(112.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(4.dp, Color(0xFFEEEEEE), CircleShape)
                                .clickable {
                                    showImageSourceDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val imageToLoad = selectedAvatarUri ?: profile.avatarUrl ?: profile.avatarBase64
                            if (imageToLoad != null) {
                                AsyncImage(
                                    model = imageToLoad,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = displayName.take(2).uppercase(),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-4).dp, y = (-4).dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    showImageSourceDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Rounded.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Info Básica
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Información Pública", style = MaterialTheme.typography.labelSmall.copy(color = onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                    
                    VibeTextField(value = displayName, onValueChange = { displayName = it }, label = "Nombre visible")
                    VibeTextField(value = headline, onValueChange = { headline = it }, label = "Profesión / Título", placeholder = "Ej. Diseñador, Estudiante...")
                    VibeTextField(value = bio, onValueChange = { bio = it }, label = "Biografía corta", minLines = 3)
                    VibeTextField(value = tags, onValueChange = { tags = it }, label = "Etiquetas (separadas por coma)")
                }
            }

            // Estado Actual (Bento)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Estado en el Radar", style = MaterialTheme.typography.labelSmall.copy(color = onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                    
                    VibeTextField(value = status, onValueChange = { status = it }, label = "Vibe o estado personalizado", placeholder = "Ej. En busca de café...")
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BentoStatusCard(
                            label = "Disponible",
                            icon = Icons.Rounded.CheckCircle,
                            iconTint = Color(0xFF10B981),
                            selected = presence == PresenceStatus.Available,
                            onClick = { presence = PresenceStatus.Available },
                            modifier = Modifier.weight(1f)
                        )
                        BentoStatusCard(
                            label = "Buscando",
                            icon = Icons.Rounded.PersonSearch,
                            iconTint = Color(0xFF6366F1),
                            selected = presence == PresenceStatus.Looking,
                            onClick = { presence = PresenceStatus.Looking },
                            modifier = Modifier.weight(1f)
                        )
                        BentoStatusCard(
                            label = "Ocupado",
                            icon = Icons.Rounded.Block,
                            iconTint = Color(0xFFEF4444),
                            selected = presence == PresenceStatus.Busy || presence == PresenceStatus.Away,
                            onClick = { presence = PresenceStatus.Busy },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Tipo de Nodo & Premium (Grouped List)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Configuración Avanzada", style = MaterialTheme.typography.labelSmall.copy(color = onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                    
                    GroupedList {
                        GroupedListItem(
                            title = "Tipo de Nodo",
                            subtitle = kind.displayName,
                            icon = if (kind == RadarNodeKind.Person) Icons.Rounded.Person else Icons.Rounded.Groups,
                            iconBg = Color(0xFFDDE1FF),
                            iconTint = Color(0xFF001355),
                            onClick = {
                                kind = when (kind) {
                                    RadarNodeKind.Person -> RadarNodeKind.Service
                                    RadarNodeKind.Service -> RadarNodeKind.Group
                                    RadarNodeKind.Group -> RadarNodeKind.Person
                                }
                            }
                        )
                        HorizontalDivider(color = borderColor, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        GroupedListItem(
                            title = "Modo Premium",
                            subtitle = if (isPremium) "Activado" else "Desactivado",
                            icon = Icons.Rounded.WorkspacePremium,
                            iconBg = Color(0xFFFFF0B3),
                            iconTint = Color(0xFF7A5C00),
                            onClick = { isPremium = !isPremium }
                        )
                    }
                }
            }

            // Catálogo Premium (solo si está activo)
            if (isPremium) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Catálogo de Servicios", style = MaterialTheme.typography.labelSmall.copy(color = onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                        VibeTextField(
                            value = premiumCatalog,
                            onValueChange = { premiumCatalog = it },
                            label = "Tus precios o servicios (visible sin Match)",
                            minLines = 2,
                            placeholder = "Ej. Logos $50 · Web $200"
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Foto de Perfil") },
            text = { Text("¿Desde dónde quieres obtener la imagen?") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }) {
                    Text("Galería")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    launchCamera()
                }) {
                    Text("Cámara")
                }
            }
        )
    }
}

@Composable
fun VibeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    minLines: Int = 1,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF444655)))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF999999)) },
            minLines = minLines,
            maxLines = if (minLines > 1) 5 else 1,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BentoStatusCard(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val bgColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.Start) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GroupedList(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        content = content
    )
}

@Composable
fun GroupedListItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
