import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applications.player.model.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Import MutableStateFlow and combine it with other imports
class PlaylistsViewModel(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    // --- EXISTING CODE ---
    val playlists: StateFlow<List<PlaylistEntity>> = playlistRepository.getAllPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- NEW STATE for selected playlist ---
    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist: StateFlow<PlaylistEntity?> = _selectedPlaylist.asStateFlow()

    // This will hold the video that the user wants to add to a playlist.
    // When it's not null, we'll show the dialog.
    private val _videoToAddToPlaylist = MutableStateFlow<Video?>(null)
    val videoToAddToPlaylist: StateFlow<Video?> = _videoToAddToPlaylist.asStateFlow()

    // --- NEW FUNCTIONS ---
    /**
     * Called when the user clicks "Add to Playlist".
     * It sets the video and triggers the dialog to be shown.
     */
    fun onAddToPlaylistRequest(video: Video) {
        _videoToAddToPlaylist.value = video
    }

    /**
     * Called to dismiss the dialog.
     */
    fun onDismissPlaylistDialog() {
        _videoToAddToPlaylist.value = null
    }

    /**
     * Called when a user selects a playlist from the list.
     */
    fun onPlaylistSelected(playlist: PlaylistEntity?) {
        _selectedPlaylist.value = playlist
    }

    /**
     * Called to go back from the video list of a playlist.
     */
    fun onBackFromPlaylist() {
        _selectedPlaylist.value = null
    }


    // --- MODIFIED FUNCTION ---
    /**
     * Now takes a PlaylistEntity directly.
     * Called when a user selects a playlist from the dialog.
     */
    fun addVideoToPlaylist(playlist: PlaylistEntity, video: Video) {
        viewModelScope.launch {
            val updatedVideos = playlist.videos.toMutableList().apply { add(video) }
            // Ensure we don't add duplicates if the video is already there
            // val updatedVideos = playlist.videos.toMutableList().apply {
            //    if (!this.contains(video)) add(video)
            // }
            val updatedPlaylist = playlist.copy(videos = updatedVideos)
            playlistRepository.updatePlaylist(updatedPlaylist)

            // Dismiss the dialog after adding
            onDismissPlaylistDialog()
        }
    }

    // --- EXISTING FUNCTIONS (no changes needed) ---
    fun createNewPlaylist(name: String) {
        viewModelScope.launch {
            val newPlaylist = PlaylistEntity(name = name)
            playlistRepository.createPlaylist(newPlaylist)
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlist)
        }
    }
}