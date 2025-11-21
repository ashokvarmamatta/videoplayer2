package com.applications.player.presentation.homeScreen

import PlaylistEntity
import PlaylistRepository
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applications.player.data.VideoRepository
import com.applications.player.model.Video
import com.applications.player.presentation.videosbyfolders.Folder
import com.applications.player.util.ViewStyle
import com.applications.player.util.ViewStyleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File


class HomeActivityViewModel(
    private val videoRepository: VideoRepository,
    private val playlistRepository: PlaylistRepository,
    private val viewStyleManager: ViewStyleManager // Inject the manager
) : ViewModel() {

    private val _homeActivityModel = MutableStateFlow(HomeActivityState())
    val homeActivityState: StateFlow<HomeActivityState> = _homeActivityModel.asStateFlow()

    init {
        // Load the initial view style from SharedPreferences
        val initialStyle = viewStyleManager.getViewStyle()
        _homeActivityModel.update { it.copy(viewStyle = initialStyle) }

        loadAllVideos()
        loadFolders()
    }

    // Public function to handle UI events (clicks)
    fun onNavigationItemSelected(item: NavItem) {
        _homeActivityModel.update { currentState ->
            currentState.copy(itemSelected = item)
        }
    }

    fun onVideoSelected(video: Video?) {
        _homeActivityModel.update { currentState ->
            currentState.copy( selectedVideo = video)
        }
    }

    fun onVideoRenameSelected(video: Video?) {
        // This function will be used to show the rename dialog
        _homeActivityModel.update { currentState ->
            currentState.copy(videoToRename = video)
        }
    }


    /**
     * **[NEW]** Executes the video rename operation.
     * @param video The video to rename.
     * @param newName The desired new name for the video.
     */
    fun renameVideo(video: Video, newName: String) {
        viewModelScope.launch {
            _homeActivityModel.update { it.copy(isAllLoading = true, error = null) }
            try {
                val updatedVideo = videoRepository.renameVideo(video, newName)
                if (updatedVideo != null) {
                    _homeActivityModel.update {
                        it.copy(success = "Video renamed successfully", videoToRename = null)
                    }
                    // Refresh data to reflect the change everywhere
                    loadAllVideos()
                    loadFolders()
                } else {
                    _homeActivityModel.update {
                        it.copy(error = "Failed to rename video.", isAllLoading = false)
                    }
                }
            } catch (e: Exception) {
                _homeActivityModel.update {
                    it.copy(error = "Failed to rename video: ${e.message}", isAllLoading = false)
                }
                e.printStackTrace()
            }
        }
    }

    fun loadVideosOfFolder(folderPath: String) {
        viewModelScope.launch {
            // 1. Set loading to true
            _homeActivityModel.update { it.copy(isLoadingVideosInFolder = true) }
            try {
                val videos = videoRepository.getVideosByFolder(folderPath)
                // 2. Set the data and turn loading off
                _homeActivityModel.update {
                    it.copy(videosInFolder = videos, isLoadingVideosInFolder = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 3. Turn loading off on error
                _homeActivityModel.update { it.copy(isLoadingVideosInFolder = false) }
            }
        }
    }


    // ADD THIS FUNCTION: To clear the folder view when going back
    fun clearVideosInFolder() {
        _homeActivityModel.update { it.copy(videosInFolder = emptyList()) }
    }



    fun deleteVideo(video: Video) {
        viewModelScope.launch {
            // OPTIONAL: You might want a separate state for deletion progress if it's long
            _homeActivityModel.update { it.copy(error = null) }

            try {
                // Assuming videoRepository exposes a suspend deleteVideo function.
                val success = videoRepository.deleteVideo(video)

                if (success) {
                    // Update state by removing the deleted video locally
                    _homeActivityModel.update {
                        it.copy(success = "Video deleted successfully")
                    }
                    loadAllVideos()
                    loadFolders()
                    // A successful delete usually triggers a success Toast in the Activity/Fragment.
                } else {
                    _homeActivityModel.update {
                        it.copy(error = "Failed to delete video: File system error.")
                    }
                }
            } catch (e: Exception) {
                _homeActivityModel.update {
                    it.copy(error = "Failed to delete video: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }


    // In ViewModel.kt (No change needed)
    fun loadAllVideos() {
        viewModelScope.launch { // This runs on Dispatchers.Main.immediate
            _homeActivityModel.update { it.copy(isAllLoading = true, error = null, videos = emptyList()) }
            try {
                // .collect() runs on the Main thread, and flowOn(IO) handles the background work.
                videoRepository.getAllVideosChunked()
                    .collect { chunkedVideos ->
                        _homeActivityModel.update {
                            it.copy(videos = it.videos + chunkedVideos)
                        }
                    }
                _homeActivityModel.update { it.copy(isAllLoading = false) }
            } catch (e: Exception) {
                _homeActivityModel.update {
                    it.copy(error = "Failed to load videos: ${e.message}", isAllLoading = false)
                }
                e.printStackTrace()
            }
        }
    }


    fun loadFolders() {
        viewModelScope.launch {
            _homeActivityModel.update { it.copy(isLoadingFolders = true, error = null) }
            try {
                val folders = videoRepository.getFolders()
                _homeActivityModel.update { it.copy(folders = folders, isLoadingFolders = false) }
            } catch (e: Exception) {
                _homeActivityModel.update {
                    it.copy(isLoadingFolders = false, error = "Failed to load folders: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }

    /**
     * Toggles the view style between GRID and LIST and saves the preference.
     */
    fun toggleViewStyle() {
        val currentStyle = _homeActivityModel.value.viewStyle
        val newStyle = if (currentStyle == ViewStyle.GRID) ViewStyle.LIST else ViewStyle.GRID
        viewStyleManager.saveViewStyle(newStyle)
        _homeActivityModel.update { it.copy(viewStyle = newStyle) }
    }

}
