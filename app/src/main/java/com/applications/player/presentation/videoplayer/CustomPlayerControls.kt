package com.applications.player.presentation.videoplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onEnterPipMode: () -> Unit, // PiP callback
    onUserInteract: (Boolean) -> Unit // Callback for user interaction
) {
    var tempSliderValue by remember { mutableStateOf(state.currentPosition.toFloat()) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val speedOptions = remember { listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f) }

    LaunchedEffect(state.currentPosition, state.isSeeking) {
        if (!state.isSeeking) {
            tempSliderValue = state.currentPosition.toFloat()
        }
    }
    LaunchedEffect(state.duration) {
        if (state.duration > 0 && tempSliderValue == 0f) {
            tempSliderValue = state.currentPosition.toFloat()
        }
    }

    // Speed Selection Dialog (Implementation omitted for brevity)
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed") },
            text = { /* Dialog content */ },
            confirmButton = { TextButton(onClick = { showSpeedDialog = false }) { Text("CLOSE") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Top Row: Current Time, Slider, Total Time ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(state.currentPosition),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.width(60.dp)
            )

            Slider(
                value = if (state.isSeeking) tempSliderValue else state.currentPosition.toFloat(),
                onValueChange = { newValue ->
                    onUserInteract(true)
                    viewModel.setSeeking(true)
                    tempSliderValue = newValue
                },
                onValueChangeFinished = {
                    onUserInteract(false)
                    viewModel.seekTo(tempSliderValue.toLong())
                    viewModel.setSeeking(false)
                },
                valueRange = 0f..state.duration.toFloat().coerceAtLeast(0f),
                enabled = state.duration > 0,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
                modifier = Modifier.weight(1f)
            )

            Text(
                text = formatDuration(state.duration),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.width(60.dp),
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 2. Action Buttons (LazyRow) ---
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lock/Unlock Button
            item {
                PlayerControlButton(
                    icon = if (isScreenLocked) Icons.Default.Lock else Icons.Default.Lock,
                    text = if (isScreenLocked) "Unlock" else "Lock",
                    onClick = onToggleLock
                )
            }

            // Fullscreen Button
            item {
                PlayerControlButton(
                    icon = if (isFullScreen) Icons.Default.ExitToApp else Icons.Default.ExitToApp,
                    text = if (isFullScreen) "Exit" else "Fullscreen",
                    onClick = onToggleFullscreen
                )
            }

            // Speed Control Button
            item {
                PlayerControlButton(
                    icon = Icons.Default.AddCircle,
                    text = "${String.format("%.2f", state.playbackSpeed)}x",
                    onClick = { showSpeedDialog = true }
                )
            }

            // Picture-in-Picture (PiP) Button
            item {
                PlayerControlButton(
                    icon = Icons.Default.AccountBox,
                    text = "PIP",
                    onClick = onEnterPipMode
                )
            }

            // Subtitles Button
            item {
                PlayerControlButton(
                    icon = Icons.Default.List,
                    text = "Subtitles",
                    onClick = { /* TODO: Implement subtitle selection */ }
                )
            }

            // Options Button (Placeholder)
            item {
                PlayerControlButton(
                    icon = Icons.Default.Settings,
                    text = "Options",
                    onClick = { /* TODO: Implement options menu */ }
                )
            }
        }
    }
}

@Composable
private fun PlayerControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .widthIn(min = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(text = text, color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}