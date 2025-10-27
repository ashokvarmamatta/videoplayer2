package com.applications.player.presentation.homeScreen

import android.content.Intent
import android.util.Log
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import coil3.compose.AsyncImage
import com.applications.player.R
import com.applications.player.presentation.playlists.AllPlaylistsScreen
import com.applications.player.presentation.settings.SettingsHomeScreen
import com.applications.player.presentation.videosOfFolder.AllVideosActivity
import com.applications.player.presentation.videosOfFolder.VideoListScreen
import com.applications.player.presentation.videosOfFolder.VideoViewModel
import com.applications.player.presentation.videosbyfolders.FoldersViewModel
import com.applications.player.presentation.videosbyfolders.VideosByFoldersScreen
import org.koin.compose.viewmodel.koinViewModel

@Preview(showBackground = true)
@Composable
fun HomeActivityScreen(
    homeActivityViewModel: HomeActivityViewModel = koinViewModel(),
    foldersViewModel: FoldersViewModel = koinViewModel(),
    videoViewModel: VideoViewModel = koinViewModel()
) {
    Scaffold(
        /* topBar = { TopAppBar() }, bottomBar = {
             BottomNavRow(homeActivityViewModel)
         }*/
    ) { paddingValues ->
        paddingValues
        val state by homeActivityViewModel.homeActivityState.collectAsState()
        val context = LocalContext.current
        val folderListState by foldersViewModel.folderListState.collectAsState()




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


                            Log.e("", "folders ${folderListState.folders}")
                            VideosByFoldersScreen(
                                folders = folderListState.folders,
                                onFolderClick = { folder ->
                                    val intent = Intent(context, AllVideosActivity::class.java)
                                    intent.putExtra("FILTER_FOLDER_PATH", folder.path)
                                    startActivity(context, intent, null)
                                }, {

                                }
                            )
                        }

                        NavItem.VIDEOS -> {
                            val state by videoViewModel.videoListState.collectAsState()
                            VideoListScreen(
                                videoList = state.videos, // Use the videos from the state
                                isLoading = state.isLoading, // Pass loading state to UI
                                title = "All Videos", // Pass dynamic title
                                onVideoClick = { video1 ->

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


                BottomNavRow(viewModel = homeActivityViewModel)
            }


        }

    }
}


@Composable
fun BottomNavRow(viewModel: HomeActivityViewModel) {
    val items = listOf(
        "Folders" to R.drawable.folder_video,
        "Videos" to R.drawable.allvideo,
        "Playlists" to R.drawable.playlist,
        "Settings" to R.drawable.settings
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White) // Set the background color to white
            .padding(vertical = 8.dp), // Add some vertical padding
        horizontalArrangement = Arrangement.SpaceAround, // Distribute items evenly
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val label = item.first
            val iconRes = item.second

            // Each item is a Column containing an Icon and a Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {


                        val navItem = when (label) {
                            "Folders" -> NavItem.FOLDERS
                            "Videos" -> NavItem.VIDEOS
                            "Playlists" -> NavItem.PLAYLISTS
                            "Settings" -> NavItem.SETTINGS
                            else -> null
                        }
                        navItem?.let {
                            viewModel.onNavigationItemSelected(navItem)
                        }


                    }
                    .padding(4.dp)
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Black // Set the icon tint to black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    textAlign = TextAlign.Center,
                    color = Color.Black // Set the text color to black
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
        AsyncImage(
            model = R.drawable.menu,
            contentDescription = "Menu Icon",
            modifier = Modifier
                .size(48.dp)
                .padding(10.dp),
            colorFilter = ColorFilter.tint(Color.Black)
        )
        Text("Video Player")
    }
}