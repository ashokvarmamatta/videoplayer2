package com.applications.player.presentation.videosplitting

import android.net.Uri

data class VideoSplittingState(
    val isSplitting: Boolean = false,
    val progress: Float = 0f,
    val splitVideoUri: Uri? = null,
    val error: String? = null
)
