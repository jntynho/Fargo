package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.network.StreamQuality
import com.example.network.SubtitleTrack
import com.example.ui.theme.LocalAccentColor
import kotlinx.coroutines.delay

private const val BROWSER_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GoPlayer(
    title: String,
    qualities: List<StreamQuality>,
    subtitles: List<SubtitleTrack> = emptyList(),
    defaultHeaders: Map<String, String> = emptyMap(),
    onClose: () -> Unit
) {
    ExoPlayerOverlay(
        title = title,
        qualities = qualities,
        subtitles = subtitles,
        defaultHeaders = defaultHeaders,
        onClose = onClose
    )
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExoPlayerOverlay(
    title: String,
    qualities: List<StreamQuality>,
    subtitles: List<SubtitleTrack> = emptyList(),
    defaultHeaders: Map<String, String> = emptyMap(),
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var selectedQuality by remember(qualities) {
        mutableStateOf(qualities.firstOrNull { it.isDefault } ?: qualities.firstOrNull())
    }
    var selectedSubtitle by remember(subtitles) { mutableStateOf<SubtitleTrack?>(null) }

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPos by remember { mutableLongStateOf(0L) }
    var videoResolution by remember { mutableStateOf("Detecting...") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorDetails by remember { mutableStateOf<String?>(null) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    var showBottomSheetMenu by remember { mutableStateOf(false) }
    var bottomSheetTab by remember { mutableStateOf(0) } // 0: Quality, 1: Subtitle, 2: Diagnostics

    // Helper to toggle landscape/portrait
    fun toggleOrientation() {
        activity?.let { act ->
            if (isLandscape) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
    }

    // Auto-hide controls after 4.5 seconds of inactivity
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4500)
            showControls = false
        }
    }

    // Handle system bars and screen orientation cleanup on dispose
    DisposableEffect(activity) {
        activity?.let { act ->
            val windowInsetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val windowInsetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Back button behavior inside player
    BackHandler {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            onClose()
        }
    }

    val activeHeaders = remember(selectedQuality, defaultHeaders) {
        val streamHeaders = selectedQuality?.headers ?: emptyMap()
        defaultHeaders + streamHeaders
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .build().apply {
                playWhenReady = true
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        errorMessage = null
                        errorDetails = null
                        duration = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                    }
                    Player.STATE_IDLE -> isBuffering = false
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoResolution = "${videoSize.width}x${videoSize.height}"
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                errorMessage = "Playback Error (${error.errorCodeName})"
                errorDetails = error.message ?: "Failed to stream or decode video."
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Periodic time tracker for smooth seekbar updates
    LaunchedEffect(exoPlayer, selectedQuality) {
        while (true) {
            if (exoPlayer.playbackState == Player.STATE_READY || exoPlayer.isPlaying) {
                currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                bufferedPos = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                duration = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(400)
        }
    }

    // Reconfigure media source on quality or subtitle change
    LaunchedEffect(selectedQuality, selectedSubtitle) {
        val quality = selectedQuality ?: return@LaunchedEffect
        val url = quality.url.trim()

        if (url.isBlank()) {
            errorMessage = "Invalid Stream URL"
            errorDetails = "The provided media link is empty."
            return@LaunchedEffect
        }

        errorMessage = null
        errorDetails = null
        isBuffering = true

        try {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(activeHeaders["User-Agent"] ?: BROWSER_UA)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)

            val customHeaders = HashMap<String, String>()
            activeHeaders.forEach { (k, v) ->
                if (!k.equals("User-Agent", ignoreCase = true)) {
                    customHeaders[k] = v
                }
            }
            if (customHeaders.isNotEmpty()) {
                httpFactory.setDefaultRequestProperties(customHeaders)
            }

            val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

            val mediaItemBuilder = MediaItem.Builder().setUri(url)

            when {
                url.contains(".mpd", ignoreCase = true) || quality.format.contains("dash", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
                }
                url.contains(".m3u8", ignoreCase = true) || quality.format.contains("hls", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                else -> {
                    mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
                }
            }

            selectedSubtitle?.let { sub ->
                val subConfig = MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(sub.url))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(sub.language)
                    .setLabel(sub.label)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
            }

            val mediaItem = mediaItemBuilder.build()
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

            val resumePosition = currentPos
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            if (resumePosition > 0) {
                exoPlayer.seekTo(resumePosition)
            }
            exoPlayer.play()
        } catch (e: Exception) {
            errorMessage = "Playback Setup Failed"
            errorDetails = e.message ?: "Could not build media source."
            isBuffering = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                    },
                    onDoubleTap = { offset ->
                        val screenWidth = size.width
                        if (offset.x < screenWidth / 2) {
                            // Double tap left -> rewind 10s
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L))
                        } else {
                            // Double tap right -> fast forward 10s
                            exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                        }
                    }
                )
            }
            .testTag("video_player_overlay")
    ) {
        // 1. AndroidView ExoPlayer Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = resizeMode
                playerView.keepScreenOn = true
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Center Buffering Indicator
        if (isBuffering && errorMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(54.dp),
                    color = LocalAccentColor.current,
                    strokeWidth = 4.dp
                )
            }
        }

        // 3. Error Overlay
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = errorMessage ?: "Playback Error",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = errorDetails ?: "Unable to stream media content.",
                            color = Color(0xFFD1D5DB),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    bottomSheetTab = 2
                                    showBottomSheetMenu = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Info")
                            }

                            Button(
                                onClick = {
                                    errorMessage = null
                                    errorDetails = null
                                    isBuffering = true
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LocalAccentColor.current),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Retry")
                            }

                            FilledTonalButton(
                                onClick = onClose,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }

        // 4. Native Overlay Controls (YouTube/Netflix Clean Style)
        AnimatedVisibility(
            visible = showControls && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Player",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    // Aspect Ratio Button (Fit / Zoom / Stretch)
                    IconButton(
                        onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AspectRatio,
                            contentDescription = "Aspect Ratio",
                            tint = Color.White
                        )
                    }

                    // Settings & Quality Sheet Trigger
                    IconButton(
                        onClick = {
                            bottomSheetTab = 0
                            showBottomSheetMenu = true
                        },
                        modifier = Modifier.testTag("player_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Player Settings",
                            tint = Color.White
                        )
                    }
                }

                // Center Clean Media Controls (10s Rewind | Large Play/Pause | 10s Forward)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L)) },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Surface(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .size(70.dp)
                            .testTag("play_pause_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)) },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Bottom Timeline & Orientation Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // Time and info row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatDuration(currentPos),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " / ${formatDuration(duration)}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = selectedQuality?.quality ?: "Auto",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Fullscreen / Screen Rotation Toggle Button
                            IconButton(
                                onClick = { toggleOrientation() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLandscape) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                                    contentDescription = "Rotate Screen",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Native Video Slider
                    Slider(
                        value = if (duration > 0) (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f,
                        onValueChange = { percent ->
                            val seekTo = (percent * duration).toLong()
                            exoPlayer.seekTo(seekTo)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = LocalAccentColor.current,
                            activeTrackColor = LocalAccentColor.current,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_seekbar")
                    )
                }
            }
        }

        // 5. Native Modal Bottom Sheet for Quality, Subtitles & Info (Unifies multiple popups)
        if (showBottomSheetMenu) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheetMenu = false },
                containerColor = Color(0xFF1E1E24),
                contentColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // BottomSheet Tab Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = bottomSheetTab == 0,
                            onClick = { bottomSheetTab = 0 },
                            label = { Text("Quality") },
                            leadingIcon = { Icon(Icons.Outlined.HighQuality, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LocalAccentColor.current,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )

                        if (subtitles.isNotEmpty()) {
                            FilterChip(
                                selected = bottomSheetTab == 1,
                                onClick = { bottomSheetTab = 1 },
                                label = { Text("Subtitles") },
                                leadingIcon = { Icon(Icons.Outlined.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LocalAccentColor.current,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                        }

                        FilterChip(
                            selected = bottomSheetTab == 2,
                            onClick = { bottomSheetTab = 2 },
                            label = { Text("Diagnostics") },
                            leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LocalAccentColor.current,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    when (bottomSheetTab) {
                        0 -> {
                            // Quality Options
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Stream Quality",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                qualities.forEach { q ->
                                    val isSelected = q == selectedQuality
                                    Surface(
                                        onClick = {
                                            selectedQuality = q
                                            showBottomSheetMenu = false
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) LocalAccentColor.current.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, LocalAccentColor.current) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${q.quality} (${q.format})",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) LocalAccentColor.current else Color.White
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = LocalAccentColor.current,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Subtitles Options
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Subtitles / Captions",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Surface(
                                    onClick = {
                                        selectedSubtitle = null
                                        showBottomSheetMenu = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedSubtitle == null) LocalAccentColor.current.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                    border = if (selectedSubtitle == null) androidx.compose.foundation.BorderStroke(1.5.dp, LocalAccentColor.current) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Off", color = if (selectedSubtitle == null) LocalAccentColor.current else Color.White)
                                        if (selectedSubtitle == null) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = LocalAccentColor.current, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                subtitles.forEach { sub ->
                                    val isSelected = selectedSubtitle == sub
                                    Surface(
                                        onClick = {
                                            selectedSubtitle = sub
                                            showBottomSheetMenu = false
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) LocalAccentColor.current.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, LocalAccentColor.current) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(sub.label, color = if (isSelected) LocalAccentColor.current else Color.White)
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = LocalAccentColor.current, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Diagnostics Info
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DiagnosticsItem("Title", title)
                                DiagnosticsItem("Selected Quality", selectedQuality?.quality ?: "Unknown")
                                DiagnosticsItem("Stream Format", selectedQuality?.format ?: "Unknown")
                                DiagnosticsItem("Resolution", videoResolution)
                                DiagnosticsItem("Position", "${formatDuration(currentPos)} / ${formatDuration(duration)}")
                                DiagnosticsItem("Buffered", "${formatDuration(bufferedPos)} (${if (duration > 0) (bufferedPos * 100 / duration) else 0}%)")
                                DiagnosticsItem("Playback State", if (isBuffering) "BUFFERING" else if (isPlaying) "PLAYING" else "PAUSED")
                                DiagnosticsItem("Orientation", if (isLandscape) "Landscape Mode" else "Portrait Mode")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
        Text(text = value, fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace)
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
