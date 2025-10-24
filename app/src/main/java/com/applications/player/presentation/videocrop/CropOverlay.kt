package com.applications.player.presentation.videocrop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun CropOverlay(
    videoSize: IntSize,
    selectedRatio: AspectRatio
) {
    val cropModifier = Modifier.fillMaxSize()
    val cropBoxModifier = Modifier
        .background(Color.Transparent)
        .border(2.dp, Color.White)

    val cropBoxRatio = when (selectedRatio) {
        AspectRatio.RATIO_1_1 -> 1f / 1f
        AspectRatio.RATIO_9_16 -> 9f / 16f
        AspectRatio.RATIO_16_9 -> 16f / 9f
        else -> {
            if (videoSize.width > 0 && videoSize.height > 0) {
                videoSize.width.toFloat() / videoSize.height.toFloat()
            } else {
                16f / 9f
            }
        }
    }

    Box(
        modifier = cropModifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(cropBoxRatio)
                .fillMaxSize()
                .then(cropBoxModifier)
        )
    }
}

@Composable
fun AspectRatioButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(text)
    }
}