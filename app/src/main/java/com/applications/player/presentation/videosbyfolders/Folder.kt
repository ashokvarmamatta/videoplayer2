package com.applications.player.presentation.videosbyfolders

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Folder(
    val name: String,
    val path: String, // Path or bucket ID used for filtering MediaStore
    val videoCount: Int,
    val thumbnailUri: Uri? = null // Optional URI for the first video thumbnail
) : Parcelable