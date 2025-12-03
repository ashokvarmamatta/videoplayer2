package com.applications.player.presentation.videoplayer

import PlaylistSelectionDialog
import PlaylistsViewModel
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import com.applications.player.R
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
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.applications.player.model.Video
import com.applications.player.presentation.homeScreen.RenameVideoDialog
import com.applications.player.presentation.settings.SettingsScreenState
import com.applications.player.presentation.settings.SettingsViewModel
import com.applications.player.presentation.videoMerging.VideoMergingActivity
import com.applications.player.presentation.videosOfFolder.SelectionDialog
import com.applications.player.presentation.videosplitting.VideoSplittingActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.provider.Settings
import android.database.ContentObserver

import org.koin.androidx.compose.koinViewModel


@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    video: Video,
    viewModel: VideoPlayerViewModel = koinViewModel(),
    onEnterPipMode: () -> Unit,
    onFinishActivity:() -> Unit,
    onSettingsChanged: (SettingsScreenState) -> Unit
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


    var currentOrientation by remember { mutableStateOf(settings.defaultScreenOrientation) }
    LaunchedEffect(settings.defaultScreenOrientation, isFullScreen) {
        if (!isFullScreen) {
            currentOrientation = settings.defaultScreenOrientation
        }
    }

    val activity = LocalContext.current as? Activity

    // You need the coroutine scope to launch the long-press job
    val coroutineScope = rememberCoroutineScope()



    //for playlist
    val playlistsViewModel: PlaylistsViewModel = org.koin.compose.viewmodel.koinViewModel()
    val videoToAddToPlaylist by playlistsViewModel.videoToAddToPlaylist.collectAsState()
    val playlists by playlistsViewModel.playlists.collectAsState()
    val selectedPlaylist by playlistsViewModel.selectedPlaylist.collectAsState()

    // START: Add these lines
    var brightness by remember {
        mutableStateOf(
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        )
    }




    LaunchedEffect(settings) {
        onSettingsChanged(settings)
    }


    // START: Modify the DisposableEffect
    DisposableEffect(settings.rememberBrightness) { // Keyed to the setting now
        val contentResolver = context.contentResolver
        val brightnessUri = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS)

        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                val newBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                if (brightness != newBrightness) {
                    brightness = newBrightness
                    Log.d("BrightnessTracker", "Brightness changed to: $newBrightness")

                    // --- THIS IS THE NEW LOGIC ---
                    if (settings.rememberBrightness) {
                        viewModel.onBrightnessChanged(newBrightness)
                    }
                    val activity = context as? Activity
                    if (activity != null) {
                        viewModel.applyRememberedBrightness(activity)
                    }
                    // -----------------------------
                }
            }
        }

        // Only register the observer if the setting is enabled
        if (settings.rememberBrightness) {
            contentResolver.registerContentObserver(brightnessUri, true, observer)
            Log.d("BrightnessTracker", "Brightness observer registered.")
        }

        // Clean up the observer when the effect disposes
        onDispose {
            if (settings.rememberBrightness) {
                contentResolver.unregisterContentObserver(observer)
                Log.d("BrightnessTracker", "Brightness observer unregistered.")
            }
        }
    }


    SystemUiAndOrientationManager(
        isFullScreen = isFullScreen,
        orientation = currentOrientation
    )




    DisposableEffect(video) {
        viewModel.initPlayer(video)

        val activity = context as? Activity
        if (activity != null) {
            viewModel.applyRememberedBrightness(activity)
        }
        onDispose { viewModel.releasePlayer() }
    }

    LaunchedEffect(
        showControls,
        state.isPlaying,
        userInteractedWithControls,
        isScreenLocked,
        state.isInPipMode
    ) {
        if (!state.isInPipMode && !isScreenLocked && showControls && state.isPlaying && !userInteractedWithControls) {
            delay(3000)
            showControls = false
        }
    }


    LaunchedEffect(state.isVideoDeleted, state.isVideoRenamed) {
        if (state.isVideoDeleted || state.isVideoRenamed) {
            onFinishActivity()
            viewModel.onActionHandled() // Reset the state in the ViewModel
        }
    }

    val subtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                viewModel.addSubtitle(it)
                Toast.makeText(context, "Subtitle added", Toast.LENGTH_SHORT).show()
                viewModel.togglePlayPause(true)
            }
        }
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(
                settings.doubleTapToFastForwardAndRewind,
                settings.longPressToPlayAt2xSpeed,
                isScreenLocked
            ) {
                detectTapGestures(
                    onTap = { if (!isScreenLocked) showControls = !showControls },
                    onDoubleTap = { offset ->
                        if (isScreenLocked || !settings.doubleTapToFastForwardAndRewind) return@detectTapGestures
                        if (offset.x < size.width / 2) {
                            viewModel.rewind()
                        } else {
                            viewModel.fastForward()
                        }
                    },
                    // --- FIX IS HERE ---
                    onLongPress = {
                        if (isScreenLocked || !settings.longPressToPlayAt2xSpeed) return@detectTapGestures

                        // Launch a coroutine to handle the press-and-hold logic
                        coroutineScope.launch {
                            viewModel.setPlaybackSpeed(2.0f, isTemporary = true)

                            try {
                                // This is the correct way to call a suspend function here
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.any { it.pressed.not() }) {
                                            break // Finger lifted
                                        }
                                    }
                                }
                            } finally {
                                // Revert speed when the coroutine is cancelled or the loop breaks
                                viewModel.setPlaybackSpeed(1.0f)
                            }
                        }
                    }
                )
            }
    ) {
        // ... The rest of your file is correct and doesn't need to change
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
                onTap = {
                    Toast.makeText(context, "Long Press to Unlock", Toast.LENGTH_LONG).show()
                }
            )
        }

        val controlsVisible = !state.isInPipMode && !isScreenLocked && showControls

        AnimatedVisibility(
            visible = controlsVisible,            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = { Text(video.name, color = Color.White, maxLines = 1) },    navigationIcon = {
                    IconButton(onClick = { onBackPressedDispatcher?.onBackPressed() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    /*IconButton(onClick = { onFinishActivity() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.fullscreen_exit_),
                            contentDescription = "Close Player",
                            tint = Color.White
                        )
                    }*/
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f))
            )
        }


        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            // --- FIX IS HERE: THIS IS THE ONLY PART THAT NEEDS TO BE CHANGED ---
            CustomPlayerControls(
                state = state,
                viewModel = viewModel,
                onToggleFullscreen = { forceLandscape ->
                    val newFullScreenState = !isFullScreen
                    isFullScreen = newFullScreenState

                    if (newFullScreenState) {
                        if (forceLandscape) {
                            // If forcing landscape, UPDATE THE STATE.
                            currentOrientation = "Landscape"
                        }
                        // If not forcing landscape (e.g., for tall videos),
                        // the orientation is already managed by the `LaunchedEffect`
                        // that keys off `settings.defaultScreenOrientation`, so we do nothing here.
                    } else {
                        // When exiting fullscreen, revert to the default setting orientation by UPDATING THE STATE.
                        currentOrientation = settings.defaultScreenOrientation
                    }
                },
                isFullScreen = isFullScreen,
                onToggleLock = {
                    isScreenLocked = !isScreenLocked
                    if (isScreenLocked) {
                        showControls = false
                    }
                },
                isScreenLocked = isScreenLocked,
                onEnterPipMode = onEnterPipMode,
                onSelectSubtitle = {
                    viewModel.togglePlayPause(false)
                    Log.e("", "get subtitle file clicked")
                    subtitleLauncher.launch(arrayOf("application/x-subrip", "text/vtt"))
                },
                onUserInteract = { userInteractedWithControls = it },
                onTogglePlayPause = viewModel::togglePlayPause,
                onAddToPlaylist = {
                    viewModel.togglePlayPause(false)
                    playlistsViewModel.onAddToPlaylistRequest(video)
                },
                onRename = {
                    viewModel.togglePlayPause(false)
                    viewModel.onVideoRenameSelected(video)
                },
                onDelete = {
                    viewModel.togglePlayPause(false)
                    viewModel.deleteVideo(video)
                    Toast.makeText(context, "Delete request sent for ${video.name}.", Toast.LENGTH_SHORT).show()
                    onBackPressedDispatcher?.onBackPressed()
                }
            )
        }

        if (state.duration == 0L && state.error == null && !state.isInPipMode) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        state.error?.let {
            Text(
                text = "Error: $it",
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (state.videoToRename != null) {
            RenameVideoDialog(
                video = state.videoToRename!!,
                onDismiss = { viewModel.onVideoRenameSelected(null) },
                onRename = { video, newName ->
                    viewModel.renameVideo(video, newName)
                }
            )
        }

        videoToAddToPlaylist?.let { video ->
            PlaylistSelectionDialog(
                video = video,
                playlists = playlists,
                onDismiss = { playlistsViewModel.onDismissPlaylistDialog() },
                onPlaylistSelected = { playlist, videoToAdd ->
                    playlistsViewModel.addVideoToPlaylist(playlist, videoToAdd)
                    Toast.makeText(context, "Video Added to Playlist", Toast.LENGTH_SHORT).show()
                },
                onAddNewPlaylist = { playlistName ->
                    playlistsViewModel.createNewPlaylist(playlistName)
                }
            )
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
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .size(48.dp)
        )
    }
}

