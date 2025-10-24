package com.applications.player.presentation.videosbyfolders

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext // Needed for ImageRequest.Builder
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.applications.player.presentation.TorrentStreamer.VideoStreamActivity

// Note: You had R.drawable.ic_launcher_foreground in the original code, but R is not used here.
// import com.applications.player.R // Kept the import if needed elsewhere, but commented out if not.

// *** NOTE: You need to define the Folder data class yourself. I'm assuming it looks like this: ***
// data class Folder(
//     val name: String,
//     val path: String,
//     val videoCount: Int,
//     val thumbnailUri: Uri // The URI object for the thumbnail
// )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosByFoldersScreen(
    folders: List<Folder>,
    onFolderClick: (Folder) -> Unit,
    onStreamLinkClicked: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Folders") })
        }
    ) { paddingValues ->

        Column( modifier = Modifier.padding(paddingValues)) {
            Text("Stream link new ", Modifier.clickable {
                onStreamLinkClicked.invoke(true)
            })

            Divider()

            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {


                items(folders, key = { it.path }) { folder ->
                    FolderCard(folder = folder, onClick = { onFolderClick(folder) })
                    Divider()
                }
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
        ) // Changed tag to avoid IDE warnings

        // Thumbnail (using the URI of the first video in the folder)
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