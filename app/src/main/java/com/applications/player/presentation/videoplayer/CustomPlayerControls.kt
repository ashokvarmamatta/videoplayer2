package com.applications.player.presentation.videoplayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import com.applications.player.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.data.position
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.circle
import com.applications.player.presentation.videosOfFolder.formatDuration
import com.applications.player.ui.theme.ReadexPro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPlayerControls(
    state: VideoPlayerState,
    viewModel: VideoPlayerViewModel,
    onToggleFullscreen: (forceLandscape: Boolean) -> Unit,
    isFullScreen: Boolean,
    onToggleLock: () -> Unit,
    isScreenLocked: Boolean,
    onEnterPipMode: () -> Unit,
    onSelectSubtitle: () -> Unit,
    onUserInteract: (Boolean) -> Unit,
    onAddToPlaylist: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onTogglePlayPause: () -> Unit
) {
    var tempSliderValue by remember { mutableStateOf(state.currentPosition.toFloat()) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    // MODIFICATION: New state to control the options dialog visibility
    var showOptionsDialog by remember { mutableStateOf(false) }

    val speedOptions = remember { listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f) }

    LaunchedEffect(state.currentPosition, state.isSeeking) {
        if (!state.isSeeking) {
            tempSliderValue = state.currentPosition.toFloat()
        }
    }

    // MODIFICATION: Logic for the new "Options" dialog
    if (showOptionsDialog) {
        OptionsDialog(
            onDismissRequest = { showOptionsDialog = false },
            onAddToPlaylist = onAddToPlaylist,
            onRename = onRename,
            onDelete = onDelete,
            onEnterPipMode = onEnterPipMode,
            onShowSpeedDialog = { showSpeedDialog = true },
            playbackSpeed = state.playbackSpeed
        )
    }

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed") },
            text = {
                Column {
                    speedOptions.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.playbackSpeed == speed,
                                onClick = {
                                    viewModel.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                            )
                            Text(
                                text = "${String.format("%.2f", speed)}x",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("CLOSE") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDuration(state.currentPosition), color = Color.White, fontSize = 12.sp)

            CanvasSlider(
                value = if (state.isSeeking) tempSliderValue else state.currentPosition.toFloat(),
                onValueChange = {
                    onUserInteract(true)
                    viewModel.setSeeking(true)
                    tempSliderValue = it
                },
                onValueChangeFinished = {
                    onUserInteract(false)
                    viewModel.seekTo(tempSliderValue.toLong())
                    viewModel.setSeeking(false)
                },
                valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            /* Slider(
                 value = if (state.isSeeking) tempSliderValue else state.currentPosition.toFloat(),
                 onValueChange = {
                     onUserInteract(true)
                     viewModel.setSeeking(true)
                     tempSliderValue = it
                 },
                 onValueChangeFinished = {
                     onUserInteract(false)
                     viewModel.seekTo(tempSliderValue.toLong())
                     viewModel.setSeeking(false)
                 },
                 valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f),
                 enabled = state.duration > 0,
                 modifier = Modifier
                     .weight(1f)
                     .padding(horizontal = 8.dp).clip(RectangleShape),
                 colors = SliderDefaults.colors(
                     thumbColor = Color(0xFF526CF8),
                     activeTrackColor = Color(0xFF526CF8),
                     inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                 ),
                 thumb = {
                     Icon(
                         painter = painterResource(id = R.drawable.thub_icon),
                         contentDescription = "Slider thumb",
                         modifier = Modifier.size(20.dp),
                         tint = Color(0xFF526CF8)
                     )
                 }
             )*/
            Text(formatDuration(state.duration), color = Color.White, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // MODIFICATION: Removed "Speed" and "PiP" from this row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                PlayerControlButton(
                    icon = painterResource(R.drawable.n_subtitle),
                    text = "Subtitles",
                    onClick = { onSelectSubtitle.invoke() }
                )
            }
            item {
                PlayerControlButton(
                    icon = if (isScreenLocked) painterResource(R.drawable.n_lock) else painterResource(
                        R.drawable.n_lock
                    ),
                    text = if (isScreenLocked) "Unlock" else "Lock",
                    onClick = onToggleLock
                )
            }

            item {
                Box(
                    modifier = Modifier.padding(bottom = 20.dp), // You can adjust this value
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(10.dp))
                        PlayerControlButtonForPlayPause(
                            icon = if (state.isPlaying) painterResource(R.drawable.pause) else painterResource(
                                R.drawable.play_arrow
                            ),
                            text = if (state.isPlaying) "Pause" else "Play",
                            onClick = onTogglePlayPause
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
            }

            item {
                PlayerControlButton(
                    icon = if (isFullScreen) painterResource(R.drawable.n_full_screen) else painterResource(
                        R.drawable.n_full_screen
                    ),
                    text = if (isFullScreen) "Exit" else "Fullscreen",
                    onClick = {
                        // CHANGE 1: START - Updated Fullscreen logic
                        val videoWidth = state.videoWidth
                        val videoHeight = state.videoHeight

                        // Force landscape only if video is wider than it is tall (e.g., 16:9)
                        // and we are entering fullscreen, not exiting.
                        val shouldForceLandscape = if (!isFullScreen) {
                            videoWidth > videoHeight
                        } else {
                            // When exiting fullscreen, let the system handle returning to default.
                            false
                        }

                        onToggleFullscreen(shouldForceLandscape)
                        // CHANGE 1: END
                    }
                )
            }
            item {
                PlayerControlButton(
                    icon = painterResource(R.drawable.n_settings),
                    text = "Options",
                    onClick = {
                        // CHANGE 2: START - Pause video and show dialog
                        if (state.isPlaying) {
                            onTogglePlayPause()
                        }
                        showOptionsDialog = true
                        // CHANGE 2: END
                    }
                )
            }
        }
    }
}

// MODIFICATION: New composable for the options dialog
@Composable
private fun OptionsDialog(
    onDismissRequest: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onEnterPipMode: () -> Unit,
    onShowSpeedDialog: () -> Unit,
    playbackSpeed: Float
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Video Options") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DialogOption(
                    icon = painterResource(R.drawable.n_playlist),
                    text = "Add to playlist"
                ) {
                    onAddToPlaylist()
                    onDismissRequest()
                }
                DialogOption(icon = painterResource(R.drawable.n_folder_video), text = "Rename") {
                    onRename()
                    onDismissRequest()
                }
                DialogOption(icon = painterResource(R.drawable.del), text = "Delete") {
                    onDelete()
                    onDismissRequest()
                }
                DialogOption(
                    icon = painterResource(R.drawable.speed_),
                    text = "Playback Speed (${String.format("%.2f", playbackSpeed)}x)"
                ) {
                    onShowSpeedDialog()
                    onDismissRequest()
                }
                DialogOption(
                    icon = painterResource(R.drawable.n_video),
                    text = "Picture-in-Picture"
                ) {
                    onEnterPipMode()
                    onDismissRequest()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text("CLOSE") }
        }
    )
}

