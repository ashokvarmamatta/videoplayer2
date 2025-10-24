package com.applications.player.presentation.videoMerging

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.applications.player.presentation.videomerging.VideoMergingScreen
import com.applications.player.ui.theme.VideoPlayerTheme

class VideoMergingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val videoUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("videoUri", Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("videoUri")
        }
        setContent {
            VideoPlayerTheme {
                if (videoUri != null) {
                    VideoMergingScreen(videoUri = videoUri)
                }
            }
        }
    }
}