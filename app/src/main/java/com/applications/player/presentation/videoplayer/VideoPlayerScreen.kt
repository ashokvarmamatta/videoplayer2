package com.applications.player.presentation.videoplayer

import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.applications.player.model.Video
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.media3.ui.PlayerView
import android.widget.Toast
import androidx.compose.material.icons.filled.Clear
import com.applications.player.presentation.videosOfFolder.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    video: Video,
    viewModel: VideoPlayerViewModel = koinViewModel(),
    onEnterPipMode: () -> Unit // PiP callback from Activity
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    var showControls by remember { mutableStateOf(true) }
    var userInteractedWithControls by remember { mutableStateOf(false) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }

    SystemUiAndOrientationManager(isFullScreen = isFullScreen)

    DisposableEffect(video) {
        viewModel.initPlayer(video)
        onDispose { viewModel.releasePlayer() }
    }

    // Auto-hide controls
    LaunchedEffect(showControls, state.isPlaying, userInteractedWithControls, isScreenLocked, state.isInPipMode) {
        if (!state.isInPipMode && !isScreenLocked && showControls && state.isPlaying && !userInteractedWithControls) {
            delay(3000)
            showControls = false
        }
    }

    // Main UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = !isScreenLocked && !state.isInPipMode) { showControls = !showControls }
    ) {
        // 1. AndroidView for PlayerView (Video Surface)
        AndroidView<PlayerView>(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = null
                    useController = false
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = viewModel.player
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- LOCKED STATE OVERLAY ---
        if (isScreenLocked && !state.isInPipMode) {
            UnlockButton(
                onLongPress = {
                    isScreenLocked = false
                    showControls = true
                    Toast.makeText(context, "Screen Unlocked", Toast.LENGTH_SHORT).show()
                },
                onTap = {
                    Toast.makeText(context, "Long Press to Unlock", Toast.LENGTH_LONG).show()
                }
            )
        }

        // --- CONTROL VISIBILITY RULES ---

        // utilityControlsVisible: For Top Bar, FF/RW zones, Slider, and Options. (Hidden in PiP)
        val utilityControlsVisible = !state.isInPipMode && !isScreenLocked && showControls

        // playPauseVisible: For the large center Play/Pause button. (Visible in PiP or when showControls is true)
        val playPauseVisible = !isScreenLocked && (state.isInPipMode || showControls)


        // 2. Custom Top Bar (Utility)
        AnimatedVisibility(
            visible = utilityControlsVisible && !isFullScreen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = { Text(video.name, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { onBackPressedDispatcher?.onBackPressed() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f))
            )
        }

        // 3. Fast Forward / Rewind Overlay Buttons (Utility)
        AnimatedVisibility(
            visible = utilityControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind Zone
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clickable { viewModel.rewind() }
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowLeft,
                        contentDescription = "Rewind",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Fast Forward Zone
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clickable { viewModel.fastForward() }
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Fast Forward",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        // 4. Custom Play/Pause Button (ISOLATED - FIXED FOR PiP)
        AnimatedVisibility(
            visible = playPauseVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            // --- FIX 1 & 2: Change alignment based on PiP state ---
            modifier = Modifier.align(
                if (state.isInPipMode) Alignment.BottomCenter else Alignment.Center
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(if (state.isInPipMode) 0.5f else 1f) // Restrict size slightly in PiP mode
                    .padding(bottom = if (state.isInPipMode) 16.dp else 0.dp), // Add padding at bottom in PiP
                contentAlignment = Alignment.Center
            ) {
                // --- FIX 3: Change size based on PiP state ---
                val iconSize = if (state.isInPipMode) 48.dp else 96.dp

                IconButton(onClick = { viewModel.togglePlayPause() }) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Clear else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }


        // 5. Custom Bottom Controls Bar (Utility - includes Slider and Options)
        AnimatedVisibility(
            visible = utilityControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CustomPlayerControls(
                state = state,
                viewModel = viewModel,
                onToggleFullscreen = { isFullScreen = !isFullScreen },
                isFullScreen = isFullScreen,
                onToggleLock = {
                    isScreenLocked = !isScreenLocked
                    if (isScreenLocked) {
                        Toast.makeText(context, "Long Press to Unlock", Toast.LENGTH_LONG).show()
                    }
                },
                isScreenLocked = isScreenLocked,
                onEnterPipMode = onEnterPipMode,
                onUserInteract = { userInteractedWithControls = it }
            )
        }

        // 6. Optional Loading Indicator
        if (state.duration == 0L && state.error == null && !state.isPlaying && !state.isInPipMode) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // 7. Optional Error Display
        state.error?.let {
            Text(
                text = "Error: $it",
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}


// New Unlock Button Composable (Long press to unlock)
@Composable
fun UnlockButton(
    onLongPress: () -> Unit,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { onTap() }
                )
            }
            .background(Color.Black.copy(alpha = 0.1f)),
        contentAlignment = Alignment.CenterStart
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Unlock",
            tint = Color.White,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .size(48.dp)
        )
    }
}

// System UI and Orientation Manager (for fullscreen)
@Composable
fun SystemUiAndOrientationManager(isFullScreen: Boolean) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return
    val window = activity.window
    val insetsController = remember(window) { WindowCompat.getInsetsController(window, window.decorView) }

    // 1. STATE CHANGE: Use LaunchedEffect to manage state transitions without premature cleanup
    LaunchedEffect(isFullScreen) {
        if (isFullScreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // 2. CLEANUP: Use DisposableEffect(Unit) to ONLY run cleanup when the Composable is destroyed
    DisposableEffect(Unit) {
        val initialOrientation = activity.requestedOrientation
        val initialSystemBarsBehavior = insetsController.systemBarsBehavior

        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = initialSystemBarsBehavior
            activity.requestedOrientation = initialOrientation
        }
    }
}