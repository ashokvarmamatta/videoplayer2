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
import com.applications.player.presentation.settings.SettingsViewModel
import com.applications.player.presentation.videoMerging.VideoMergingActivity
import com.applications.player.presentation.videosOfFolder.SelectionDialog
import com.applications.player.presentation.videosplitting.VideoSplittingActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel


@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    video: Video,
    viewModel: VideoPlayerViewModel = koinViewModel(),
    onEnterPipMode: () -> Unit,
    onFinishActivity:() -> Unit
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

    // You need the coroutine scope to launch the long-press job
    val coroutineScope = rememberCoroutineScope()



    //for playlist
    val playlistsViewModel: PlaylistsViewModel = org.koin.compose.viewmodel.koinViewModel()
    val videoToAddToPlaylist by playlistsViewModel.videoToAddToPlaylist.collectAsState()
    val playlists by playlistsViewModel.playlists.collectAsState()
    val selectedPlaylist by playlistsViewModel.selectedPlaylist.collectAsState()




    SystemUiAndOrientationManager(
        isFullScreen = isFullScreen,
        orientation = settings.defaultScreenOrientation
    )

    DisposableEffect(video) {
        viewModel.initPlayer(video)
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

        // --- CHANGE 1: This UnlockButton should also be hidden in PiP mode ---
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

        // --- CHANGE 2: Top controls visibility is already correct, no change needed here ---
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f))
            )
        }

        // --- CHANGE 3: Center Play/Pause button needs to be hidden in PiP mode ---
        AnimatedVisibility(
            // Condition now includes a check for PiP mode
            visible = !state.isInPipMode && !isScreenLocked && showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(72.dp) // No need to check for PiP size anymore
            ) {
                Icon(
                    painter = if (state.isPlaying) (painterResource(R.drawable.pause)) else (painterResource(
                        R.drawable.play_arrow
                    )),
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // --- CHANGE 4: Bottom controls visibility is already correct, no change needed here ---
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
                onSelectSubtitle = {
                    viewModel.togglePlayPause(false)
                    Log.e("", "get subtitle file clicked")
                    subtitleLauncher.launch(arrayOf("application/x-subrip", "text/vtt"))
                },
                onUserInteract = { userInteractedWithControls = it },
                onOptionsClicked = {
                    viewModel.togglePlayPause(false)
                   viewModel.onShowDIalog(true)
                }
            )
        }

        // --- CHANGE 5: Loading indicator should also be hidden in PiP mode ---
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

        // --- 3. DISPLAY THE DIALOG ---
        if (state.showOptonDialog) {
            ChooseVideoFunctionality(
                selectedVideo = video,
                context = context,
                videoPlayerViewModel = viewModel,
                playlistsViewModel = playlistsViewModel,
                onBackPressed = { onBackPressedDispatcher?.onBackPressed() } // Pass the back press callback

            )
        }

        // --- NEW: Handle the rename dialog ---
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
fun SystemUiAndOrientationManager(isFullScreen: Boolean, orientation: String) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val window = activity.window
    val insetsController =
        remember(window) { WindowCompat.getInsetsController(window, window.decorView) }

    LaunchedEffect(isFullScreen, orientation) {
        val requestedOrientation = when (orientation) {
            "Landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "Portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        activity.requestedOrientation =
            if (isFullScreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else requestedOrientation

        if (isFullScreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
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
                        videoPlayerViewModel.togglePlayPause(true)},
            onPlayClick = { video ->

                val intent = Intent(context, VideoPlayerActivityCompose::class.java)
                intent.putExtra("video", video)
                startActivity(context, intent, null)
            },
            onAddToPlayListChecked = {
                videoPlayerViewModel.onShowDIalog(false)
                playlistsViewModel.onAddToPlaylistRequest(selectedVideo)
                Toast.makeText(context, "Video Added to Playlist", Toast.LENGTH_SHORT).show()
            },
            onRenameCLicked = { video ->
                videoPlayerViewModel.onShowDIalog(false)
                videoPlayerViewModel.onVideoRenameSelected(video) // Show rename dialog

            },
            onSplitClick = { video ->
                videoPlayerViewModel.onShowDIalog(false)
                val intent = Intent(context, VideoSplittingActivity::class.java)
                intent.putExtra("videoUri", video.uri)
                startActivity(context, intent, null)
            },
            onMergeClick = { video ->
                videoPlayerViewModel.onShowDIalog(false)
                val intent = Intent(context, VideoMergingActivity::class.java)
                intent.putExtra("videoUri", video.uri)
                startActivity(context, intent, null)
            },
            // --- NEW: Handle Delete Click ---
            onDeleteClick = { video ->
                videoPlayerViewModel.onShowDIalog(false)
                videoPlayerViewModel.deleteVideo(video) // Corrected function call
                Toast.makeText(context, "Video Deleted Successfully", Toast.LENGTH_SHORT).show()
           onBackPressed()
            }
        )


}
