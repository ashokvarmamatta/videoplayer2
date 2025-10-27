package com.applications.player.presentation.videosOfFolder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applications.player.data.VideoRepository
import com.applications.player.model.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class VideoViewModel(private val videoRepository: VideoRepository) : ViewModel() {

    // Use the VideoListState to manage data, loading, and error
    private val _videoListState = MutableStateFlow(VideoListState())
    val videoListState: StateFlow<VideoListState> = _videoListState.asStateFlow()

    init {
        // --- FIX: Removed loadAllVideos() from init block. ---
        // The loading logic should now be entirely driven by the Activity's Intent
        // to correctly handle "All Videos" or "Videos by Folder".
    }

    /**
     * Loads ALL videos irrespective of folder.
     */
    fun loadAllVideos() {
        viewModelScope.launch {
            _videoListState.update { it.copy(isLoading = true, error = null) }
            try {
                // repository.getVideos() calls repository.getVideosByFolder(null)
                val videos = videoRepository.getVideos()
                _videoListState.update { it.copy(videos = videos, isLoading = false) }
            } catch (e: Exception) {
                _videoListState.update {
                    it.copy(isLoading = false, error = "Failed to load all videos: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }

    /**
     * **[NEW]** Loads videos filtered by a specific folder path.
     * This resolves the "Unresolved reference 'loadVideosByFolder'".
     */
    fun loadVideosByFolder(folderPath: String) {
        viewModelScope.launch {
            _videoListState.update { it.copy(isLoading = true, error = null) }
            try {
                val videos = videoRepository.getVideosByFolder(folderPath)
                _videoListState.update { it.copy(videos = videos, isLoading = false) }
            } catch (e: Exception) {
                _videoListState.update {
                    it.copy(isLoading = false, error = "Failed to load videos for folder: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }

    /**
     * **[NEW]** Deletes a video using the repository and updates the UI state.
     * @param video The Video object to be deleted.
     */
    fun deleteVideo(video: Video) {
        viewModelScope.launch {
            // OPTIONAL: You might want a separate state for deletion progress if it's long
            _videoListState.update { it.copy(error = null) }

            try {
                // Assuming videoRepository exposes a suspend deleteVideo function.
                val success = videoRepository.deleteVideo(video)

                if (success) {
                    // Update state by removing the deleted video locally
                    _videoListState.update { currentState ->
                        currentState.copy(
                            videos = currentState.videos.filter { it.uri != video.uri }
                        )
                    }
                    // A successful delete usually triggers a success Toast in the Activity/Fragment.
                } else {
                    _videoListState.update {
                        it.copy(error = "Failed to delete video: File system error.")
                    }
                }
            } catch (e: Exception) {
                _videoListState.update {
                    it.copy(error = "Failed to delete video: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }
}