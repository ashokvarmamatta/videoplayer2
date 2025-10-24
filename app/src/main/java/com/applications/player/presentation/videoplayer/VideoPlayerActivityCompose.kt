package com.applications.player.presentation.videoplayer

import android.content.Context
import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.media3.common.util.UnstableApi
import com.applications.player.model.Video
import org.koin.android.ext.android.inject

@UnstableApi // Used for Media3/ExoPlayer
class VideoPlayerActivityCompose : ComponentActivity() {

    // Inject the ViewModel here to ensure it's accessible outside setContent
    private val viewModel: VideoPlayerViewModel by inject()
    private lateinit var videoData: Video

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val video = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("video", Video::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("video") as? Video
        }

        if (video == null) {
            finish()
            return
        }
        videoData = video

        // Set the Compose Content
        setContent {
            MaterialTheme {
                VideoPlayerScreen(
                    video = videoData,
                    viewModel = viewModel,
                    // Pass the function to trigger PiP from the Composable
                    onEnterPipMode = { enterPipMode(this) }
                )
            }
        }
    }

    // --- PiP Functionality ---

    private fun enterPipMode(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val aspectRatio = Rational(16, 9)

                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()

                // ------------------------------------------------------------------
                // FIX: REMOVED THE LINE BELOW TO PREVENT FORCING A PAUSE
                // viewModel.togglePlayPause(shouldPlay = false)
                // ------------------------------------------------------------------

                enterPictureInPictureMode(params)

            } catch (e: IllegalStateException) {
                // Handle error if PiP is not supported or failed
            }
        }
    }

    // This is called when the user presses the home button or navigates away
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isInPictureInPictureMode) {
            // Check if the player is currently playing before entering PiP
            if (viewModel.state.value.isPlaying) {
                enterPipMode(this)
            }
        }
    }

    // Called when the Activity enters or exits PiP mode
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)

        // Notify the ViewModel about the PiP state change
        viewModel.setIsInPipMode(isInPictureInPictureMode)
    }
}