package com.applications.player.presentation.videosOfFolder

import com.applications.player.model.Video

// Define the state data class for the video list
data class VideoListState(
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
