package com.applications.player.presentation.videosbyfolders

import android.Manifest.permission
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.applications.player.presentation.TorrentStreamer.VideoStreamActivity
import com.applications.player.presentation.videosOfFolder.AllVideosActivity
import com.applications.player.ui.theme.VideoPlayerTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class VideosByFoldersActivity : AppCompatActivity() {

    private val foldersViewModel: FoldersViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)


        val permChecker = registerForActivityResult<Array<String>, Map<String, Boolean>>(
            ActivityResultContracts.RequestMultiplePermissions(),
            ActivityResultCallback { result ->
                for (s in result.keys) {
                    if (result[s] == false) {
                        // User denied a crucial permission
                        Toast.makeText(this, "Storage permission required to view videos.", Toast.LENGTH_LONG).show()
                        return@ActivityResultCallback
                    }
                }

                setContent {
                    VideoPlayerTheme {
                        val state by foldersViewModel.folderListState.collectAsState()



                        VideosByFoldersScreen(
                            folders = state.folders,
                            onFolderClick = { folder ->
                                openAllVideosActivity(folder.path)
                            },{
startActivity(Intent(this, VideoStreamActivity::class.java))
                            }
                        )
                    }
                }
            })

        // --- Permission Logic Execution ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                setContent {
                    VideoPlayerTheme {
                        val state by foldersViewModel.folderListState.collectAsState()

                        VideosByFoldersScreen(
                            folders = state.folders,
                            onFolderClick = { folder ->
                                openAllVideosActivity(folder.path)
                            },{
                                startActivity(Intent(this, VideoStreamActivity::class.java))
                            }
                        )
                    }
                }
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                // Request 'All Files Access' permission
                startActivity(intent)
            }
        }
        else {
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


    }

    override fun onResume() {
        super.onResume()
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                TODO("VERSION.SDK_INT < R")
            }
        ) {
            setContent {
                VideoPlayerTheme {
                    val state by foldersViewModel.folderListState.collectAsState()



                    VideosByFoldersScreen(
                        folders = state.folders,
                        onFolderClick = { folder ->
                            openAllVideosActivity(folder.path)
                        },{
                            startActivity(Intent(this, VideoStreamActivity::class.java))
                        }
                    )
                }
            }
        }
    }

    private fun openAllVideosActivity(folderPath: String? = null) {
        val intent = Intent(this, AllVideosActivity::class.java).apply {
            if (folderPath != null) {
                // Pass the folder path to AllVideosActivity for filtering
                putExtra("FILTER_FOLDER_PATH", folderPath)
            }
        }
        startActivity(intent)
    }


}


