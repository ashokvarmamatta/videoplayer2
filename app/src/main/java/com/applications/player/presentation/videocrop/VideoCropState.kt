package com.applications.player.presentation.videocrop

import android.net.Uri

data class VideoCropState(
    val selectedRatio: AspectRatio = AspectRatio.ORIGINAL,
    val isCropping: Boolean = false,
    val progress: Float = 0f,
    val croppedVideoUri: String? = null,
    val error: String? = null
)

enum class AspectRatio {
    RATIO_1_1,
    RATIO_9_16,
    RATIO_16_9,
    ORIGINAL
}