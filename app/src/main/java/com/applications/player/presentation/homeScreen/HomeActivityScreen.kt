package com.applications.player.presentation.homeScreen

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Preview(showBackground = true)
@Composable
fun HomeActivityScreen(
    homeActivityViewModel: HomeActivityViewModel = koinViewModel()
) {
    Scaffold(
        /* topBar = { TopAppBar() }, bottomBar = {
             BottomNavRow(homeActivityViewModel)
         }*/
    ) { paddingValues ->
        paddingValues
        val state by homeActivityViewModel.homeActivityState.collectAsState()
        val playlistsViewModel: PlaylistsViewModel = koinViewModel()
        val videoToAddToPlaylist by playlistsViewModel.videoToAddToPlaylist.collectAsState()
        val playlists by playlistsViewModel.playlists.collectAsState()

        val context = LocalContext.current


        LaunchedEffect(Unit) {
            // homeActivityViewModel.loadAllVideos()
            homeActivityViewModel.onNavigationItemSelected(NavItem.FOLDERS)
        }


        Box(
            modifier = Modifier
                .fillMaxSize()


        ) {

            Column {

                TopAppBar()

                Spacer(modifier = Modifier.height(8.dp))

                LaunchedEffect(state.itemSelected) {
                    Log.e("", "itemSelected ${state.itemSelected}")
                }
                Box(modifier = Modifier.weight(1f)) {
                    when (state.itemSelected) {

                        NavItem.FOLDERS -> {


                            Log.e("", "folders ${state.folders}")
                            VideosByFoldersScreen(
                                folders = state.folders,
                                onFolderClick = { folder ->
                                    val intent = Intent(context, AllVideosActivity::class.java)
                                    intent.putExtra("FILTER_FOLDER_PATH", folder.path)
                                    startActivity(context, intent, null)
                                }, {

                                }
                            )
                        }

                        NavItem.VIDEOS -> {
                            val state by homeActivityViewModel.homeActivityState.collectAsState()
                            VideoListScreen(
                                videoList = state.videos, // Use the videos from the state
                                isLoading = state.isAllLoading, // Pass loading state to UI
                                title = "All Videos", // Pass dynamic title
                                onVideoClick = { video1 ->
                                    homeActivityViewModel.onVideoSelected(video1)
                                }
                            )
                        }

                        NavItem.PLAYLISTS -> {

                            AllPlaylistsScreen()
                        }

                        NavItem.SETTINGS -> {
                            SettingsHomeScreen()
                        }

                        else -> null
                    }
                }


                BottomNavRow(viewModel = homeActivityViewModel, state)

                if (state.selectedVideo != null) {
                    ChooseVideoFunctionality(
                        selectedVideo = state.selectedVideo,
                        context,
                        homeActivityViewModel,playlistsViewModel
                    )
                }

                // 4. Show the new PlaylistSelectionDialog when its state is active
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


@Composable
fun BottomNavRow(viewModel: HomeActivityViewModel, state: HomeActivityState) {
    // Define the different states for each navigation item (label, unselected icon, selected icon)
    val navItems = listOf(
        Triple("Folders", R.drawable.folders_unselected, R.drawable.folder_video),
        Triple("Videos", R.drawable.allvideo, R.drawable.allvideos_selected),
        Triple(
            "Playlists",
            R.drawable.playlist,
            R.drawable.playlist
        ), // Assuming you have a playlist_selected icon
        Triple(
            "Settings",
            R.drawable.settings,
            R.drawable.settings
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

// Extracted the Top App Bar for clarity
@Composable
fun TopAppBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(8.dp))


        /*AsyncImage(
            model = R.drawable.menu,
            contentDescription = "Menu Icon",
            modifier = Modifier
                .size(48.dp)
                .padding(10.dp),
            colorFilter = ColorFilter.tint(Color.Black)
        )*/
        Text(
            "Video Player",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(top = 40.dp, start = 10.dp),
            fontWeight = FontWeight.Bold
        )
    }
}


@OptIn(UnstableApi::class)
@Composable
fun ChooseVideoFunctionality(
    selectedVideo: Video?,
    context: Context,
    homeActivityViewModel: HomeActivityViewModel,
    playlistsViewModel: PlaylistsViewModel
) {
    if (selectedVideo != null)
        SelectionDialog(
            video = selectedVideo!!,
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
            onSplitClick = { video ->
                homeActivityViewModel.onVideoSelected(null)
                val intent = Intent(context, VideoSplittingActivity::class.java)
                intent.putExtra("videoUri", video.uri)
                startActivity(context, intent, null)
            },
            onCropClick = { video ->
                homeActivityViewModel.onVideoSelected(null)
                val intent = Intent(context, VideoCropActivity::class.java)
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
                homeActivityViewModel.deleteVideo(video)
                // In a full MVVM/Repository pattern, this would call videoViewModel.deleteVideo(video)
                // We use a Toast and refresh here as a placeholder for the delete operation.
                Log.d("AllVideosActivity", "Attempting to delete video: ${video.name}")
                // Toast.makeText(this@AllVideosActivity, "Delete request sent for ${video.name}. List will refresh.", Toast.LENGTH_SHORT).show()
                //  loadVideosBasedOnIntent() // Refresh list immediately
            }
        )
}