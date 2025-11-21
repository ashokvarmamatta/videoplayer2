package com.applications.player.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applications.player.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _isDecoderDialogShown = MutableStateFlow(false)

    val uiState = combine(
        repository.getSettings(),
        _isDecoderDialogShown
    ) { settings, isDialogShown ->
        SettingsScreenState(
            appLanguage = settings.appLanguage,
            showNoMediaFiles = settings.showNoMediaFiles,
            defaultScreenOrientation = settings.defaultScreenOrientation,
            decoder = settings.decoder,
            showHiddenFiles = settings.showHiddenFiles,
            rememberAspectRatio = settings.rememberAspectRatio,
            longPressToPlayAt2xSpeed = settings.longPressToPlayAt2xSpeed,
            rememberBackgroundPlay = settings.rememberBackgroundPlay,
            rememberBrightness = settings.rememberBrightness,
            doubleTapToFastForwardAndRewind = settings.doubleTapToFastForwardAndRewind,
            autoPlayNext = settings.autoPlayNext,
            showMusic = settings.showMusic,
            isDecoderDialogShown = isDialogShown,
            // Add the brightness value
            brightness = settings.brightness
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsScreenState())


    fun onShowDecoderDialog() {
        _isDecoderDialogShown.value = true
    }

    fun onDismissDecoderDialog() {
        _isDecoderDialogShown.value = false
    }

    fun onDecoderSelected(decoder: String) {
        viewModelScope.launch {
            repository.updateDecoder(decoder)
        }
        onDismissDecoderDialog()
    }

    fun onOrientationSelected(orientation: String) {
        // The fix is here: move the repository call inside the launch block
        viewModelScope.launch {
            repository.updateDefaultScreenOrientation(orientation)
        }
    }


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

    // --- NEW FUNCTION ---
    fun onBrightnessChange(newBrightness: Int) {
        viewModelScope.launch {
            repository.updateBrightness(newBrightness)
        }
    }
}
