package com.applications.player.presentation.videosbyfolders

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.applications.player.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosByFoldersScreen(
    folders: List<Folder>,
    onFolderClick: (Folder) -> Unit,
    onStreamLinkClicked: (Boolean) -> Unit
) {
    Scaffold(
        /*topBar = {
            TopAppBar(title = { Text("Folders") })
        }*/
        /* bottomBar = {
             BottomNavigationBar()
         },*/
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->


        Column(modifier = Modifier.padding(paddingValues)) {
            /*Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier.padding(paddingValues),
                verticalAlignment = Alignment.CenterVertically
            ) {
                //load image from resource drawable R.drawable.icon1 using AsynceImage
                Spacer(modifier = Modifier.width(8.dp))
                AsyncImage(
                    model = R.drawable.menu,
                    contentDescription = "Icon",
                    modifier = Modifier
                        .width(50.dp)
                        .padding(10.dp),
                    colorFilter = ColorFilter.tint(Color.Black)
                )

                Text("Video Player ", Modifier.clickable {
                    onStreamLinkClicked.invoke(true)
                })

            }*/



            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .weight(1f),
                contentPadding = PaddingValues(8.dp)
            ) {


                items(folders, key = { it.path }) { folder ->
                    FolderCard(folder = folder, onClick = { onFolderClick(folder) })
                    Divider()
                }
            }

          //  BottomNavRow()


        }


    }
}


@Composable
fun BottomNavRow() {
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


                        if (label == "Folders") {

                        } else if (label == "Videos") {


                        } else if (label == "Playlists") {

                        } else if (label == "Settings") {

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


@Composable
fun FolderCard(folder: Folder, onClick: () -> Unit) {
    val context = LocalContext.current // Get the context for ImageRequest.Builder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Log.e(
            "FolderCard",
            "video url : ${folder.thumbnailUri}"
        )
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(folder.thumbnailUri) // Pass the File object directly
                // Optional: Specify a time frame in milliseconds (e.g., 5 seconds)
                .crossfade(true)
                .build(),
            contentDescription = "Thumbnail for your video",
            modifier = Modifier.size(60.dp),
            contentScale = ContentScale.Crop
        )



        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            Text(
                text = "${folder.videoCount} videos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Open folder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun BottomNavigationBar() {
    // A list of items to display in the navigation bar
    val items = listOf(
        "Folders" to R.drawable.folder_video, // Replace with your actual drawable
        "Videos" to R.drawable.menu,   // Replace with your actual drawable
        "Playlists" to R.drawable.menu, // Replace with your actual drawable
        "Settings" to R.drawable.menu  // Replace with your actual drawable
    )

    NavigationBar(
        // Set the background color to match the image
        //containerColor = Color(0xFF1F1F2E) // A dark color similar to the screenshot
    ) {
        items.forEachIndexed { index, item ->
            val label = item.first
            val iconRes = item.second
            val isSelected = index == 0 // "Folders" is selected in the image

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = label,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = { Text(label) },
                selected = isSelected,
                onClick = { /* TODO: Handle navigation */ },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.White.copy(alpha = 0.7f),
                    indicatorColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp) // Or a specific color you want
                )
            )
        }
    }
}
