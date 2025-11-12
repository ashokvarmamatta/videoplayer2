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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.applications.player.databinding.ActivityComposeVideoPlayerBinding
import com.applications.player.model.Video
import com.applications.player.presentation.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

@UnstableApi
class VideoPlayerActivityCompose : ComponentActivity() {

    private val viewModel: VideoPlayerViewModel by inject()
    private val settingsViewModel: SettingsViewModel by inject()
val binding: ActivityComposeVideoPlayerBinding by lazy {
    ActivityComposeVideoPlayerBinding.inflate(layoutInflater)
}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left,0, systemBars.right, systemBars.bottom)
            insets
        }


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
binding.compose.setContent {
    MaterialTheme {
        VideoPlayerScreen(
            video = video,
            viewModel = viewModel,
            onEnterPipMode = { enterPipMode(this) },
            onFinishActivity = { finish() }
        )
    }
}
        /*setContent {

        }*/
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
