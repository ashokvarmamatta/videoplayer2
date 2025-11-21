package com.applications.player.presentation.homeScreen

import com.applications.player.model.Video
import com.applications.player.presentation.videosbyfolders.Folder
import com.applications.player.util.ViewStyle
import com.applications.player.util.ViewStyleManager

data class HomeActivityState(
    var itemSelected: NavItem = NavItem.FOLDERS,

    var selectedVideo: Video? =null,

    //deleting video
    val error: String? = null,
    val success: String? = null,

    //loading videos
    val videos: List<Video> = emptyList(),
    val videosInFolder: List<Video> = emptyList(),
    var isAllLoading: Boolean = false,

    var isLoadingVideosInFolder: Boolean = false,

    //loading playlists
    // val playlists: List<Playlist> = emptyList(),
    // val isLoadingPlaylists: Boolean = false,

    //for loading folders
    val folders: List<Folder> = emptyList(),
    var isLoadingFolders: Boolean = false,

    val videoToRename: Video? = null,

    val viewStyle : ViewStyle= ViewStyle.LIST


)


// An enum to make the item selection type-safe and clear
enum class NavItem {
    FOLDERS,
    VIDEOS,
    PLAYLISTS,
    SETTINGS
}