package com.applications.player.presentation.settings

data class SettingsScreenState(
    val appLanguage: String = "English",
    val showNoMediaFiles: Boolean = false,
    val defaultScreenOrientation: String = "Auto-rotate(sensor)",
    val decoder: String = "Use HW Decoder in Priority",
    val showHiddenFiles: Boolean = false,
    val rememberAspectRatio: Boolean = true,
    val longPressToPlayAt2xSpeed: Boolean = true,
    val rememberBackgroundPlay: Boolean = true,
    val rememberBrightness: Boolean = true,
    val doubleTapToFastForwardAndRewind: Boolean = true,
    val autoPlayNext: Boolean = true,
    val showMusic: Boolean = false
)
