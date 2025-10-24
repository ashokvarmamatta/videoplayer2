package com.applications.player.presentation.videoplayer

data class VideoPlayerState(
    // Playback data
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val playbackSpeed: Float = 1.0f,

    // UI state
    val isSeeking: Boolean = false,
    val showControls: Boolean = true,
    val error: String? = null,
    val isInPipMode: Boolean = false // Tracks if the video is currently in PiP mode
)