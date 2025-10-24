package com.applications.player.presentation.videosplitting

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// --- Assumption: Define the necessary state and helper function structure ---

// This should ideally live in the ViewModel file, but is included here for context

/**
 * Helper function to format milliseconds to "mm:ss" string.
 */
fun formatTime(milliseconds: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

// --- VideoSplittingScreen Implementation ---

@Composable
fun VideoSplittingScreen(
    videoUri: Uri,
    // Assuming you inject this via Koin/Hilt in a real app,
    // but using viewModel() for Compose preview
    viewModel: VideoSplittingViewModel = viewModel()
) {
    val context = LocalContext.current
    val videoSplittingState by viewModel.state.collectAsState()

    // State for local playback and trimming
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoDuration by remember { mutableLongStateOf(0L) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var trimRange by remember { mutableStateOf(0f..0f) }

    // --- 1. ExoPlayer Setup and Lifecycle Management ---
    DisposableEffect(videoUri) {
        val player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        videoDuration = this@apply.duration.coerceAtLeast(0)
                        trimRange = 0f..videoDuration.toFloat()
                    }
                }
            })
            prepare()
        }
        exoPlayer = player

        // Start observing playback position
        val updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                currentPosition = player.currentPosition.coerceAtLeast(0)
                // Loop video preview within the trimming range
                val startMs = trimRange.start.toLong()
                val endMs = trimRange.endInclusive.toLong()

                if (currentPosition >= endMs) {
                    player.seekTo(startMs)
                }
                delay(100)
            }
        }

        onDispose {
            updateJob.cancel()
            player.release()
            exoPlayer = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Video Player Preview using AndroidView and ExoPlayer's StyledPlayerView
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp) // Fixed height for consistency
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx: Context ->
                    // FIX 1: Create PlayerView without setting player immediately
                    PlayerView(ctx)
                },
                update = { view ->
                    // FIX 1: Set the player here. This runs whenever exoPlayer state changes
                    // (i.e., when it goes from null to the actual player instance),
                    // ensuring playback starts.
                    view.player = exoPlayer
                    view.useController = true
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Splitting State UI ---
        if (videoSplittingState.isSplitting) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Splitting Video...")
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = videoSplittingState.progress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (videoSplittingState.splitVideoUri == null) {
            // --- 2. User-Friendly Trimming Controls ---
            VideoSplittingControls(
                videoDuration = videoDuration,
                trimRange = trimRange,
                currentPosition = currentPosition,
                onRangeChange = { newRange ->
                    trimRange = newRange

                    val startMs = newRange.start.toLong()
                    val endMs = newRange.endInclusive.toLong()

                    // FIX 2: Only seek if the current position is outside the new trim range.
                    // This prevents constant seeking to the start time while dragging the end time.
                    if (currentPosition < startMs || currentPosition > endMs) {
                        exoPlayer?.seekTo(startMs)
                    }
                },
                onSplitClick = { start, end ->
                    exoPlayer?.pause() // Pause playback before splitting
                    viewModel.splitVideo(context, videoUri, start, end)
                }
            )
        } else {
            // Display split video ready message and controls
            Text("Split video saved!", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { /* TODO: Implement a way to play the split video */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Play Split Video")
            }
        }
    }
}

// --- 3. Refactored Trimming Controls Composable (Range Slider) ---

@Composable
fun VideoSplittingControls(
    videoDuration: Long,
    trimRange: ClosedFloatingPointRange<Float>,
    currentPosition: Long,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onSplitClick: (Long, Long) -> Unit
) {
    if (videoDuration == 0L) {
        Text("Loading video duration...", modifier = Modifier.padding(16.dp))
        return
    }

    val maxDurationFloat = videoDuration.toFloat()
    val startMs = trimRange.start.toLong()
    val endMs = trimRange.endInclusive.toLong()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Current Playback Position
        Text(
            "Current Position: ${formatTime(currentPosition)}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Trim Range Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Start: ${formatTime(startMs)}", color = MaterialTheme.colorScheme.primary)
            Text("End: ${formatTime(endMs)}", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Range Slider for Trimming
        RangeSlider(
            value = trimRange,
            onValueChange = onRangeChange,
            valueRange = 0f..maxDurationFloat,
            steps = (maxDurationFloat / 1000).toInt() // Steps per second
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSplitClick(startMs, endMs) },
            enabled = endMs > startMs && endMs <= videoDuration
        ) {
            Text("Split Video Segment (${formatTime(endMs - startMs)})")
        }
    }
}
