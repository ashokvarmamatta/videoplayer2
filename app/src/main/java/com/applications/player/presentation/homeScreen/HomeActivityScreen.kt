package com.applications.player.presentation.homeScreen

import PlaylistEntity
import PlaylistSelectionDialog
import PlaylistsViewModel
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.applications.player.R

import com.applications.player.model.Video
import com.applications.player.presentation.playlists.AllPlaylistsScreen
import com.applications.player.presentation.settings.SettingsHomeScreen
import com.applications.player.presentation.videoMerging.VideoMergingActivity
import com.applications.player.presentation.videocrop.VideoCropActivity
import com.applications.player.presentation.videoplayer.VideoPlayerActivityCompose
import com.applications.player.presentation.videosOfFolder.AllVideosActivity
import com.applications.player.presentation.videosOfFolder.SelectionDialog
import com.applications.player.presentation.videosOfFolder.VideoListScreen
import com.applications.player.presentation.videosbyfolders.VideosByFoldersScreen
import com.applications.player.presentation.videosplitting.VideoSplittingActivity
import org.koin.compose.viewmodel.koinViewModel
// Correctly import the Folder data class, assuming it's here

import com.applications.player.presentation.videosbyfolders.Folder
import com.applications.player.util.ViewStyle
import java.io.File
import java.util.concurrent.TimeUnit

@Preview(showBackground = true)
@Composable
fun HomeActivityScreen(
    homeActivityViewModel: HomeActivityViewModel = koinViewModel()
) {
    Scaffold { paddingValues ->
        paddingValues
        val state by homeActivityViewModel.homeActivityState.collectAsState()
        val playlistsViewModel: PlaylistsViewModel = koinViewModel()
        val videoToAddToPlaylist by playlistsViewModel.videoToAddToPlaylist.collectAsState()
        val playlists by playlistsViewModel.playlists.collectAsState()
        val selectedPlaylist by playlistsViewModel.selectedPlaylist.collectAsState()
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        // --- NEW: State for selected folder ---
        var selectedFolder by remember { mutableStateOf<Folder?>(null) }



        // --- NEW: Refresh data on screen resume ---
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    homeActivityViewModel.loadAllVideos()
                    homeActivityViewModel.loadFolders()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        LaunchedEffect(Unit) {
            homeActivityViewModel.onNavigationItemSelected(NavItem.FOLDERS)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                // Pass all relevant states to the TopAppBar
                TopAppBar(
                    navItem = state.itemSelected,
                    selectedPlaylist = selectedPlaylist,
                    selectedFolder = selectedFolder,
                    onBackFromPlaylist = { playlistsViewModel.onBackFromPlaylist() },
                    onBackFromFolder = {
                        selectedFolder = null
                    }, // Lambda to clear the selected folder
                    onToggleViewStyle = { homeActivityViewModel.toggleViewStyle() },
                    currentViewStyle = state.viewStyle
                )

                Spacer(modifier = Modifier.height(1.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (state.itemSelected) {
                        NavItem.FOLDERS -> {
                            if (selectedFolder == null) {
                                VideosByFoldersScreen(
                                    folders = state.folders,
                                    onFolderClick = { folder ->
                                        // --- THIS IS THE FIX ---
                                        // 1. Tell the ViewModel to start loading the videos for the clicked folder.
                                        homeActivityViewModel.loadVideosOfFolder(folder.path)

                                        // 2. Update the UI state to show the video list screen.
                                        selectedFolder = folder
                                    },
                                    viewStyle = state.viewStyle,
                                    onStreamLinkClicked = {
                                        // Handle the click if needed, or leave empty
                                    }
                                )
                            } else {
                                // This part is already correct and will now receive the data.
                                VideoListScreen(
                                    videoList = state.videosInFolder,
                                    viewStyle = state.viewStyle,
                                    isLoading = state.isLoadingVideosInFolder,
                                    title = selectedFolder!!.name,
                                    onVideoClick = { video ->
                                        homeActivityViewModel.onVideoSelected(video)
                                    },
                                    onBackPressed = {
                                        selectedFolder = null
                                        homeActivityViewModel.clearVideosInFolder()
                                    }
                                )
                            }
                        }



                        NavItem.VIDEOS -> {
                            VideoListScreen(
                                videoList = state.videos,
                                viewStyle = state.viewStyle,
                                isLoading = state.isAllLoading,
                                title = "All Videos",
                                onVideoClick = { video1 ->
                                    homeActivityViewModel.onVideoSelected(video1)
                                }
                            )
                        }

                        NavItem.PLAYLISTS -> {
                            if (selectedPlaylist == null) {
                                AllPlaylistsScreen(
                                    playlistsViewModel = playlistsViewModel,
                                    onPlaylistClick = { playlist ->
                                        playlistsViewModel.onPlaylistSelected(playlist)
                                    }
                                )
                            } else {
                                VideoListScreen(
                                    videoList = selectedPlaylist!!.videos,
                                    viewStyle = state.viewStyle,
                                    isLoading = false,
                                    title = selectedPlaylist!!.name,
                                    onVideoClick = { video1 ->
                                        homeActivityViewModel.onVideoSelected(video1)
                                    },
                                    onBackPressed = {
                                        playlistsViewModel.onBackFromPlaylist()
                                    }
                                )
                            }
                        }

                        NavItem.SETTINGS -> {
                            SettingsHomeScreen()
                        }

                        else -> Unit
                    }
                }

                BottomNavRow(viewModel = homeActivityViewModel, state, playlistsViewModel)

                if (state.selectedVideo != null) {
                    ChooseVideoFunctionality(
                        selectedVideo = state.selectedVideo,
                        context,
                        homeActivityViewModel, playlistsViewModel
                    )
                }

                // --- NEW: Handle the rename dialog ---
                if (state.videoToRename != null) {
                    RenameVideoDialog(
                        video = state.videoToRename!!,
                        onDismiss = { homeActivityViewModel.onVideoRenameSelected(null) },
                        onRename = { video, newName ->
                            homeActivityViewModel.renameVideo(video, newName)
                        }
                    )
                }

                videoToAddToPlaylist?.let { video ->
                    PlaylistSelectionDialog(
                        video = video,
                        playlists = playlists,
                        onDismiss = { playlistsViewModel.onDismissPlaylistDialog() },
                        onPlaylistSelected = { playlist, videoToAdd ->
                            playlistsViewModel.addVideoToPlaylist(playlist, videoToAdd)
                        },
                        onAddNewPlaylist = { playlistName ->
                            playlistsViewModel.createNewPlaylist(playlistName)
                        }
                    )
                }
            }
        }
    }
}

