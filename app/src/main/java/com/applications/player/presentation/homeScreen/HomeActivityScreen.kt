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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.media3.common.util.UnstableApi
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
import java.io.File

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

        // --- NEW: State for selected folder ---
        var selectedFolder by remember { mutableStateOf<Folder?>(null) }


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
                    onBackFromFolder = { selectedFolder = null } // Lambda to clear the selected folder
                )

                Spacer(modifier = Modifier.height(1.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (state.itemSelected) {
                        NavItem.FOLDERS -> {
                            // --- MODIFICATION: Show videos if a folder is selected ---
                            if (selectedFolder == null) {
                                // Show folder list
                                VideosByFoldersScreen(
                                    folders = state.folders,
                                    onFolderClick = { folder ->
                                        // Set the selected folder instead of launching a new activity
                                        selectedFolder = folder
                                    }, {}
                                )
                            } else {
                                // --- FIX IS HERE ---
                                // Filter the main video list based on the selected folder's path
                                val videosInFolder = remember(state.videos, selectedFolder) {
                                    state.videos.filter { video ->
                                        video.folderPath == selectedFolder?.path
                                    }
                                }

                                // Show videos of the selected folder
                                VideoListScreen(
                                    videoList = videosInFolder, // Use the filtered list
                                    isLoading = false,
                                    title = selectedFolder!!.name,
                                    onVideoClick = { video ->
                                        homeActivityViewModel.onVideoSelected(video)
                                    },
                                    onBackPressed = {
                                        // The app bar's back button will call this
                                        selectedFolder = null
                                    }
                                )
                            }
                        }

                        NavItem.VIDEOS -> {
                            VideoListScreen(
                                videoList = state.videos,
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
    // ... no changes needed here
    val navItems = listOf(
        Triple("Folders", R.drawable.folders_unselected, R.drawable.folder_video),
        Triple("Videos", R.drawable.allvideo, R.drawable.allvideos_selected),
        Triple(
            "Playlists",
            R.drawable.playlist,
            R.drawable.play_list_selected
        ), // Assuming you have a playlist_selected icon
        Triple(
            "Settings",
            R.drawable.settings,
            R.drawable.setting_selected
        )   // Assuming you have a settings_selected icon
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White) // Set the background color to white
            .padding(vertical = 8.dp), // Add some vertical padding
        horizontalArrangement = Arrangement.SpaceAround, // Distribute items evenly
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEach { (label, unselectedIcon, selectedIcon) ->

            // Determine which NavItem this iteration corresponds to
            val currentNavItem = when (label) {
                "Folders" -> NavItem.FOLDERS
                "Videos" -> NavItem.VIDEOS
                "Playlists" -> NavItem.PLAYLISTS
                "Settings" -> NavItem.SETTINGS
                else -> throw IllegalArgumentException("Unknown nav item label: $label")
            }

            // Check if this item is the one currently selected in the state
            val isSelected = state.itemSelected == currentNavItem

            // Dynamically choose the icon and color based on the selection status
            val iconRes = if (isSelected) selectedIcon else unselectedIcon
            //  val tint =if (isSelected) Color(0xFF6200EE) else Color.Black // Example: Purple for selected, Black for unselected
            val tint = Color.Black // Example: Purple for selected, Black for unselected

            // Each item is a Column containing an Icon and a Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        // When clicked, notify the ViewModel to update the state
                        viewModel.onNavigationItemSelected(currentNavItem)
                        if (currentNavItem == NavItem.PLAYLISTS) {
                            playlistsViewModel.onPlaylistSelected(null)
                        }
                    }
                    .padding(4.dp)
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint = tint // Apply the dynamic tint
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    textAlign = TextAlign.Center,
                    color = tint // Also apply the tint to the text for consistency
                )
            }
        }
    }
}


// --- MODIFIED TopAppBar ---
// --- MODIFIED TopAppBar ---
@Composable
fun TopAppBar(
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
        else -> "Video Player"
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
