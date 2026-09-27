package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import compose.icons.FeatherIcons
import compose.icons.feathericons.Briefcase
import compose.icons.feathericons.Camera
import compose.icons.feathericons.Film
import compose.icons.feathericons.Menu
import compose.icons.feathericons.Settings
import compose.icons.feathericons.Shield
import compose.icons.feathericons.Sliders
import compose.icons.feathericons.Tv
import compose.icons.feathericons.Users
import com.example.ui.ActiveVideoPlayback
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.components.ExoPlayerOverlay
import com.example.ui.components.GoPlayer
import com.example.ui.components.PhotosetLightbox
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(viewModel: MainViewModel) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentScreen by viewModel.screenState.collectAsStateWithLifecycle()
    val activeVideo by viewModel.activeVideo.collectAsStateWithLifecycle()
    val activeLightbox by viewModel.activeLightbox.collectAsStateWithLifecycle()
    val resolvingStatus by viewModel.resolvingVideoStatus.collectAsStateWithLifecycle()
    val videoResolutionError by viewModel.videoResolutionError.collectAsStateWithLifecycle()

    // Handle back button press
    BackHandler(enabled = true) {
        if (activeLightbox != null) {
            viewModel.closeLightbox()
        } else if (activeVideo != null) {
            viewModel.closeVideo()
        } else if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            val handled = viewModel.navigateBack()
            if (!handled) {
                // At root, let system handle exit
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen is ScreenState.Home,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = palette.surface,
                drawerContentColor = palette.textPrimary,
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accent,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(FeatherIcons.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text("Goony", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = palette.textPrimary)
                            Text("Media Hub & Player", fontSize = 12.sp, color = palette.textMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = palette.border)
                    Spacer(modifier = Modifier.height(12.dp))

                    NavigationDrawerItem(
                        icon = { Icon(FeatherIcons.Film, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text("Scenes Vault") },
                        selected = currentScreen is ScreenState.Home,
                        onClick = {
                            viewModel.navigateTo(ScreenState.Home)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.2f),
                            selectedTextColor = accent,
                            selectedIconColor = accent
                        )
                    )

                    NavigationDrawerItem(
                        icon = { Icon(FeatherIcons.Users, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text("Actors Directory") },
                        selected = currentScreen is ScreenState.Actors,
                        onClick = {
                            viewModel.navigateTo(ScreenState.Actors)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.2f),
                            selectedTextColor = accent,
                            selectedIconColor = accent
                        )
                    )

                    NavigationDrawerItem(
                        icon = { Icon(FeatherIcons.Briefcase, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text("Studios") },
                        selected = currentScreen is ScreenState.Studios,
                        onClick = {
                            viewModel.navigateTo(ScreenState.Studios)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.2f),
                            selectedTextColor = accent,
                            selectedIconColor = accent
                        )
                    )

                    NavigationDrawerItem(
                        icon = { Icon(FeatherIcons.Tv, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text("Hanime (DASH 4K)") },
                        selected = currentScreen is ScreenState.Hanime,
                        onClick = {
                            viewModel.navigateTo(ScreenState.Hanime)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.2f),
                            selectedTextColor = accent,
                            selectedIconColor = accent
                        )
                    )

                    NavigationDrawerItem(
                        icon = { Icon(FeatherIcons.Camera, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text("Creators (OnlyFans/Fansly)") },
                        selected = currentScreen is ScreenState.Coomers,
                        onClick = {
                            viewModel.navigateTo(ScreenState.Coomers)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.2f),
                            selectedTextColor = accent,
                            selectedIconColor = accent
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider(color = palette.border)
                    Spacer(modifier = Modifier.height(12.dp))

                    NavigationDrawerItem(
                        icon = { Icon(FeatherIcons.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text("Settings & Sync") },
                        selected = currentScreen is ScreenState.Settings,
                        onClick = {
                            viewModel.navigateTo(ScreenState.Settings)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.2f),
                            selectedTextColor = accent,
                            selectedIconColor = accent
                        )
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Active Screen View
                Box(modifier = Modifier.fillMaxSize()) {
                    when (val screen = currentScreen) {
                        is ScreenState.Home -> HomeScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                        is ScreenState.AddEditLink -> AddEditLinkScreen(viewModel, screen.linkId)
                        is ScreenState.Actors -> ActorManagementScreen(viewModel)
                        is ScreenState.AddEditActor -> ActorManagementScreen(viewModel)
                        is ScreenState.ActorScenes -> HomeScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                        is ScreenState.Studios -> StudioManagementScreen(viewModel)
                        is ScreenState.AddEditStudio -> StudioManagementScreen(viewModel)
                        is ScreenState.StudioScenes -> HomeScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                        is ScreenState.Coomers -> CoomerManagementScreen(viewModel)
                        is ScreenState.AddEditCoomer -> CoomerManagementScreen(viewModel)
                        is ScreenState.CoomerDetail -> CoomerDetailScreen(viewModel, screen.coomerId)
                        is ScreenState.Hanime -> HanimeManagementScreen(viewModel)
                        is ScreenState.AddEditHanime -> HanimeManagementScreen(viewModel)
                        is ScreenState.HanimeDetail -> HanimeDetailScreen(viewModel, screen.hanimeId)
                        is ScreenState.PhotosetViewer -> PhotosetViewerScreen(viewModel, screen.title, screen.images, screen.initialIndex)
                        is ScreenState.Settings -> SettingsScreen(viewModel)
                    }
                }
            }

            // GoPlayer / ExoPlayer Video Player Overlay
            activeVideo?.let { video ->
                GoPlayer(
                    title = video.title,
                    qualities = video.qualities,
                    subtitles = video.subtitles,
                    defaultHeaders = video.headers,
                    onClose = { viewModel.closeVideo() }
                )
            }

            // High-Res Photoset Lightbox Overlay
            activeLightbox?.let { (images, startIndex) ->
                PhotosetLightbox(
                    images = images,
                    initialIndex = startIndex,
                    onClose = { viewModel.closeLightbox() }
                )
            }

            // Video Resolving / Debrid Progress Overlay
            resolvingStatus?.let { statusText ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .padding(20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(44.dp),
                                color = accent,
                                strokeWidth = 3.5.dp
                            )
                            Text(
                                text = statusText,
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Video Resolution / Debrid Error Dialog
            videoResolutionError?.let { errText ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissVideoError() },
                    icon = {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Stream Playback Error",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Text(
                            text = errText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = palette.textSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.dismissVideoError() },
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}
