package com.applications.player


import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import appModule


import coil3.ImageLoader
import coil3.video.VideoFrameDecoder


import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

import org.koin.core.context.startKoin


class VideoPlayerApplication : Application() {

    override fun onCreate() {

        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@VideoPlayerApplication)

            modules(appModule)

        }


        val imageLoader =
            ImageLoader.Builder(applicationContext).components { add(VideoFrameDecoder.Factory()) }
                .build()


        // This part is for setting the language when the app starts.
        val sp = getSharedPreferences("prefs", MODE_PRIVATE)
        val languageCode = sp.getString("language", "en") ?: "en"

        // Set the locale for the application's lifetime.
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)


    }

}