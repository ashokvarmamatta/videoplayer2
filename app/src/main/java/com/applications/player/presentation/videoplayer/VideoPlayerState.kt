package com.applications.player.presentation.videoplayer

import com.applications.player.model.Video

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
    val isInPipMode: Boolean = false ,// Tracks if the video is currently in PiP mode

    val videoToRename: Video? = null, // <-- ADD THIS

    val showOptonDialog: Boolean=false,

    // --- ADD THESE NEW FLAGS ---
    val isVideoDeleted: Boolean = false,
    val isVideoRenamed: Boolean = false
)