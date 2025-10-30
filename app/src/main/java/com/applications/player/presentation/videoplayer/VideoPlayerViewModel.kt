package com.applications.player.presentation.videoplayer


import android.app.Application
import androidx.compose.animation.core.copy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
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
import kotlinx.coroutines.flow.first
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
        // Observe settings changes and update the local variable
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                currentSettings = settings
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
            it.playWhenReady = shouldPlay ?: !it.playWhenReady
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
            if (playbackState == Player.STATE_ENDED && currentSettings.autoPlayNext) {
                // TODO: Implement logic to play the next video in the playlist
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