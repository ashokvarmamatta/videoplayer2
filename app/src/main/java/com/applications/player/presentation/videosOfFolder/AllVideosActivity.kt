package com.applications.player.presentation.videosOfFolder


import PlaylistSelectionDialog
import PlaylistsViewModel
import android.Manifest.permission
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.applications.player.R
import com.applications.player.databinding.ActivityAllVideosBinding
import com.applications.player.model.Video

import com.applications.player.presentation.videosplitting.VideoSplittingActivity
import com.applications.player.ui.theme.VideoPlayerTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.applications.player.presentation.videoMerging.VideoMergingActivity
import com.applications.player.presentation.videoplayer.VideoPlayerActivityCompose
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

class AllVideosActivity : AppCompatActivity() {

    private val binding: ActivityAllVideosBinding by lazy {
        ActivityAllVideosBinding.inflate(layoutInflater)
    }
    // This ViewModel now holds the logic for loading All vs. Filtered videos
    private val videoViewModel: VideoViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- FIX: INITIAL VIDEO LOADING LOGIC MOVED TO START OF onCreate ---
        // Load videos based on the intent filter. This MUST happen before permissions are checked
        // AND before any state overwrites from a ViewModel init (which is now empty).
        loadVideosBasedOnIntent()

        val permChecker = registerForActivityResult<Array<String>, Map<String, Boolean>>(
            ActivityResultContracts.RequestMultiplePermissions(),
            ActivityResultCallback { result ->
                for (s in result.keys) {
                    if (result[s] == false) {
                        // User denied a crucial permission
                        Toast.makeText(this, "Storage permission required to view videos.", Toast.LENGTH_LONG).show()
                        return@ActivityResultCallback
                    }
                }
                // Permissions granted, set up the Composable content
                setupComposeContent()
            })

