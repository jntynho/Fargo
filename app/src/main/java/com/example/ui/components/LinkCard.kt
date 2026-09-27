package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.ActorEntity
import com.example.data.local.entity.LinkEntity
import com.example.data.local.entity.StudioEntity
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import java.text.SimpleDateFormat
import java.util.*

private enum class CardActionMenuState {
    CLOSED,
    MAIN_MENU,   // 4 Action Buttons: Delete, Edit, URL, Magnet
    URL_SUBMENU, // HD, 4K URL Navigation
    MAGNET_SUBMENU // HD, 4K Magnet Streaming/Download
}

@Composable
fun LinkCard(
    link: LinkEntity,
    actors: List<ActorEntity> = emptyList(),
    studios: List<StudioEntity> = emptyList(),
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    onPlay: (url: String) -> Unit,
    onOpenGallery: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val playUrl = link.url4K ?: link.urlHD ?: link.magnet4K ?: link.magnet

    // Quick Action Overlay State
    var menuState by remember { mutableStateOf(CardActionMenuState.CLOSED) }
    val isOverlayActive = menuState != CardActionMenuState.CLOSED

    // Ultra lightweight GPU-accelerated Zoom & Blur Transitions
    val imageScale by animateFloatAsState(
        targetValue = if (isOverlayActive) 1.06f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cover_scale"
    )
    val imageBlur by animateDpAsState(
        targetValue = if (isOverlayActive) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "cover_blur"
    )

    // Performers / actors names
    val matchedActors = actors.filter { link.actorIds.contains(it.id) }
    val actorsDisplayName = if (matchedActors.isNotEmpty()) {
        matchedActors.joinToString(", ") { it.name }
    } else if (link.actorIds.isNotEmpty()) {
        link.actorIds.joinToString(", ")
    } else {
        ""
    }

    // Studio name
    val studioName = studios.firstOrNull { link.studioIds.contains(it.id) }?.name
        ?: if (link.studioIds.isNotEmpty()) link.studioIds.first() else ""

    // Formatted date
    val displayDate = remember(link.createdAt, link.assignedDate) {
        val ts = link.assignedDate ?: link.createdAt
        formatDisplayDate(ts)
    }

    fun openWebUrl(url: String?) {
        if (!url.isNullOrBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open URL: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No URL specified for this quality", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleMagnet(magnetUri: String?) {
        if (!magnetUri.isNullOrBlank()) {
            onPlay(magnetUri)
        } else {
            Toast.makeText(context, "No Magnet link specified for this quality", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("scene_card_${link.id}")
    ) {
        // ========================================================
        // 1. Edge-to-Edge 16:9 Thumbnail with Tap & Blur/Zoom Overlay
        // ========================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(palette.cardBg)
                .clip(RoundedCornerShape(0.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            menuState = if (isOverlayActive) CardActionMenuState.CLOSED else CardActionMenuState.MAIN_MENU
                        }
                    )
                }
        ) {
            // Background Image with hardware layer acceleration
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = imageScale
                        scaleY = imageScale
                    }
                    .blur(imageBlur)
            ) {
                if (link.coverImage.isNotEmpty()) {
                    AsyncImage(
                        model = link.coverImage,
                        contentDescription = link.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }

            // Smooth Scrim Layer
            if (isOverlayActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.52f))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            menuState = CardActionMenuState.CLOSED
                        }
                )
            }

            // Native Staggered Floating Action Buttons
            AnimatedContent(
                targetState = menuState,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(180, easing = LinearOutSlowInEasing)) +
                            scaleIn(
                                initialScale = 0.82f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                            ))
                        .togetherWith(
                            fadeOut(animationSpec = tween(120, easing = FastOutLinearInEasing)) +
                                    scaleOut(targetScale = 0.9f, animationSpec = tween(120))
                        )
                },
                modifier = Modifier.align(Alignment.Center),
                label = "smooth_card_menu"
            ) { state ->
                when (state) {
                    CardActionMenuState.CLOSED -> {
                        Spacer(modifier = Modifier.size(0.dp))
                    }

                    CardActionMenuState.MAIN_MENU -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            // 1. Delete (Trash) Button
                            StaggeredCircularButton(
                                delayIndex = 0,
                                icon = Icons.Outlined.Delete,
                                label = "Delete",
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White,
                                onClick = {
                                    menuState = CardActionMenuState.CLOSED
                                    onDelete()
                                }
                            )

                            // 2. Edit (Pen) Button
                            StaggeredCircularButton(
                                delayIndex = 1,
                                icon = Icons.Outlined.Edit,
                                label = "Edit",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = {
                                    menuState = CardActionMenuState.CLOSED
                                    onEdit()
                                }
                            )

                            // 3. URL Button
                            val hasUrl = !link.urlHD.isNullOrBlank() || !link.url4K.isNullOrBlank()
                            StaggeredCircularButton(
                                delayIndex = 2,
                                icon = Icons.Outlined.Link,
                                label = "URL",
                                containerColor = if (hasUrl) accent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                contentColor = if (hasUrl) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                onClick = {
                                    menuState = CardActionMenuState.URL_SUBMENU
                                }
                            )

                            // 4. Magnet Button
                            val hasMagnet = !link.magnet.isNullOrBlank() || !link.magnet4K.isNullOrBlank()
                            StaggeredCircularButton(
                                delayIndex = 3,
                                icon = Icons.Outlined.Download,
                                label = "Magnet",
                                containerColor = if (hasMagnet) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                contentColor = if (hasMagnet) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                onClick = {
                                    menuState = CardActionMenuState.MAGNET_SUBMENU
                                }
                            )
                        }
                    }

                    CardActionMenuState.URL_SUBMENU -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            // Back Button
                            StaggeredCircularButton(
                                delayIndex = 0,
                                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                label = "Back",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = { menuState = CardActionMenuState.MAIN_MENU }
                            )

                            // HD URL
                            val hasHd = !link.urlHD.isNullOrBlank()
                            StaggeredQualityButton(
                                delayIndex = 1,
                                title = "HD",
                                subtitle = "Web URL",
                                containerColor = if (hasHd) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                contentColor = Color.White,
                                isAvailable = hasHd,
                                onClick = {
                                    menuState = CardActionMenuState.CLOSED
                                    openWebUrl(link.urlHD)
                                }
                            )

                            // 4K URL
                            val has4K = !link.url4K.isNullOrBlank()
                            StaggeredQualityButton(
                                delayIndex = 2,
                                title = "4K",
                                subtitle = "Web URL",
                                containerColor = if (has4K) Color(0xFFEAB308) else Color.Gray.copy(alpha = 0.4f),
                                contentColor = Color.Black,
                                isAvailable = has4K,
                                onClick = {
                                    menuState = CardActionMenuState.CLOSED
                                    openWebUrl(link.url4K)
                                }
                            )
                        }
                    }

                    CardActionMenuState.MAGNET_SUBMENU -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            // Back Button
                            StaggeredCircularButton(
                                delayIndex = 0,
                                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                label = "Back",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = { menuState = CardActionMenuState.MAIN_MENU }
                            )

                            // HD Magnet
                            val hasHd = !link.magnet.isNullOrBlank()
                            StaggeredQualityButton(
                                delayIndex = 1,
                                title = "HD",
                                subtitle = "Stream / DL",
                                containerColor = if (hasHd) Color(0xFF10B981) else Color.Gray.copy(alpha = 0.4f),
                                contentColor = Color.White,
                                isAvailable = hasHd,
                                onClick = {
                                    menuState = CardActionMenuState.CLOSED
                                    handleMagnet(link.magnet)
                                }
                            )

                            // 4K Magnet
                            val has4K = !link.magnet4K.isNullOrBlank()
                            StaggeredQualityButton(
                                delayIndex = 2,
                                title = "4K",
                                subtitle = "Stream / DL",
                                containerColor = if (has4K) Color(0xFF8B5CF6) else Color.Gray.copy(alpha = 0.4f),
                                contentColor = Color.White,
                                isAvailable = has4K,
                                onClick = {
                                    menuState = CardActionMenuState.CLOSED
                                    handleMagnet(link.magnet4K)
                                }
                            )
                        }
                    }
                }
            }
        }

        // ========================================================
        // 2. Native Material 3 UI Metadata Container
        // ========================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (playUrl != null) {
                        onPlay(playUrl)
                    } else if (link.galleryUrls.isNotEmpty()) {
                        onOpenGallery()
                    } else {
                        onEdit()
                    }
                },
            color = palette.cardBg
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left & Center Column: [Top: Actor | Studio] and [Bottom: Title | Date]
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: Top-Left (Actor) | Top-Right (Studio)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val actorGradient = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                        Text(
                            text = actorsDisplayName.ifEmpty { "Scene" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                brush = actorGradient,
                                letterSpacing = 0.15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = studioName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Row 2: Bottom-Left (Title) | Bottom-Right (Date)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = link.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Normal,
                                lineHeight = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = displayDate,
                            style = MaterialTheme.typography.bodySmall.copy(
                                letterSpacing = 0.25.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaggeredCircularButton(
    delayIndex: Int,
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delayIndex.let { kotlinx.coroutines.delay((it * 30).toLong()) }
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "btn_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(120),
        label = "btn_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = containerColor,
            shadowElevation = 6.dp,
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun StaggeredQualityButton(
    delayIndex: Int,
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    isAvailable: Boolean,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delayIndex.let { kotlinx.coroutines.delay((it * 30).toLong()) }
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "btn_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(120),
        label = "btn_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = containerColor,
            shadowElevation = 6.dp,
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = contentColor
                )
            }
        }
        Text(
            text = if (isAvailable) subtitle else "N/A",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isAvailable) Color.White else Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

fun formatDisplayDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "Jul 31, 2026"
    }
}
