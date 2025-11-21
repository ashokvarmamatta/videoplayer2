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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.applications.player.presentation.videoMerging.VideoMergingActivity
import com.applications.player.presentation.videoplayer.VideoPlayerActivityCompose
import com.applications.player.presentation.videosplitting.VideoSplittingActivity
import com.applications.player.ui.theme.VideoPlayerTheme
import com.applications.player.util.ViewStyle
import com.applications.player.util.ViewStyleManager
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

class AllVideosActivity : AppCompatActivity() {

    private val binding: ActivityAllVideosBinding by lazy {
        ActivityAllVideosBinding.inflate(layoutInflater)
    }
    private val videoViewModel: VideoViewModel by viewModel()
    // --- NEW: Inject ViewStyleManager ---
    private val viewStyleManager: ViewStyleManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadVideosBasedOnIntent()

        val permChecker = registerForActivityResult<Array<String>, Map<String, Boolean>>(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            if (result.values.all { it }) {
                // Permissions granted, set up the Composable content
                setupComposeContent()
            } else {
                // User denied a crucial permission
                Toast.makeText(this, "Storage permission required to view videos.", Toast.LENGTH_LONG).show()
                finish() // Close activity if permissions are denied
            }
        }

        // --- Permission Logic Execution ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                setupComposeContent() // Already has permission, proceed
            } else {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback for devices that might not handle this intent
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        } else {
            // Request standard permissions for older APIs
            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(
                    permission.ACCESS_MEDIA_LOCATION,
                    permission.READ_EXTERNAL_STORAGE,
                    permission.WRITE_EXTERNAL_STORAGE,
                    permission.CAMERA
                )
            } else {
                arrayOf(
                    permission.READ_EXTERNAL_STORAGE,
                    permission.WRITE_EXTERNAL_STORAGE,
                    permission.CAMERA
                )
            }
            permChecker.launch(permissionsToRequest)
        }
    }

    private fun loadVideosBasedOnIntent() {
        val folderPath = intent.getStringExtra("FILTER_FOLDER_PATH")
        if (folderPath != null) {
            videoViewModel.loadVideosByFolder(folderPath)
            Log.d("AllVideosActivity", "Loading videos for folder: $folderPath")
        } else {
            videoViewModel.loadAllVideos()
            Log.d("AllVideosActivity", "Loading ALL videos.")
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupComposeContent() {
        binding.composeView.setContent {
            VideoPlayerTheme {
                val state by videoViewModel.videoListState.collectAsState()
                var selectedVideo by remember { mutableStateOf<Video?>(null) }
                var videoForPlaylistDialog by remember { mutableStateOf<Video?>(null) }
                var videoToRename by remember { mutableStateOf<Video?>(null) }

                // --- NEW: State for view style ---
                var currentViewStyle by remember { mutableStateOf(viewStyleManager.getViewStyle()) }

                val playlistsViewModel: PlaylistsViewModel = koinViewModel()
                val playlists by playlistsViewModel.playlists.collectAsState()

                val folderPath = intent.getStringExtra("FILTER_FOLDER_PATH")
                val screenTitle = folderPath?.let { File(it).name } ?: "All Videos"
                val isFolderView = folderPath != null

                // --- NEW: Use Scaffold to place the TopAppBar ---
                Scaffold(
                    topBar = {
                        LocalTopAppBar(
                            title = screenTitle,
                            isFolderView = isFolderView,
                            currentViewStyle = currentViewStyle,
                            onToggleViewStyle = {
                                val newStyle = if (currentViewStyle == ViewStyle.GRID) ViewStyle.LIST else ViewStyle.GRID
                                viewStyleManager.saveViewStyle(newStyle)
                                currentViewStyle = newStyle
                            },
                            onBackClick = { finish() } // Finishes the activity
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        VideoListScreen(
                            videoList = state.videos,
                            isLoading = state.isLoading,
                            title = screenTitle,
                            // --- PASS VIEW STYLE ---
                            viewStyle = currentViewStyle,
                            onVideoClick = { video1 ->
                                selectedVideo = video1
                            }
                        )
                    }
                }

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
                        onRenameCLicked = { video ->
                            selectedVideo = null
                            videoToRename = video
                        },
                        onMergeClick = { video ->
                            selectedVideo = null
                            val intent = Intent(this@AllVideosActivity, VideoMergingActivity::class.java)
                            intent.putExtra("videoUri", video.uri)
                            startActivity(intent)
                        },
                        onDeleteClick = { video ->
                            selectedVideo = null
                            videoViewModel.deleteVideo(video)
                            Toast.makeText(this@AllVideosActivity, "Delete request sent for ${video.name}.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                if (videoForPlaylistDialog != null) {
                    PlaylistSelectionDialog(
                        video = videoForPlaylistDialog!!,
                        playlists = playlists,
                        onDismiss = {
                            videoForPlaylistDialog = null
                            selectedVideo = null
                        },
                        onPlaylistSelected = { playlist, videoToAdd ->
                            playlistsViewModel.addVideoToPlaylist(playlist, videoToAdd)
                            videoForPlaylistDialog = null
                            selectedVideo = null
                        },
                        onAddNewPlaylist = { playlistName ->
                            playlistsViewModel.createNewPlaylist(playlistName)
                        }
                    )
                }

                if (videoToRename != null) {
                    RenameVideoDialog(
                        video = videoToRename!!,
                        onDismiss = { videoToRename = null },
                        onRename = { video, newName ->
                            val currentFolder = intent.getStringExtra("FILTER_FOLDER_PATH")
                            videoViewModel.renameVideo(video, newName, currentFolder)
                            videoToRename = null
                            Toast.makeText(this, "Renaming video...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            loadVideosBasedOnIntent()
        } else {
            // For older versions, permission result handles the first load,
            // this handles subsequent resumes.
            loadVideosBasedOnIntent()
        }
    }
}

// --- NEW: A Local Top App Bar for this specific Activity ---
@Composable
fun LocalTopAppBar(
    title: String,
    isFolderView: Boolean,
    currentViewStyle: ViewStyle,
    onToggleViewStyle: () -> Unit,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 4.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show back button only if viewing videos from a specific folder
        if (isFolderView) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )

        IconButton(onClick = onToggleViewStyle) {
            val iconRes = if (currentViewStyle == ViewStyle.GRID) {
                R.drawable.vp_toggle_mode
            } else {
                R.drawable.menu
            }
            AsyncImage(
                model = iconRes,
                modifier = Modifier.size(25.dp),
                contentDescription = "Toggle View Style"
            )
        }
    }
}

// Note: RenameVideoDialog, SelectionDialog etc. are assumed to be in the same file or imported.




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
              //  OptionRow(iconResId = R.drawable.vid_play, text = "Play") { onPlayClick(video) }
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