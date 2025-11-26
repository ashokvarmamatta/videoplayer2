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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.applications.player.R
import com.applications.player.util.ViewStyle // <-- Import ViewStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosByFoldersScreen(folders: List<Folder>,
                          onFolderClick: (Folder) -> Unit,
                          viewStyle: ViewStyle,
                          onStreamLinkClicked: (Boolean) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        // --- FIX IS HERE: Add .fillMaxSize() to the Column ---
        Column(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize() // This ensures the Column takes up all available space
        ) {

            // --- Conditionally display Grid or List ---
            // This code is now correct because its parent has a defined size.
            if (viewStyle == ViewStyle.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.weight(1f), // .weight(1f) will now work correctly
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(folders, key = { it.path }) { folder ->
                        FolderGridItem(folder = folder, onClick = { onFolderClick(folder) })
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f), // .weight(1f) will now work correctly
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    items(folders, key = { it.path }) { folder ->
                        FolderCard(folder = folder, onClick = { onFolderClick(folder) })
                        //Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
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
            .padding(vertical = 12.dp, horizontal = 8.dp), // Increased vertical padding for list item
        verticalAlignment = Alignment.CenterVertically
    ) {
        Log.e(
            "FolderCard",
            "video url : ${folder.thumbnailUri}"
        )
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(folder.thumbnailUri) // Pass the File object directly
                .crossfade(true)
                .build(),
            contentDescription = "Thumbnail for your video",
            modifier = Modifier
                .size(width = 100.dp, height = 60.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
fun FolderGridItem(folder: Folder, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Thumbnail from the first video in the folder
        AsyncImage(
            model = folder.thumbnailUri,
            contentDescription = "Thumbnail for ${folder.name}",
            modifier = Modifier
                .height(80.dp) // Set a fixed height for a uniform grid
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)), // Adds rounded corners to the thumbnail
            contentScale = ContentScale.Crop // Ensures the image fills the space
        )


        Spacer(modifier = Modifier.height(8.dp))

        // Folder Name
        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // Video Count
        Text(
            text = "${folder.videoCount} videos",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
