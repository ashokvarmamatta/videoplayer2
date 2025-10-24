package com.applications.player



import android.app.Application
import appModule


import coil3.ImageLoader
import coil3.video.VideoFrameDecoder



import org.koin.android.ext.koin.androidContext

import org.koin.core.context.startKoin



class VideoPlayerApplication : Application() {

    override fun onCreate() {

        super.onCreate()

        startKoin {

            androidContext(this@VideoPlayerApplication)

            modules(appModule)

        }



        val imageLoader = ImageLoader.Builder(applicationContext).components {add(VideoFrameDecoder.Factory())}.build()

    }

}