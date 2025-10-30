package com.applications.player.presentation.videoplayer

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import com.applications.player.R
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.PlayerView
import com.applications.player.model.Video
import com.applications.player.presentation.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    video: Video,
    viewModel: VideoPlayerViewModel = koinViewModel(),
    onEnterPipMode: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settings by settingsViewModel.uiState.collectAsState()

    val context = LocalContext.current
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    var showControls by remember { mutableStateOf(true) }
    var userInteractedWithControls by remember { mutableStateOf(false) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    SystemUiAndOrientationManager(isFullScreen = isFullScreen, orientation = settings.defaultScreenOrientation)

    DisposableEffect(video) {
        viewModel.initPlayer(video)
        onDispose { viewModel.releasePlayer() }
    }

    LaunchedEffect(interactionSource, settings.longPressToPlayAt2xSpeed) {
        if (!settings.longPressToPlayAt2xSpeed) return@LaunchedEffect

        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> viewModel.setPlaybackSpeed(2.0f, isTemporary = true)
                is PressInteraction.Release -> viewModel.setPlaybackSpeed(1.0f) // Revert to normal speed
                is PressInteraction.Cancel -> viewModel.setPlaybackSpeed(1.0f)
            }
        }
    }

    LaunchedEffect(showControls, state.isPlaying, userInteractedWithControls, isScreenLocked, state.isInPipMode) {
        if (!state.isInPipMode && !isScreenLocked && showControls && state.isPlaying && !userInteractedWithControls) {
            delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(settings.doubleTapToFastForwardAndRewind) {
                detectTapGestures(
                    onTap = { if (!isScreenLocked) showControls = !showControls },
                    onDoubleTap = { offset ->
                        if (isScreenLocked || !settings.doubleTapToFastForwardAndRewind) return@detectTapGestures
                        if (offset.x < size.width / 2) {
                            viewModel.rewind()
                        } else {
                            viewModel.fastForward()
                        }
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView -> playerView.player = viewModel.player },
            modifier = Modifier.fillMaxSize()
        )

        if (isScreenLocked && !state.isInPipMode) {
            UnlockButton(
                onLongPress = {
                    isScreenLocked = false
                    showControls = true
                    Toast.makeText(context, "Screen Unlocked", Toast.LENGTH_SHORT).show()
                },
                onTap = { Toast.makeText(context, "Long Press to Unlock", Toast.LENGTH_LONG).show() }
            )
        }

        val controlsVisible = !state.isInPipMode && !isScreenLocked && showControls

        AnimatedVisibility(
            visible = controlsVisible,
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

        AnimatedVisibility(
            visible = !isScreenLocked && showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(if (state.isInPipMode) 48.dp else 72.dp)
            ) {

                Icon(
                    painter = if (state.isPlaying) (painterResource(R.drawable.pause)) else (painterResource(R.drawable.play_arrow)),
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CustomPlayerControls(
                state = state,
                viewModel = viewModel,
                onToggleFullscreen = { isFullScreen = !isFullScreen },
                isFullScreen = isFullScreen,
                onToggleLock = { isScreenLocked = !isScreenLocked },
                isScreenLocked = isScreenLocked,
                onEnterPipMode = onEnterPipMode,
                onUserInteract = { userInteractedWithControls = it }
            )
        }

        if (state.duration == 0L && state.error == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        state.error?.let {
            Text(text = "Error: $it", color = Color.Red, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun UnlockButton(onLongPress: () -> Unit, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongPress() }, onTap = { onTap() })
            }
            .background(Color.Black.copy(alpha = 0.1f)),
        contentAlignment = Alignment.CenterStart
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Unlock",
            tint = Color.White,
            modifier = Modifier.padding(horizontal = 24.dp).size(48.dp)
        )
    }
}

@Composable
fun SystemUiAndOrientationManager(isFullScreen: Boolean, orientation: String) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val window = activity.window
    val insetsController = remember(window) { WindowCompat.getInsetsController(window, window.decorView) }

    LaunchedEffect(isFullScreen, orientation) {
        val requestedOrientation = when (orientation) {
            "Landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "Portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        activity.requestedOrientation = if (isFullScreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else requestedOrientation

        if (isFullScreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }
}
