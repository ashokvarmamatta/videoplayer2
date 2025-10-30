package com.applications.player.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applications.player.domain.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val uiState = repository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsScreenState())

    fun onShowNoMediaFilesChange(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowNoMediaFiles(show)
        }
    }

    fun onShowHiddenFilesChange(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowHiddenFiles(show)
        }
    }

    fun onRememberAspectRatioChange(remember: Boolean) {
        viewModelScope.launch {
            repository.updateRememberAspectRatio(remember)
        }
    }

    fun onLongPressToPlayAt2xSpeedChange(enable: Boolean) {
        viewModelScope.launch {
            repository.updateLongPressToPlayAt2xSpeed(enable)
        }
    }

    fun onRememberBackgroundPlayChange(remember: Boolean) {
        viewModelScope.launch {
            repository.updateRememberBackgroundPlay(remember)
        }
    }

    fun onRememberBrightnessChange(remember: Boolean) {
        viewModelScope.launch {
            repository.updateRememberBrightness(remember)
        }
    }

    fun onDoubleTapToFastForwardAndRewindChange(enable: Boolean) {
        viewModelScope.launch {
            repository.updateDoubleTapToFastForwardAndRewind(enable)
        }
    }

    fun onAutoPlayNextChange(enable: Boolean) {
        viewModelScope.launch {
            repository.updateAutoPlayNext(enable)
        }
    }

    fun onShowMusicChange(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowMusic(show)
        }
    }
}
