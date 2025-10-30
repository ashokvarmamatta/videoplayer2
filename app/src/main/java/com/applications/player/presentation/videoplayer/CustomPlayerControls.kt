package com.applications.player.presentation.videoplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.*
import com.applications.player.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.applications.player.presentation.videosOfFolder.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPlayerControls(
    state: VideoPlayerState,
    viewModel: VideoPlayerViewModel,
    onToggleFullscreen: () -> Unit,
    isFullScreen: Boolean,
    onToggleLock: () -> Unit,
    isScreenLocked: Boolean,
    onEnterPipMode: () -> Unit,
    onUserInteract: (Boolean) -> Unit
) {
    var tempSliderValue by remember { mutableStateOf(state.currentPosition.toFloat()) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val speedOptions = remember { listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f) }

    LaunchedEffect(state.currentPosition, state.isSeeking) {
        if (!state.isSeeking) {
            tempSliderValue = state.currentPosition.toFloat()
        }
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
            Slider(
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
                    .padding(horizontal = 8.dp)
            )
            Text(formatDuration(state.duration), color = Color.White, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            /* item { PlayerControlButton(if (isScreenLocked) Icons.Default.LockOpen else Icons.Default.Lock, "Lock", onToggleLock) }
             item { PlayerControlButton(if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, if (isFullScreen) "Exit" else "Fullscreen", onToggleFullscreen) }
             item { PlayerControlButton(Icons.Default.Speed, "${String.format("%.2f", state.playbackSpeed)}x") { showSpeedDialog = true } }
             item { PlayerControlButton(Icons.Default.PictureInPicture, "PiP", onEnterPipMode) }
             item { PlayerControlButton(Icons.Default.Subtitles, "Subtitles") { *//* TODO *//* } }
            item { PlayerControlButton(Icons.Default.Settings, "Options") { *//* TODO *//* } }
*/
                    item { PlayerControlButton(
                        icon = if (isScreenLocked) painterResource(R.drawable.lock_open) else painterResource(R.drawable.lock),
                        text = if (isScreenLocked) "Unlock" else "Lock",
                        onClick = onToggleLock
                    ) }
            item { PlayerControlButton(
                icon = if (isFullScreen) painterResource(R.drawable.fullscreen_exit_) else painterResource(R.drawable.fullscreen),
                text = if (isFullScreen) "Exit" else "Fullscreen",
                onClick = onToggleFullscreen
            ) }
            item { PlayerControlButton(
                icon = painterResource(R.drawable.speed_), // Placeholder for a custom speed icon
                text = "${String.format("%.2f", state.playbackSpeed)}x",
                onClick = { showSpeedDialog = true }
            ) }
            item { PlayerControlButton(
                icon = painterResource(R.drawable.picture_in_picture), // Placeholder for a custom PiP icon
                text = "PiP",
                onClick = onEnterPipMode
            ) }
            item { PlayerControlButton(
                icon = painterResource(R.drawable.subtitles_), // Placeholder for a custom Subtitles icon
                text = "Subtitles",
                onClick = { /* TODO */ }
            ) }
            item { PlayerControlButton(
                icon = painterResource(R.drawable.settings1), // Placeholder for a custom Settings icon
                text = "Options",
                onClick = { /* TODO */ }
            ) }
        }
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
        Icon(icon, contentDescription = text, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}