// MODIFICATION: New helper composable for a row in the options dialog
@Composable
private fun DialogOption(icon: Painter, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = text,
            tint = Color(0xFF000000),
            modifier = Modifier.size(25.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, fontFamily = ReadexPro, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun PlayerControlButton(icon: Painter, text: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = text,
            tint = Color(0xFFFFFFFF),
            modifier = Modifier.size(25.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            fontFamily = ReadexPro
        )
    }
}

@Composable
private fun PlayerControlButtonForPlayPause(icon: Painter, text: String, onClick: () -> Unit) {// MODIFICATION: Wrap the Icon in a Box with a circular background
    Box(
        modifier = Modifier
            .size(48.dp) // Set a fixed size for the circular area
            .background(color = Color(0xFFFF702F), shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center // Center the icon within the Box
    ) {
        Icon(
            painter = icon,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(28.dp) // Keep the original icon size
        )


    }

}



@Composable
fun CanvasSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val activeTrackColor = Color(0xFF5F381C)
    val inactiveTrackColor = Color.LightGray
    val thumbColor = Color(0xFFFF702F) // A slightly lighter blue for the thumb

    // --- NEW: Get density from the local composition ---
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp) // Increased height for easier touch interaction
            .pointerInput(valueRange) {
                detectDragGestures(
                    onDragEnd = onValueChangeFinished
                ) { change, _ ->
                    val width = size.width.toFloat()
                    val newValue =
                        (change.position.x / width * (valueRange.endInclusive - valueRange.start))
                            .coerceIn(valueRange.start, valueRange.endInclusive)
                    onValueChange(newValue)
                    change.consume()
                }
            }
    ) {
        // --- FIX: Convert Dp to Px using the density ---
        val thumbRadius = with(density) { 8.dp.toPx() }
        val trackHeight = with(density) { 4.dp.toPx() }

        val progress = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val progressPx = progress * canvasWidth

            // Draw inactive track
            drawRoundRect(
                color = inactiveTrackColor,
                topLeft = Offset(x = 0f, y = (canvasHeight - trackHeight) / 2),
                size = Size(width = canvasWidth, height = trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2)
            )

            // Draw active track
            drawRoundRect(
                color = activeTrackColor,
                topLeft = Offset(x = 0f, y = (canvasHeight - trackHeight) / 2),
                size = Size(width = progressPx, height = trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2)
            )

            // Draw thumb
            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(
                    x = progressPx.coerceIn(0f, canvasWidth),
                    y = canvasHeight / 2
                ) // Coerce thumb position
            )
        }
    }
}