// ... BottomNavRow function remains the same ...
@Composable
fun BottomNavRow(
    viewModel: HomeActivityViewModel,
    state: HomeActivityState,
    playlistsViewModel: PlaylistsViewModel
) {
    val navItems = listOf(
        Triple("Folders", R.drawable.folders_unselected, R.drawable.folder_video),
        Triple("Videos", R.drawable.allvideo, R.drawable.allvideos_selected),
        Triple("Playlists", R.drawable.playlist, R.drawable.play_list_selected),
        Triple("Settings", R.drawable.settings, R.drawable.setting_selected)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEach { (label, unselectedIcon, selectedIcon) ->

            val currentNavItem = when (label) {
                "Folders" -> NavItem.FOLDERS
                "Videos" -> NavItem.VIDEOS
                "Playlists" -> NavItem.PLAYLISTS
                "Settings" -> NavItem.SETTINGS
                else -> throw IllegalArgumentException("Unknown nav item label: $label")
            }

            val isSelected = state.itemSelected == currentNavItem
            val iconRes = if (isSelected) selectedIcon else unselectedIcon
            val tint = Color.Black

            // --- THIS IS THE FIX ---
            // 1. Determine the background color based on the selection state.
            val backgroundColor = if (isSelected) Color.LightGray.copy(alpha = 0.4f) else Color.Transparent

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    // 2. Add a weight modifier to ensure each item takes equal space.
                    .weight(1f)
                    .clickable {
                        viewModel.onNavigationItemSelected(currentNavItem)
                        if (currentNavItem == NavItem.PLAYLISTS) {
                            playlistsViewModel.onPlaylistSelected(null)
                        }
                    }
                    // 3. Apply the background modifier with rounded corners.
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    // 4. Add padding *inside* the background.
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint = tint
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    textAlign = TextAlign.Center,
                    color = tint
                )
            }
        }
    }
}



