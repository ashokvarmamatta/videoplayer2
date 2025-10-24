package com.applications.player.presentation.videosOfFolder


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
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.applications.player.R
import com.applications.player.databinding.ActivityAllVideosBinding
import com.applications.player.model.Video
import com.applications.player.presentation.VideoPlayerActivity

import com.applications.player.presentation.videocrop.VideoCropActivity
import com.applications.player.presentation.videosplitting.VideoSplittingActivity
import com.applications.player.ui.theme.VideoPlayerTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.applications.player.presentation.videoMerging.VideoMergingActivity
import com.applications.player.presentation.videoplayer.VideoPlayerActivityCompose
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
    private fun setupComposeContent() {
        binding.composeView.setContent {
            VideoPlayerTheme {
                // *** FIX: Observing the correct StateFlow (videoListState) ***
                val state by videoViewModel.videoListState.collectAsState()
                var selectedVideo by remember { mutableStateOf<Video?>(null) }

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
                        onSplitClick = { video ->
                            selectedVideo = null
                            val intent = Intent(this@AllVideosActivity, VideoSplittingActivity::class.java)
                            intent.putExtra("videoUri", video.uri)
                            startActivity(intent)
                        },
                        onCropClick = { video ->
                            selectedVideo = null
                            val intent = Intent(this@AllVideosActivity, VideoCropActivity::class.java)
                            intent.putExtra("videoUri", video.uri)
                            startActivity(intent)
                        },
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
    onSplitClick: (Video) -> Unit,
    onCropClick: (Video) -> Unit,
    onMergeClick: (Video) -> Unit,
    onDeleteClick: (Video) -> Unit // --- NEW: Delete Callback ---
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Choose an action for ${video.name}")
        },
        text = {
            Text("What would you like to do with this video?")
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = { onPlayClick(video) }) { Text("Play") }
                TextButton(onClick = { onSplitClick(video) }) { Text("Split") }
//                TextButton(onClick = { onCropClick(video) }) { Text("Crop") }
                TextButton(onClick = { onMergeClick(video) }) { Text("Merge/Delete Gap") }
                // --- NEW: Delete Button Added ---
                TextButton(onClick = { onDeleteClick(video) }) { Text("Delete Video") }
            }
        }
    )
}