        // --- Permission Logic Execution ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                setupComposeContent() // Already has permission, proceed
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                // Request 'All Files Access' permission
                startActivity(intent)
            }
        } else {
            // Request standard permissions for older APIs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permChecker.launch(
                    arrayOf(
                        permission.ACCESS_MEDIA_LOCATION,
                        permission.READ_EXTERNAL_STORAGE,
                        permission.WRITE_EXTERNAL_STORAGE,
                        permission.CAMERA
                    )
                )
            } else {
                permChecker.launch(
                    arrayOf(
                        permission.READ_EXTERNAL_STORAGE,
                        permission.WRITE_EXTERNAL_STORAGE,
                        permission.CAMERA
                    )
                )
            }
        }
    }

    // Extracted video loading logic
    private fun loadVideosBasedOnIntent() {
        val folderPath = intent.getStringExtra("FILTER_FOLDER_PATH")
        if (folderPath != null) {
            // Load videos ONLY for the specified folder
            videoViewModel.loadVideosByFolder(folderPath)
            Log.d("AllVideosActivity", "Loading videos for folder: $folderPath")
        } else {
            // Load ALL videos (default behavior)
            videoViewModel.loadAllVideos()
            Log.d("AllVideosActivity", "Loading ALL videos.")
        }
    }

    // New function to set up the Compose content and observe the state
    @OptIn(UnstableApi::class)
    private fun setupComposeContent() {
        binding.composeView.setContent {
            VideoPlayerTheme {
                // *** FIX: Observing the correct StateFlow (videoListState) ***
                val state by videoViewModel.videoListState.collectAsState()
                var selectedVideo by remember { mutableStateOf<Video?>(null) }
                var videoForPlaylistDialog by remember { mutableStateOf<Video?>(null) }

                val playlistsViewModel: PlaylistsViewModel = koinViewModel()
                val videoToAddToPlaylist by playlistsViewModel.videoToAddToPlaylist.collectAsState()
                val playlists by playlistsViewModel.playlists.collectAsState()

                // --- NEW: State to manage the rename dialog ---
                var videoToRename by remember { mutableStateOf<Video?>(null) }


                // Set the screen title dynamically
                val folderPath = intent.getStringExtra("FILTER_FOLDER_PATH")
                val screenTitle = if (folderPath != null) {
                    // Extract the folder name from the path for the title
                    File(folderPath).name // Use File to reliably get the name from the path
                } else {
                    "All Videos"
                }

                VideoListScreen(
                    videoList = state.videos, // Use the videos from the state
                    isLoading = state.isLoading, // Pass loading state to UI
                    title = screenTitle, // Pass dynamic title
                    onVideoClick = { video1 ->
                        selectedVideo = video1
                    }
                )

                // Display error from state
                if (state.error != null) {
                    Toast.makeText(this, state.error, Toast.LENGTH_LONG).show()
                }

                if (selectedVideo != null) {
                    SelectionDialog(
                        video = selectedVideo!!,
                        onDismiss = { selectedVideo = null },
                        onPlayClick = { video ->
                            selectedVideo = null
                            val intent = Intent(this@AllVideosActivity, VideoPlayerActivityCompose::class.java)
                            intent.putExtra("video", video)
                            startActivity(intent)
                        },
                        onAddToPlayListChecked = { video ->
                           videoForPlaylistDialog = video

                        },
                        onSplitClick = { video ->
                            selectedVideo = null
                            val intent = Intent(this@AllVideosActivity, VideoSplittingActivity::class.java)
                            intent.putExtra("videoUri", video.uri)
                            startActivity(intent)
                        },
                        // --- NEW: Connect onRenameClicked ---
                        onRenameCLicked = { video ->
                            selectedVideo = null // Dismiss the selection dialog
                            videoToRename = video // Show the rename dialog
                        },
                       /* onCropClick = { video ->
                            selectedVideo = null
                            val intent = Intent(this@AllVideosActivity, VideoCropActivity::class.java)
                            intent.putExtra("videoUri", video.uri)
                            startActivity(intent)
                        },*/
                        onMergeClick = { video ->
                            selectedVideo = null
                            val intent = Intent(this@AllVideosActivity, VideoMergingActivity::class.java)
                            intent.putExtra("videoUri", video.uri)
                            startActivity(intent)
                        },
                        // --- NEW: Handle Delete Click ---
                        onDeleteClick = { video ->
                            selectedVideo = null
                            videoViewModel.deleteVideo(video)
                            // In a full MVVM/Repository pattern, this would call videoViewModel.deleteVideo(video)
                            // We use a Toast and refresh here as a placeholder for the delete operation.
                            Log.d("AllVideosActivity", "Attempting to delete video: ${video.name}")
                            Toast.makeText(this@AllVideosActivity, "Delete request sent for ${video.name}. List will refresh.", Toast.LENGTH_SHORT).show()
                            loadVideosBasedOnIntent() // Refresh list immediately
                        }
                    )
                }
              if(videoForPlaylistDialog!=null) {
                  // Handle adding to playlist logic here
                  PlaylistSelectionDialog(
                      video = selectedVideo!!,
                      playlists = playlists,
                      onDismiss = { playlistsViewModel.onDismissPlaylistDialog() },
                      onPlaylistSelected = { playlist, videoToAdd ->
                          playlistsViewModel.addVideoToPlaylist(playlist, videoToAdd)
                          videoForPlaylistDialog = null
                          selectedVideo=null
                      },
                      onAddNewPlaylist = { playlistName ->
                          playlistsViewModel.createNewPlaylist(playlistName)

                      }
                  )
              }

                // --- NEW: Show Rename Dialog when videoToRename is not null ---
                if (videoToRename != null) {
                    RenameVideoDialog(
                        video = videoToRename!!,
                        onDismiss = { videoToRename = null },
                        onRename = { video, newName ->
                            // Call the ViewModel to perform the rename operation
                            val currentFolder = intent.getStringExtra("FILTER_FOLDER_PATH")
                            videoViewModel.renameVideo(video, newName, currentFolder)
                            videoToRename = null // Dismiss the dialog
                            Toast.makeText(this, "Renaming video...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()

        // --- FIX: Refresh logic in onResume ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Check if permission is granted again after the user returns from settings
            if (Environment.isExternalStorageManager()) {
                setupComposeContent() // Set content if permissions are confirmed
                loadVideosBasedOnIntent() // Reload videos to pick up any new files
            }
        } else {
            // For older versions, the content is set after the permission checker result,
            // but we should still reload videos to refresh the list.
            loadVideosBasedOnIntent()
        }
    }
}



@Composable
fun SelectionDialog(
    video: Video,
    onDismiss: () -> Unit,
    onPlayClick: (Video) -> Unit,
    onAddToPlayListChecked:(Video) -> Unit,
    onRenameCLicked:(Video) -> Unit,
    onSplitClick: (Video) -> Unit,
    onMergeClick: (Video) -> Unit,
    onDeleteClick: (Video) -> Unit
) {
    // Use AlertDialog for the dialog-style UI
    AlertDialog(
        onDismissRequest = onDismiss,
        // The title composable now contains the thumbnail and video name
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = video.thumbnailUri),
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        // The text composable contains the list of actions
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp)) // Add space between title and options
                OptionRow(iconResId = R.drawable.vid_play, text = "Play") { onPlayClick(video) }
                OptionRow(iconResId = R.drawable.playlist_a, text = "Add to Playlist") { onAddToPlayListChecked(video) }
                OptionRow(iconResId = R.drawable.rename, text = "Rename") { onRenameCLicked(video) }
                OptionRow(iconResId = R.drawable.del, text = "Delete") { onDeleteClick(video) }

                // Hidden options
                // OptionRow(iconResId = R.drawable.split, text = "Split") { onSplitClick(video) }
                // OptionRow(iconResId = R.drawable.merge, text = "Merge") { onMergeClick(video) }
            }
        },
        // We handle clicks in each row, so the confirm button is not needed.
        confirmButton = {
            // You can add a "Close" button here if you want
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// Helper composable updated to use drawable resource IDs
@Composable
private fun OptionRow(
    iconResId: Int, // Changed parameter name for clarity
    text: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = text,
            modifier = Modifier.size(24.dp),
            // Optional: Tint the drawable icon to match your app's theme
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}


/**
 * **[NEW]** A dialog Composable for renaming a video.
 */
@Composable
fun RenameVideoDialog(
    video: Video,
    onDismiss: () -> Unit,
    onRename: (Video, String) -> Unit
) {
    var newName by remember { mutableStateOf(video.name) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Video") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New name") },
                    singleLine = true,
                    isError = error != null
                )
                if (error != null) {
                    Text(text = error!!, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalName = if (!newName.contains('.')) "$newName.mp4" else newName
                    if (finalName.isNotBlank() && !finalName.contains(File.separatorChar)) {
                        onRename(video, finalName)
                    } else {
                        error = "Name cannot be empty or contain '/'."
                    }
                }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}