// --- MODIFIED TopAppBar ---
// --- MODIFIED TopAppBar ---
@Composable
fun TopAppBar1(
    navItem: NavItem,
    selectedPlaylist: PlaylistEntity?,
    selectedFolder: Folder?, // Add selectedFolder parameter
    onBackFromPlaylist: () -> Unit,
    onBackFromFolder: () -> Unit // Add folder back handler
) {
    val isInsidePlaylist = navItem == NavItem.PLAYLISTS && selectedPlaylist != null
    val isInsideFolder = navItem == NavItem.FOLDERS && selectedFolder != null

    val showBackButton = isInsidePlaylist || isInsideFolder
    val title = when {
        isInsidePlaylist -> selectedPlaylist!!.name
        isInsideFolder -> selectedFolder!!.name
        else -> "Vexo Video Player"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(top = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            IconButton(onClick = {
                if (isInsidePlaylist) onBackFromPlaylist() else onBackFromFolder()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }
        } else {
            // Keep the spacer to provide padding on the left
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            // --- FIX IS HERE ---
            // Always align the text to the start (left).
            textAlign = TextAlign.Start
        )
    }
}

// In C:/Users/Ramson/StudioProjects/videoplayer2/app/src/main/java/com/applications/player/presentation/homeScreen/HomeActivityScreen.kt

// In C:/Users/Ramson/StudioProjects/videoplayer2/app/src/main/java/com/applications/player/presentation/homeScreen/HomeActivityScreen.kt

// --- MODIFY THIS COMPOSABLE ---
@Composable
fun TopAppBar(
    navItem: NavItem,
    selectedPlaylist: PlaylistEntity?,
    selectedFolder: Folder?,
    onBackFromPlaylist: () -> Unit,
    onBackFromFolder: () -> Unit,
    // --- ADD THESE PARAMETERS ---
    onToggleViewStyle: () -> Unit,
    currentViewStyle: ViewStyle
) {
    val isInsidePlaylist = navItem == NavItem.PLAYLISTS && selectedPlaylist != null
    val isInsideFolder = navItem == NavItem.FOLDERS && selectedFolder != null

    val showBackButton = isInsidePlaylist || isInsideFolder
    val title = when {
        isInsidePlaylist -> selectedPlaylist!!.name
        isInsideFolder -> selectedFolder!!.name
        else -> "Vexo Video Player"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(top = 40.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            IconButton(onClick = {
                if (isInsidePlaylist) onBackFromPlaylist() else onBackFromFolder()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
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

        // --- MODIFIED: Actions Icon ---
        IconButton(onClick = onToggleViewStyle) { // Use the passed-in click handler
            // Determine which icon to show based on the current style
            val iconRes = if (currentViewStyle == ViewStyle.GRID) {
                R.drawable.vp_toggle_mode // Show list icon to switch to list
            } else {
                R.drawable.menu // Show grid icon to switch to grid
            }
            AsyncImage(
                model = iconRes,
                modifier = Modifier.size(25.dp),
                contentDescription = "Toggle View Style"
            )
        }
    }
}


// ... ChooseVideoFunctionality function remains the same ...
@OptIn(UnstableApi::class)
@Composable
fun ChooseVideoFunctionality(
    selectedVideo: Video?,
    context: Context,
    homeActivityViewModel: HomeActivityViewModel,
    playlistsViewModel: PlaylistsViewModel
) {
    if (selectedVideo != null)

        homeActivityViewModel.onVideoSelected(null)
    val intent = Intent(context, VideoPlayerActivityCompose::class.java)
    intent.putExtra("video", selectedVideo)
    startActivity(context, intent, null)
    return
    /*SelectionDialog(
        video = selectedVideo,
        onDismiss = {homeActivityViewModel.onVideoSelected(null) },
        onPlayClick = { video ->
            homeActivityViewModel.onVideoSelected(null)
            val intent = Intent(context, VideoPlayerActivityCompose::class.java)
            intent.putExtra("video", video)
            startActivity(context, intent, null)
        },
        onAddToPlayListChecked = {
            homeActivityViewModel.onVideoSelected(null)
            playlistsViewModel.onAddToPlaylistRequest(selectedVideo)
        },
        onRenameCLicked = {video ->
            homeActivityViewModel.onVideoSelected(null)
            homeActivityViewModel.onVideoRenameSelected(video) // Show rename dialog

        },
        onSplitClick = { video ->
            homeActivityViewModel.onVideoSelected(null)
            val intent = Intent(context, VideoSplittingActivity::class.java)
            intent.putExtra("videoUri", video.uri)
            startActivity(context, intent, null)
        },
        onMergeClick = { video ->
            homeActivityViewModel.onVideoSelected(null)
            val intent = Intent(context, VideoMergingActivity::class.java)
            intent.putExtra("videoUri", video.uri)
            startActivity(context, intent, null)
        },
        // --- NEW: Handle Delete Click ---
        onDeleteClick = { video ->
            homeActivityViewModel.onVideoSelected(null)
            homeActivityViewModel.deleteVideo(video) // Corrected function call
        }
    )*/
}


/**
 * **[NEW]** A dialog for renaming a video.
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
                    Text(text = error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isNotBlank() && !newName.contains(File.separatorChar)) {
                        onRename(video, newName)
                        onDismiss()
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


@Composable
fun FolderGridItem(
    folder: Folder,
    onFolderClick: (Folder) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onFolderClick(folder) }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.folder_video), // Your folder icon
            contentDescription = folder.name,
            modifier = Modifier.size(80.dp),
            tint = Color.Unspecified // Use original icon colors
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${folder.videoCount} Videos", // Example subtext
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun VideoGridItem(
    video: Video,
    onVideoClick: (Video) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onVideoClick(video) }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // You might want an AsyncImage here if you have thumbnails
        Icon(
            painter = painterResource(id = R.drawable.allvideo), // Placeholder icon
            contentDescription = video.name,
            modifier = Modifier.size(80.dp),
            tint = Color.Black
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = video.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // You can add duration or other info as subtext
        Text(
            text = formatDuration(video.duration), // Assuming you have a formatter
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

fun formatDuration(durationMs: Long): String {
    Log.e("", "time :{$durationMs}")
    // Use the input duration directly as Long
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs.toLong())
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    Log.e("", "time :$hours:$minutes:$seconds   $totalSeconds")

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}