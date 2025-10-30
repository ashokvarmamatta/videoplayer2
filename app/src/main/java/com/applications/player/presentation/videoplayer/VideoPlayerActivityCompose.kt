package com.applications.player.presentation.videoplayer

import android.app.PictureInPictureParams
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.applications.player.model.Video
import com.applications.player.presentation.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

@UnstableApi
class VideoPlayerActivityCompose : ComponentActivity() {

    private val viewModel: VideoPlayerViewModel by inject()
    private val settingsViewModel: SettingsViewModel by inject()

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

        setContent {
            MaterialTheme {
                VideoPlayerScreen(
                    video = video,
                    viewModel = viewModel,
                    onEnterPipMode = { enterPipMode(this) }
                )
            }
        }
    }

    private fun enterPipMode(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: IllegalStateException) {
                // PiP not supported or failed
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        lifecycleScope.launch {
            val settings = settingsViewModel.uiState.first()
            if (settings.rememberBackgroundPlay &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                !isInPictureInPictureMode &&
                viewModel.state.value.isPlaying
            ) {
                enterPipMode(this@VideoPlayerActivityCompose)
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        viewModel.setIsInPipMode(isInPictureInPictureMode)
    }
}
