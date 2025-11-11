package com.applications.player.presentation.playlists


import PlaylistEntity
import PlaylistsViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.applications.player.model.Video
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AllPlaylistsScreen(
    playlistsViewModel: PlaylistsViewModel = koinViewModel(),
    // Add a lambda for navigation
    onPlaylistClick: (PlaylistEntity) -> Unit = {}
) {
    Scaffold { paddingValues ->
paddingValues
        val playlists by playlistsViewModel.playlists.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (playlists.isEmpty()) {
                // Show this message only when the list is empty
                Text(text = "No Playlists Found")
            } else {
                // Display the list of playlists
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) },
                            onDeleteClick = { playlistsViewModel.deletePlaylist(playlist) }
                        )
                        //Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistCard1(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Playlist Icon",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    // Display how many videos are in the playlist
                    text = "${playlist.videos.size} videos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button for the playlist
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Playlist",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


// Add these imports to the top of your file
// ... existing code for AllPlaylistsScreen ...
// Replace the existing PlaylistCard with this new version
@Composable
fun PlaylistCard(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 0.dp), colors = CardDefaults.cardColors(Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        )
        {
            // --- THIS IS THE REPLACEMENT ---
            // The static Icon is replaced with our new dynamic PlaylistThumbnail

            PlaylistThumbnail(
                videos = playlist.videos,
                modifier = Modifier.size(width = 100.dp, height = 60.dp)
            )
            // -----------------------------

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.videos.size} videos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Playlist",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// --- NEW COMPOSABLE FOR THE THUMBNAIL GRID ---
@Composable
fun PlaylistThumbnail(
    videos: List<Video>,
    modifier: Modifier = Modifier
) {
    // Determine the number of videos to show, taking the first 4 at most.
    val relevantVideos = videos.take(4)

    // Use a Box to draw the default icon as a background,
    // and overlay the thumbnails on top.
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)) // Clip the container to have rounded corners
    ) {
        // Default Icon, shown if there are no videos or as a fallback
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = "Playlist Icon",
            modifier = Modifier.fillMaxSize(),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) // Make it slightly transparent
        )

        // Logic to display thumbnails based on video count
        when (relevantVideos.size) {
            0 -> {
                // No videos, the default icon is already visible. Nothing more to do.
            }

            1 -> {
                // One video: show a single thumbnail that fills the container
                AsyncImage(
                    model = relevantVideos[0].uri,
                    contentDescription = "Playlist Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop // Crop to fill the space
                )
            }

            2 -> {
                // Two videos: show two halves, side-by-side
                Row(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = relevantVideos[0].uri,
                        contentDescription = "Thumbnail 1",
                        modifier = Modifier.weight(1f),
                        contentScale = ContentScale.Crop
                    )
                    AsyncImage(
                        model = relevantVideos[1].uri,
                        contentDescription = "Thumbnail 2",
                        modifier = Modifier.weight(1f),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            3 -> {
                // Three videos: one on the left, two stacked on the right
                Row(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = relevantVideos[0].uri,
                        contentDescription = "Thumbnail 1",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Column(Modifier.weight(1f)) {
                        AsyncImage(
                            model = relevantVideos[1].uri,
                            contentDescription = "Thumbnail 2",
                            modifier = Modifier.weight(1f),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = relevantVideos[2].uri,
                            contentDescription = "Thumbnail 3",
                            modifier = Modifier.weight(1f),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            else -> { // 4 or more videos
                // Four videos: show a 2x2 grid
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.weight(1f)) {
                        AsyncImage(
                            model = relevantVideos[0].uri,
                            contentDescription = "Thumbnail 1",
                            modifier = Modifier.weight(1f),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = relevantVideos[1].uri,
                            contentDescription = "Thumbnail 2",
                            modifier = Modifier.weight(1f),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Row(Modifier.weight(1f)) {
                        AsyncImage(
                            model = relevantVideos[2].uri,
                            contentDescription = "Thumbnail 3",
                            modifier = Modifier.weight(1f),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = relevantVideos[3].uri,
                            contentDescription = "Thumbnail 4",
                            modifier = Modifier.weight(1f),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}