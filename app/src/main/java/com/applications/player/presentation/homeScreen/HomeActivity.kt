package com.applications.player.presentation.homeScreen

import android.Manifest.permission
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.applications.player.R
import com.applications.player.databinding.ActivityHomeBinding
import com.applications.player.presentation.TorrentStreamer.VideoStreamActivity
import com.applications.player.presentation.videosOfFolder.VideoViewModel
import com.applications.player.presentation.videosbyfolders.FoldersViewModel
import com.applications.player.presentation.videosbyfolders.VideosByFoldersScreen
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


        val permChecker = registerForActivityResult<Array<String>, Map<String, Boolean>>(
            ActivityResultContracts.RequestMultiplePermissions(),
            ActivityResultCallback { result ->
                for (s in result.keys) {
                    if (result[s] == false) {
                        // User denied a crucial permission
                        Toast.makeText(
                            this,
                            "Storage permission required to view videos.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@ActivityResultCallback
                    }

                    setContent {
                        VideoPlayerTheme() {
                            HomeActivityScreen()
                        }

                    }
                }
            })


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                setContent {
                    VideoPlayerTheme() {
                        HomeActivityScreen()
                    }

                }
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                // Request 'All Files Access' permission
                startActivity(intent)
            }
        } else {
            // Request standard permissions for older APIs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permChecker.launch(
                    arrayOf(
                        permission.ACCESS_MEDIA_LOCATION,
                        permission.READ_EXTERNAL_STORAGE,
                        permission.WRITE_EXTERNAL_STORAGE,
                        permission.CAMERA
                    )
                )
            } else {
                permChecker.launch(
                    arrayOf(
                        permission.READ_EXTERNAL_STORAGE,
                        permission.WRITE_EXTERNAL_STORAGE,
                        permission.CAMERA
                    )
                )
            }
        }


        binding.composeView.setContent {
            VideoPlayerTheme() {
                HomeActivityScreen()
            }

        }

    }
}