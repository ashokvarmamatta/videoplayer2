// In your KoinModule.kt file or similar setup file

import com.applications.player.data.VideoRepository
import com.applications.player.presentation.homeScreen.HomeActivityViewModel

import com.applications.player.presentation.videoplayer.VideoPlayerViewModel
import com.applications.player.presentation.videosOfFolder.VideoViewModel
import com.applications.player.presentation.videosbyfolders.FoldersViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // --- Data Layer ---
    single { VideoRepository(get()) } // Assuming your repository needs a Context or other dependency


    viewModel { VideoViewModel(get()) }
    viewModel { FoldersViewModel(get()) }
    viewModel { VideoPlayerViewModel(androidApplication()) }

    viewModel { HomeActivityViewModel() }



}