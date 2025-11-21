package com.applications.player.presentation.videoplayer

import android.app.PictureInPictureParams
import android.content.Context
import android.os.Build

import android.os.Bundle
import android.util.Log
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
import com.applications.player.presentation.settings.SettingsScreenState
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

    private var currentSettings: SettingsScreenState? = null

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
                    onEnterPipMode = { enterPipMode(this) }, // Pass the function reference
                    onFinishActivity = { finish() },
                    onSettingsChanged = { newSettings -> onSettingsChanged(newSettings) }
                )
            }
        }
    }

    private fun onSettingsChanged(settings: SettingsScreenState) {
        this.currentSettings = settings
        // You can now use the updated settings in the activity
        Log.d("VideoPlayerActivityCompose", "Settings updated: $settings")
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

    override fun onPause() {
        super.onPause()
        viewModel.onPause() // Always pause the player when the activity is not in the foreground.

        // This ensures we only trigger PiP logic if the activity is being actively closed by the user (back press),
        // not when they just press the Home button.
        // `isFinishing` will be true on a back press, but false on a home press.
        if (isFinishing && !isChangingConfigurations) {
            lifecycleScope.launch {
                val settings = settingsViewModel.uiState.first()
                if (settings.rememberBackgroundPlay && viewModel.wasPlaying()) {
                    Log.e("VideoPlayerActivityCompose", "isFinishing: ${settings.rememberBackgroundPlay}")
                     enterPipMode(this@VideoPlayerActivityCompose)
                }
            }
        }
    }



    override fun onUserLeaveHint() {
        // This method is a hint that the user is leaving the activity (e.g., home button).
        // The logic is now handled in onPause(), which is called immediately after onUserLeaveHint().
        // We keep the original logic here but simplified, as onPause will be the final arbiter.
        super.onUserLeaveHint()
        lifecycleScope.launch {
           // val settings = settingsViewModel.uiState.first()
            val settings = currentSettings
            Log.e("VideoPlayerActivityCompose", "onUserLeaveHint: ${settings!!.rememberBackgroundPlay}")

            if (settings!!.rememberBackgroundPlay &&
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

    // Optional: Add onResume to handle playback resumption when returning to the app from a paused state
    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
}
