package com.applications.player.presentation.videosOfFolder


import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.applications.player.model.Video
import java.util.concurrent.TimeUnit

// This Composable now uses your VideoCard
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    videoList: List<Video>,
    isLoading: Boolean,
    title: String,
    onVideoClick: (Video) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = title) })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                videoList.isEmpty() -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (title == "All Videos")
                                "No videos found on the device."
                            else
                                "No videos found in the folder: $title.",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> {
                    // Display the actual list of videos using your VideoCard
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(videoList, key = { it.id }) { video ->
                            VideoCard(video = video, onClick = { onVideoClick(video) })
                        }
                    }
                }
            }
        }
    }
}

// Your provided VideoCard Composable
@Composable
fun VideoCard(video: Video, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Log.e("","video url : ${video.thumbnailUri}")
            // Using AsyncImage with the thumbnail URI for video thumbnail display
            AsyncImage(
                model = video.thumbnailUri,
                contentDescription = "Thumbnail for ${video.name}",
                modifier = Modifier.size(60.dp),
                contentScale = ContentScale.Crop
                // Ensure you have added the Coil dependencies: 'coil-compose' and 'coil-video'
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        // video.size is a Long
                        text = "Size: ${formatSize(video.size)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        // video.duration is passed as a Long
                        text = "Duration: ${formatDuration(video.duration)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// --- Utility Functions (Required for VideoCard) ---

/**
 * Formats file size in bytes to a human-readable format (e.g., 50.1 MB).
 */
fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var bytes = size.toDouble()
    var i = 0
    while (bytes >= 1024 && i < units.size - 1) {
        bytes /= 1024
        i++
    }
    return "%.1f %s".format(bytes, units[i])
}

/**
 * Formats duration in milliseconds to HH:MM:SS format.
 * Accepts Long to prevent integer overflow for long videos.
 */
fun formatDuration(durationMs: Long): String {
    Log.e("","time :{$durationMs}")
    // Use the input duration directly as Long
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs.toLong())
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    Log.e("","time :$hours:$minutes:$seconds   $totalSeconds")

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