@Composable
fun SystemUiAndOrientationManager(
    isFullScreen: Boolean,
    orientation: String
) {
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(activity, isFullScreen, orientation) {
        activity?.let {
            val window = it.window
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

            if (isFullScreen) {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }

            it.requestedOrientation = when (orientation) {
                "Landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                "Portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
}


@OptIn(UnstableApi::class)
@Composable
fun ChooseVideoFunctionality(
    selectedVideo: Video?,
    context: Context,
    videoPlayerViewModel: VideoPlayerViewModel,
    playlistsViewModel: PlaylistsViewModel,
    onBackPressed: () -> Unit
) {
    if (selectedVideo != null)

        SelectionDialog(
            video = selectedVideo,
            onDismiss = { videoPlayerViewModel.onShowDIalog(false)
                videoPlayerViewModel.togglePlayPause()
            },
            onPlayClick = {
                videoPlayerViewModel.onShowDIalog(false)

            },
            onAddToPlayListChecked = { video ->
                videoPlayerViewModel.onShowDIalog(false)
                playlistsViewModel.onAddToPlaylistRequest(video)
            },
            onSplitClick = { video ->
                videoPlayerViewModel.onShowDIalog(false)
                val intent = Intent(context, VideoSplittingActivity::class.java)
                intent.putExtra("videoUri", video.uri)
                startActivity(context,intent,null)

            },
            onRenameCLicked = { video ->
                videoPlayerViewModel.onShowDIalog(false)
                videoPlayerViewModel.onVideoRenameSelected(video)
            },
            onMergeClick = { video ->
                videoPlayerViewModel.onShowDIalog(false)
                val intent = Intent(context, VideoMergingActivity::class.java)
                intent.putExtra("videoUri", video.uri)
                startActivity(context,intent,null)
            },
            onDeleteClick = { video ->
                videoPlayerViewModel.onShowDIalog(false)
                videoPlayerViewModel.deleteVideo(video)
                Toast.makeText(context, "Delete request sent for ${video.name}.", Toast.LENGTH_SHORT).show()
                onBackPressed()
            }
        )
}
