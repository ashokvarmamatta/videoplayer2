package com.applications.player.presentation.videoplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
// FIX: ADD THIS SPECIFIC IMPORT TO RESOLVE 'Token' REFERENCE
import androidx.media3.session.SessionToken
import androidx.media3.session.legacy.MediaSessionCompat
import com.applications.player.model.Video
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(VideoPlayerState())
    val state: StateFlow<VideoPlayerState> = _state

    var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var updateJob: Job? = null

    // This reference is now resolved by the import above
    val mediaSessionToken: SessionToken?
        get() = mediaSession?.token

    // --- Public API for UI Commands ---

    fun initPlayer(video: Video) {
        if (player == null) {
            player = ExoPlayer.Builder(getApplication()).build().apply {
                addListener(PlayerEventListener())
                setMediaItem(MediaItem.fromUri(video.uri))
                // You were missing PlaybackParameters in your original snippet, I'm adding a default here
                // Note: I'm keeping the original code's preference for speed management if it exists
                playbackParameters = PlaybackParameters(1.0f)
                prepare()
                playWhenReady = true
            }

            // Initialize MediaSession and connect to player
            mediaSession = MediaSession.Builder(getApplication(), player!!)
                .build()

            startProgressUpdateJob()
        }
    }

    fun releasePlayer() {
        updateJob?.cancel()

        // Release MediaSession before releasing the player
        mediaSession?.release()
        mediaSession = null

        player?.release()
        player = null
        _state.value = VideoPlayerState()
    }

    // Since you are an Android developer and use Kotlin/Coroutines,
    // I am assuming the following helper methods exist and are correct:
    fun togglePlayPause(shouldPlay: Boolean? = null) {
        player?.let {
            val targetPlayState = shouldPlay ?: !it.playWhenReady
            it.playWhenReady = targetPlayState
        }
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun rewind(seconds: Int = 10) {
        player?.let {
            val newPosition = (it.currentPosition - seconds * 1000).coerceAtLeast(0L)
            it.seekTo(newPosition)
        }
    }

    fun fastForward(seconds: Int = 10) {
        player?.let {
            val duration = it.duration.coerceAtLeast(0L)
            val newPosition = (it.currentPosition + seconds * 1000).coerceAtMost(duration)
            it.seekTo(newPosition)
        }
    }

    fun setSeeking(isSeeking: Boolean) {
        _state.update { it.copy(isSeeking = isSeeking) }
    }

    fun setPlaybackSpeed(speed: Float) {
        player?.let {
            val params = PlaybackParameters(speed)
            it.playbackParameters = params
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
                val p = player
                if (p != null) {
                    _state.update { currentState ->
                        currentState.copy(
                            currentPosition = p.currentPosition.coerceAtLeast(0L),
                            duration = p.duration.coerceAtLeast(0L),
                            bufferedPosition = p.bufferedPosition,
                            isPlaying = p.isPlaying // Also update isPlaying here from player state
                        )
                    }
                }
                delay(100)
            }
        }
    }

    private inner class PlayerEventListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _state.update { it.copy(duration = player?.duration ?: 0L) }
                // Reapply playback parameters in case it was reset by ExoPlayer
                player?.let {
                    it.playbackParameters = PlaybackParameters(_state.value.playbackSpeed)
                }
            }
        }
    }

    override fun onCleared() {
        releasePlayer()
        super.onCleared()
    }
}