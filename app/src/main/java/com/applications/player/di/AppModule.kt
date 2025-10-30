// In your KoinModule.kt file or similar setup file

import androidx.room.Room
import com.applications.player.data.SettingsRepositoryImpl
import com.applications.player.data.VideoRepository
import com.applications.player.domain.SettingsRepository
import com.applications.player.presentation.homeScreen.HomeActivityViewModel
import com.applications.player.presentation.settings.SettingsViewModel

import com.applications.player.presentation.videoplayer.VideoPlayerViewModel
import com.applications.player.presentation.videosOfFolder.VideoViewModel
import com.applications.player.presentation.videosbyfolders.FoldersViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule = module {

    // --- Data Layer ---
    single { VideoRepository(get()) } // Assuming your repository needs a Context or other dependency


    viewModel { VideoViewModel(get()) }
    viewModel { FoldersViewModel(get()) }
    viewModel { VideoPlayerViewModel(androidApplication(),get()) }

    viewModel { HomeActivityViewModel(get(),get ()) }

    // ViewModel for Playlists
    viewModel {
        PlaylistsViewModel(get()) // Koin automatically provides the PlaylistRepository
    }

    single<SettingsRepository> { SettingsRepositoryImpl(androidContext()) }
    viewModel { SettingsViewModel(get()) }


    initilizeBD()


}

private fun Module.initilizeBD() {
    // Singleton for AppDatabase
    single {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            "player_database"
        ).build()
    }

// Singleton for PlaylistDao (provided from the database)
    single {
        get<AppDatabase>().playlistDao()
    }

    // Singleton for PlaylistRepository
    single {
        PlaylistRepository(get()) // Koin automatically provides the PlaylistDao
    }

}
