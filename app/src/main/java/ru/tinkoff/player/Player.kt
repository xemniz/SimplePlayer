package ru.tinkoff.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.seconds

data class PlayerState(
    val isPlaying: Boolean = false,
    val playerCurrentPosition: Int = 0,
    val duration: Int = 0
)

class Player(private val context: Context) {
    private val mediaPlayer = MediaPlayer()

    private val _state = MutableStateFlow(PlayerState())

    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var timerJob: Job? = null

    init {
        mediaPlayer.setOnPreparedListener {
            playerStateChanged()
        }
        mediaPlayer.setOnCompletionListener {
            playerStateChanged()
        }
    }

    fun seekTo(duration: Int) {
        mediaPlayer.seekTo(duration)
        playerStateChanged()
    }

    fun playPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        } else {
            mediaPlayer.start()
        }

        if (mediaPlayer.isPlaying) {
            timerJob = scope.launch {
                do {
                    playerStateChanged()
                    delay(1.seconds / 30)
                } while (timerJob?.isActive == true)
            }
        } else {
            timerJob?.cancel()
        }

        playerStateChanged()
    }

    fun loadUri(contentUri: Uri) {
        mediaPlayer.setDataSource(context, contentUri)
        mediaPlayer.prepare()
        playerStateChanged()
    }

    fun reset() {
        timerJob?.cancel()
        mediaPlayer.pause()
        mediaPlayer.seekTo(0)
        mediaPlayer.reset()
        playerStateChanged()
    }

    private fun playerStateChanged() {
        _state.update {
            PlayerState(
                isPlaying = mediaPlayer.isPlaying,
                playerCurrentPosition = mediaPlayer.currentPosition,
                duration = mediaPlayer.duration
            )
        }
    }
}