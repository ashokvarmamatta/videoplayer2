package com.applications.player.presentation.videoplayer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.applications.player.domain.SettingsRepository
import com.applications.player.model.Video
import com.applications.player.presentation.settings.SettingsScreenState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VideoPlayerViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository // Inject repository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(VideoPlayerState())
    val state: StateFlow<VideoPlayerState> = _state

    var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var updateJob: Job? = null

    // To hold the current settings state
    private var currentSettings = SettingsScreenState()

    val mediaSessionToken: SessionToken?
        get() = mediaSession?.token

    init {
        // --- CONSOLIDATED SETTINGS OBSERVER ---
        viewModelScope.launch {
            settingsRepository.getSettings()
                .distinctUntilChanged() // Only react when the settings object actually changes
                .collect { newSettings ->
                    // 1. Update the cached settings state
                    val oldSettings = currentSettings
                    currentSettings = newSettings

                    // 2. React to specific setting changes

                    // Handle Music/Volume change
                    if (oldSettings.showMusic != newSettings.showMusic) {
                        player?.volume = if (newSettings.showMusic) 1.0f else 0.0f
                    }

                    // Other settings can be handled here as needed...
                    // For example, if a setting affected playback speed default:
                    // if (oldSettings.defaultSpeed != newSettings.defaultSpeed) { ... }
                }
        }
    }

    // --- Public API for UI Commands ---

    fun initPlayer(video: Video) {
        if (player == null) {
            viewModelScope.launch {
                // Fetch initial settings before creating the player
                currentSettings = settingsRepository.getSettings().first()

                player = ExoPlayer.Builder(getApplication()).build().apply {
                    addListener(PlayerEventListener())
                    setMediaItem(MediaItem.fromUri(video.uri))
                    playbackParameters = PlaybackParameters(1.0f) // Default speed
                    // Set initial volume based on the fetched settings
                    volume = if (currentSettings.showMusic) 1.0f else 0.0f

                    prepare()
                    playWhenReady = true
                }
                mediaSession = MediaSession.Builder(getApplication(), player!!)
                    .build()

                startProgressUpdateJob()
            }
        }
    }

    fun releasePlayer() {
        updateJob?.cancel()
        mediaSession?.release()
        player?.release()
        mediaSession = null
        player = null
        _state.value = VideoPlayerState()
    }

    fun togglePlayPause(shouldPlay: Boolean? = null) {
        player?.let {
            // If the video has ended, restart it from the beginning
            if (it.playbackState == Player.STATE_ENDED) {
                it.seekTo(0)
                it.playWhenReady = true
            } else {
                it.playWhenReady = shouldPlay ?: !it.playWhenReady
            }
        }
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun rewind(seconds: Int = 10) {
        if (!currentSettings.doubleTapToFastForwardAndRewind) return
        player?.let {
            val newPosition = (it.currentPosition - seconds * 1000).coerceAtLeast(0L)
            it.seekTo(newPosition)
        }
    }

    fun fastForward(seconds: Int = 10) {
        if (!currentSettings.doubleTapToFastForwardAndRewind) return
        player?.let {
            val duration = it.duration.coerceAtLeast(0L)
            val newPosition = (it.currentPosition + seconds * 1000).coerceAtMost(duration)
            it.seekTo(newPosition)
        }
    }

    fun addSubtitle(subtitleUri: Uri) {
        player?.let { p ->
            val currentMediaItem = p.currentMediaItem ?: return
            val currentPosition = p.currentPosition

            // Determine the MIME type from the file extension
            val mimeType = if (subtitleUri.path?.endsWith(".vtt", ignoreCase = true) == true) {
                MimeTypes.TEXT_VTT
            } else {
                MimeTypes.APPLICATION_SUBRIP // Default to SRT
            }

            val subtitle = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                .setMimeType(mimeType)
                .setLanguage("en") // Optional
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()

            val newMediaItem = currentMediaItem.buildUpon()
                .setSubtitleConfigurations(listOf(subtitle))
                .build()

            p.setMediaItem(newMediaItem, currentPosition)
            p.prepare()
        }
    }

    fun setSeeking(isSeeking: Boolean) {
        _state.update { it.copy(isSeeking = isSeeking) }
    }

    fun setPlaybackSpeed(speed: Float, isTemporary: Boolean = false) {
        if (!currentSettings.longPressToPlayAt2xSpeed && isTemporary) return

        player?.let {
            it.playbackParameters = PlaybackParameters(speed)
            _state.update { currentState -> currentState.copy(playbackSpeed = speed) }
        }
    }

    fun setIsInPipMode(isInPip: Boolean) {
        _state.update { it.copy(isInPipMode = isInPip) }
    }

    private fun startProgressUpdateJob() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            while (isActive) {
                player?.let { p ->
                    _state.update { currentState ->
                        currentState.copy(
                            currentPosition = p.currentPosition.coerceAtLeast(0L),
                            duration = p.duration.coerceAtLeast(0L),
                            bufferedPosition = p.bufferedPosition.coerceAtLeast(0L),
                            isPlaying = p.isPlaying
                        )
                    }
                }
                delay(250) // Reduced frequency for optimization
            }
        }
    }

    private inner class PlayerEventListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                // If auto-play next is enabled, handle that logic
                if (currentSettings.autoPlayNext) {
                    // TODO: Implement logic to play the next video in the playlist
                } else {
                    // Otherwise, reset the current video to the start and pause it
                    player?.seekTo(0)
                    player?.playWhenReady = false
                }
            }
            if (playbackState == Player.STATE_READY) {
                _state.update { it.copy(duration = player?.duration?.coerceAtLeast(0L) ?: 0L) }
            }
        }
    }

    override fun onCleared() {
        releasePlayer()
        super.onCleared()
    }
}
