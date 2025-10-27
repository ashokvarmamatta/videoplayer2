package com.applications.player.presentation.videomerging

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.KeyboardType
import com.applications.player.presentation.videoMerging.VideoMergingViewModel
import java.text.DecimalFormat
import kotlin.math.roundToInt
import kotlinx.coroutines.delay // Needed for smooth playback indicator update

@Composable
fun VideoMergingScreen(
    videoUri: Uri,
    viewModel: VideoMergingViewModel = viewModel()
) {
    val context = LocalContext.current
    val mergeState by viewModel.state.collectAsState()

    // Read composable colors and density outside the drawing scope
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    val localDensity = LocalDensity.current // Used for converting pixels to Dp for offsets

    // --- EXO PLAYER SETUP ---
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = false // Start paused
        }
    }

    // State for live playback position update (for the yellow line indicator)
    var currentPlaybackPosition by remember { mutableLongStateOf(0L) }

    // Coroutine loop to poll and update the player position state
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPlaybackPosition = exoPlayer.currentPosition
            delay(50) // Update every 50ms for smooth UI movement
        }
    }

    // Lifecycle observer to handle pause/play when the composable leaves/enters the screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release() // Release player when screen is disposed
        }
    }
    // --- END EXO PLAYER SETUP ---

    // Local state for UI components
    var startHandlePosition by remember { mutableStateOf(0f) }
    var endHandlePosition by remember { mutableStateOf(0f) }
    var timeBarWidthPx by remember { mutableStateOf(0f) }
    var isPlayingPreview by remember { mutableStateOf(false) }

    // Local string states for manual input (allows non-numeric text without crashing/seeking)
    var startInputText by remember { mutableStateOf("") }
    var endInputText by remember { mutableStateOf("") }

    // Local state to track the previous merging status for the Toast logic
    val previousIsMerging = remember { mutableStateOf(mergeState.isMerging) }

    // --- EFFECT TO SHOW SUCCESS TOAST ---
    LaunchedEffect(mergeState.isMerging) {
        // Check for the transition from merging (true) to not merging (false)
        if (previousIsMerging.value && !mergeState.isMerging) {
            // Assuming that if isMerging flips to false, the operation completed successfully.
            // In a real-world MVVM scenario, the ViewModel would provide a dedicated success/error event.
            Toast.makeText(context, "Video merge completed successfully!", Toast.LENGTH_LONG).show()
        }
        // Update the previous state for the next composition
        previousIsMerging.value = mergeState.isMerging
    }

    // Fetch video duration
    LaunchedEffect(Unit) {
        viewModel.getVideoDuration(context, videoUri)
    }

    // Update VM/handle positions when video duration is known or timebar size changes
    LaunchedEffect(mergeState.videoDurationMs, timeBarWidthPx) {
        if (mergeState.videoDurationMs > 0 && timeBarWidthPx > 0) {
            // Initialize view model times to full duration
            viewModel.setStartTime(0L)
            viewModel.setEndTime(mergeState.videoDurationMs)

            // Initialize local UI handle positions to reflect full duration
            // This ensures the sliders start at the beginning and end of the player
            startHandlePosition = 0f
            endHandlePosition = timeBarWidthPx

            // Initialize local input text
            startInputText = (0L / 1000f).roundToInt().toString()
            endInputText = (mergeState.videoDurationMs / 1000f).roundToInt().toString()
        }
    }

    // Sync local input text state when VM state updates (e.g., from handle dragging)
    LaunchedEffect(mergeState.startTimeMs) {
        startInputText = (mergeState.startTimeMs / 1000f).roundToInt().toString()
        // Seek player to the new start time only when state changes from external sources (handles)
        if (!exoPlayer.isPlaying) {
            val seekTime = if (mergeState.startTimeMs == 0L) 1L else mergeState.startTimeMs
            exoPlayer.seekTo(seekTime)
        }
    }
    LaunchedEffect(mergeState.endTimeMs) {
        endInputText = (mergeState.endTimeMs / 1000f).roundToInt().toString()
    }

    // Auto-pause when playback reaches the end time
    LaunchedEffect(currentPlaybackPosition, isPlayingPreview) {
        if (isPlayingPreview && currentPlaybackPosition >= mergeState.endTimeMs) {
            exoPlayer.pause()
            isPlayingPreview = false
            // Seek back to start position for next preview
            exoPlayer.seekTo(mergeState.startTimeMs)
        }
    }

    // Helper function to format milliseconds into a readable format (e.g., "00:05")
    fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val decimalFormat = DecimalFormat("00")
        return "${decimalFormat.format(minutes)}:${decimalFormat.format(seconds)}"
    }

    // Converts time in milliseconds to position in pixels on the timeline
    fun timeToPx(timeMs: Long): Float {
        // Handle case where duration might be 0 initially to prevent division by zero
        if (mergeState.videoDurationMs == 0L) return 0f
        return (timeMs.toFloat() / mergeState.videoDurationMs.toFloat()) * timeBarWidthPx
    }

    // Converts position in pixels on the timeline to time in milliseconds
    fun pxToTime(positionPx: Float): Long {
        if (timeBarWidthPx == 0f || mergeState.videoDurationMs == 0L) return 0L
        return ((positionPx / timeBarWidthPx) * mergeState.videoDurationMs).toLong()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Video Player Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            // Use AndroidView for the PlayerView (ExoPlayer's UI component)
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // Hide default controls for a cleaner UI
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Play/Pause button overlay
            FloatingActionButton(
                onClick = {
                    if (isPlayingPreview) {
                        exoPlayer.pause()
                        isPlayingPreview = false
                    } else {
                        // Start playing from the selection start time
                        exoPlayer.seekTo(mergeState.startTimeMs)
                        exoPlayer.play()
                        isPlayingPreview = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp),
                containerColor = primaryColor.copy(alpha = 0.8f)
            ) {
                Icon(
                    imageVector = if (isPlayingPreview) Icons.Filled.Clear else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlayingPreview) "Pause Preview" else "Play Preview",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (mergeState.videoDurationMs > 0) {
            // TimeBar with Draggable Handles
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.3f))
                    .onSizeChanged { size ->
                        val newWidth = size.width.toFloat()
                        // Recalculate handle positions based on new width and current time
                        if (timeBarWidthPx != newWidth && timeBarWidthPx > 0) {
                            startHandlePosition = timeToPx(mergeState.startTimeMs)
                            endHandlePosition = timeToPx(mergeState.endTimeMs)
                        }
                        timeBarWidthPx = newWidth
                    }
            ) {
                // Background timeline and Segments
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw full background
                    drawRect(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        size = Size(width = size.width, height = size.height)
                    )

                    // Gap (To be deleted) overlay - using Red
                    val startPx = timeToPx(mergeState.startTimeMs)
                    val endPx = timeToPx(mergeState.endTimeMs)
                    drawRect(
                        color = Color.Red.copy(alpha = 0.6f),
                        topLeft = Offset(startPx, 0f),
                        size = Size(width = endPx - startPx, height = size.height)
                    )

                    // Kept video segment (before gap) - using Primary color
                    drawRect(
                        color = primaryColor.copy(alpha = 0.6f),
                        topLeft = Offset(0f, 0f),
                        size = Size(width = startPx, height = size.height)
                    )

                    // Kept video segment (after gap) - using Primary color
                    drawRect(
                        color = primaryColor.copy(alpha = 0.6f),
                        topLeft = Offset(endPx, 0f),
                        size = Size(width = size.width - endPx, height = size.height)
                    )

                    // Current playback indicator (simple line) - FIX: Drawing inside main canvas
                    val currentPlaybackPx = timeToPx(currentPlaybackPosition)
                    if (currentPlaybackPx in 0f..size.width) {
                        drawLine(
                            color = Color.Yellow,
                            start = Offset(currentPlaybackPx, 0f),
                            end = Offset(currentPlaybackPx, size.height),
                            strokeWidth = 3f
                        )
                    }
                }

                // Start time handle
                Box(
                    modifier = with(localDensity) { // FIX: Use localDensity to convert px (Float) to Dp
                        Modifier
                            .offset(x = startHandlePosition.toDp(), y = 0.dp)
                            .size(12.dp, 50.dp)
                    }
                        .background(onPrimaryContainerColor, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState(onDelta = { delta ->
                                // Ensure drag stays within bounds and before the end handle
                                val newPos = (startHandlePosition + delta).coerceIn(0f, endHandlePosition)
                                startHandlePosition = newPos

                                val newTimeMs = pxToTime(newPos)
                                viewModel.setStartTime(newTimeMs)

                                // Seek player to the new start time
                                exoPlayer.seekTo(newTimeMs)
                                exoPlayer.pause()
                                isPlayingPreview = false
                            })
                        )
                )

                // End time handle
                Box(
                    modifier = with(localDensity) { // FIX: Use localDensity to convert px (Float) to Dp
                        Modifier
                            .offset(x = endHandlePosition.toDp(), y = 0.dp)
                            .size(12.dp, 50.dp)
                    }
                        .background(onPrimaryContainerColor, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState(onDelta = { delta ->
                                // Ensure drag stays within bounds and after the start handle
                                val newPos = (endHandlePosition + delta).coerceIn(startHandlePosition, timeBarWidthPx)
                                endHandlePosition = newPos

                                val newTimeMs = pxToTime(newPos)
                                viewModel.setEndTime(newTimeMs)

                                // Seek player to the new end time (or slightly before)
                                exoPlayer.seekTo(newTimeMs)
                                exoPlayer.pause()
                                isPlayingPreview = false
                            })
                        )
                )
            }

            // Time labels below the bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(mergeState.startTimeMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryColor
                )
                Text(
                    text = formatTime(mergeState.endTimeMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryColor
                )
            }

            // Manual time inputs (now using local string state for better UX)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                OutlinedTextField(
                    value = startInputText, // Bind to local string state
                    onValueChange = { newValue ->
                        startInputText = newValue // Allow any string input
                        val newTimeSec = newValue.toIntOrNull()
                        if (newTimeSec != null) {
                            // Update VM state and handle position if valid number, but no coercion here
                            val newTimeMs = newTimeSec * 1000L
                            viewModel.setStartTime(newTimeMs)
                            startHandlePosition = timeToPx(newTimeMs)
                            exoPlayer.seekTo(newTimeMs.coerceIn(0L, mergeState.videoDurationMs)) // Seek but constrain for preview sanity
                        }
                    },
                    label = { Text("Start (s)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                OutlinedTextField(
                    value = endInputText, // Bind to local string state
                    onValueChange = { newValue ->
                        endInputText = newValue // Allow any string input
                        val newTimeSec = newValue.toIntOrNull()
                        if (newTimeSec != null) {
                            // Update VM state and handle position if valid number, but no coercion here
                            val newTimeMs = newTimeSec * 1000L
                            viewModel.setEndTime(newTimeMs)
                            endHandlePosition = timeToPx(newTimeMs)
                            exoPlayer.seekTo(newTimeMs.coerceIn(0L, mergeState.videoDurationMs)) // Seek but constrain for preview sanity
                        }
                    },
                    label = { Text("End (s)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Merge Button or Loading Dialog
        if (mergeState.isMerging) {
            AlertDialog(
                onDismissRequest = { /* No dismiss */ },
                title = { Text("Deleting Gap and Merging") }, // Updated dialog title
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Processing video segments...") // Updated dialog message
                        Spacer(modifier = Modifier.height(12.dp)) // Adjusted spacing
                        // The LinearProgressIndicator already provides visual progress feedback
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {}
            )
        } else {
            Button(
                onClick = {
                    val startSec = startInputText.toIntOrNull()
                    val endSec = endInputText.toIntOrNull()

                    if (startSec == null || endSec == null) {
                        Toast.makeText(context, "Start and End times must be valid numbers.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val startTimeMs = startSec * 1000L
                    val endTimeMs = endSec * 1000L

                    // Final Validation and Coercion on click
                    val validStartTime = startTimeMs.coerceIn(0L, mergeState.videoDurationMs)
                    val validEndTime = endTimeMs.coerceIn(0L, mergeState.videoDurationMs)

                    if (validStartTime >= validEndTime) {
                        Toast.makeText(context, "End time must be greater than Start time.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Update VM with corrected, constrained values before starting merge
                    viewModel.setStartTime(validStartTime)
                    viewModel.setEndTime(validEndTime)

                    viewModel.deleteGapAndMerge(context, videoUri)
                },
                modifier = Modifier.fillMaxWidth(),
                // Enable button if current VM state meets basic validity, allowing user to fix input first
                enabled = true
            ) {
                Text("Delete Gap and Merge")
            }
        }
    }
}
