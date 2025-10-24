package com.applications.player.presentation.videosbyfolders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applications.player.data.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// State for the VideosByFoldersActivity
data class FolderListState(
    val folders: List<Folder> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FoldersViewModel(
    private val repository: VideoRepository
) : ViewModel() {

    private val _folderListState = MutableStateFlow(FolderListState())
    val folderListState: StateFlow<FolderListState> = _folderListState.asStateFlow()

    init {
        loadFolders()
    }

    /**
     * Loads the list of all video folders from the repository.
     */
    fun loadFolders() {
        viewModelScope.launch {
            _folderListState.update { it.copy(isLoading = true, error = null) }
            try {
                val folders = repository.getFolders()
                _folderListState.update { it.copy(folders = folders, isLoading = false) }
            } catch (e: Exception) {
                _folderListState.update {
                    it.copy(isLoading = false, error = "Failed to load folders: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }
}