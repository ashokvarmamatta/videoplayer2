package com.applications.player.presentation.homeScreen

import PlaylistEntity
import PlaylistRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applications.player.data.VideoRepository
import com.applications.player.model.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class HomeActivityViewModel(private  val videoRepository: VideoRepository, private val playlistRepository: PlaylistRepository) : ViewModel() {

    private val _homeActivityModel = MutableStateFlow(HomeActivityState(itemSelected = NavItem.VIDEOS))
    val homeActivityState: StateFlow<HomeActivityState> = _homeActivityModel.asStateFlow()

    init {
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



    fun loadVideosOfFolder(folderPath: String) {
        viewModelScope.launch {
            _homeActivityModel.update { it.copy(isLoadingFolders = true, error = null) }
            try {
                val videosFolders = videoRepository.getVideosByFolder(folderPath)
                _homeActivityModel.update {
                    it.copy(
                        videos = videosFolders,
                        isLoadingFolders = false)
                }
            } catch (e: Exception) {
            }

        }
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
                            it.copy(videos = chunkedVideos)
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

}

