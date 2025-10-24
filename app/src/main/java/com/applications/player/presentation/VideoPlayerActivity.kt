package com.applications.player.presentation

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import com.applications.player.R
import com.applications.player.databinding.ActivityVideoPlayerBinding
import com.applications.player.model.Video

class VideoPlayerActivity : AppCompatActivity() {

    // Note: We use nullable lateinit var for player as it's initialized in onCreate/onStart
    // and released in onStop/onDestroy.
    private var player: ExoPlayer? = null

    // Correctly initialize binding lazily
    private val binding: ActivityVideoPlayerBinding by lazy {
        ActivityVideoPlayerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view using the root of the binding object
        setContentView(binding.root)

        // Retrieve the Parcelable Video object from the Intent
        val video = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("video", Video::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("video") as? Video
        }

        Log.d("VideoPlayerActivity", "Playing video: ${video?.name ?: "NULL"}")

        if (video == null) {
            // Handle the case where no video was passed
            Log.e("VideoPlayerActivity", "Error: No Video object passed in intent.")
            finish()
            return
        }

        // The player is initialized/prepared in onStart and released in onStop
        // This ensures the player is ready when the activity is visible.
        // We do *not* call initializePlayer here, but in onStart, as recommended by ExoPlayer documentation.
    }

    override fun onStart() {
        super.onStart()
        // Initialize the player here to start playback when the activity is visible
        val video = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("video", Video::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("video") as? Video
        }

        if (video != null) {
            initializePlayer(video)
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume playback if player exists
        player?.play()
    }

    override fun onPause() {
        super.onPause()
        // Pause playback
        player?.pause()
    }

    override fun onStop() {
        super.onStop()
        // Release the player resources when the activity is no longer visible
        releasePlayer()
    }


    private fun initializePlayer(video: Video) {
        if (player == null) {
            // Create an ExoPlayer instance
            player = ExoPlayer.Builder(this).build()

            // Bind the player to the PlayerView from the binding object
            binding.playerView.player = player

            // Create a MediaItem from the video's URI (video.uri is the content URI)
            val mediaItem = MediaItem.fromUri(video.uri)

            // Set the media item and prepare the player
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true // Start playback automatically
        }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
}