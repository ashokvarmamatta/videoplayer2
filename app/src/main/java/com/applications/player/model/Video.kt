package com.applications.player.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Video(
    val id: Long =99999, // Add a unique ID for LazyColumn
    val name: String ="",
    val uri: Uri = Uri.EMPTY,
    val size: Long = 999,
    val duration: Long = 100,
    val thumbnailUri: Uri? = Uri.EMPTY, // This is your thumbnail
    val folderName: String = "",
    val folderPath: String =""
) : Parcelable