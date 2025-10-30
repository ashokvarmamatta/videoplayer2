package com.applications.player.domain

import com.applications.player.presentation.settings.SettingsScreenState
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<SettingsScreenState>
    suspend fun saveSettings(settings: SettingsScreenState)
    suspend fun updateShowNoMediaFiles(show: Boolean)
    suspend fun updateShowHiddenFiles(show: Boolean)
    suspend fun updateRememberAspectRatio(remember: Boolean)
    suspend fun updateLongPressToPlayAt2xSpeed(enable: Boolean)
    suspend fun updateRememberBackgroundPlay(remember: Boolean)
    suspend fun updateRememberBrightness(remember: Boolean)
    suspend fun updateDoubleTapToFastForwardAndRewind(enable: Boolean)
    suspend fun updateAutoPlayNext(enable: Boolean)
    suspend fun updateShowMusic(show: Boolean)
}
