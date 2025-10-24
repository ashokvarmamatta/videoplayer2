package com.applications.player.presentation.videoMerging

data class VideoMergingState(
    val isMerging: Boolean = false,
    val mergedVideoUri: String? = null,
    val error: String? = null,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val videoDurationMs: Long = 0L
)