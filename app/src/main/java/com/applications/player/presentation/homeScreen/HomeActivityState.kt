package com.applications.player.presentation.homeScreen

data class HomeActivityState(
    var itemSelected: NavItem = NavItem.FOLDERS
)


// An enum to make the item selection type-safe and clear
enum class NavItem {
    FOLDERS,
    VIDEOS,
    PLAYLISTS,
    SETTINGS
}