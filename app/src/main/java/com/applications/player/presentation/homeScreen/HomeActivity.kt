package com.applications.player.presentation.homeScreen

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.applications.player.R
import com.applications.player.databinding.ActivityHomeBinding
import com.applications.player.presentation.videosOfFolder.VideoViewModel
import com.applications.player.presentation.videosbyfolders.FoldersViewModel
import com.applications.player.ui.theme.VideoPlayerTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.getValue

class HomeActivity : AppCompatActivity() {
    val binding: ActivityHomeBinding by lazy {
        ActivityHomeBinding.inflate(layoutInflater)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
       /* ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/



        binding.composeView.setContent {
            VideoPlayerTheme() {
                HomeActivityScreen()
            }

        }

    }
}