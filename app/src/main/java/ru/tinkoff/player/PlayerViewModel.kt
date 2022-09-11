package ru.tinkoff.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.seconds

@Stable
class PlayerViewModel(
    private val samplesExtractor: SamplesExtractor,
    private val player: Player
) : ViewModel() {
    private val _state = MutableStateFlow(PlayerViewModelState())

    val state: StateFlow<PlayerViewModelState> = _state.asStateFlow()

    private var timerJob: Job? = null

    init {
        player.setOnCompletionListener {
            _state.update { it.copy(isPlaying = false) }
            timerJob?.cancel()
        }
        player.setOnPreparedListener { duration ->
            _state.update { it.copy(duration = duration) }
        }
    }

    fun onTrackPicked(result: Uri?) {
        //todo error handling
        if (result == null) return

        _state.update { it.copy(contentUri = result) }

        //todo cancellation
        viewModelScope.launch {
            val samples = samplesExtractor.samples(result)
            _state.update { it.copy(samples = samples.toList().toImmutableList()) }
        }
    }

    fun pickAnotherTrack() {
        player.reset()

        timerJob?.cancel()

        _state.update {
            it.copy(
                samples = null,
                contentUri = null,
                isPlaying = false,
                playerCurrentPosition = 0
            )
        }
    }

    fun seekTo(duration: Int) {
        player.seekTo(duration)
        _state.update {
            it.copy(playerCurrentPosition = duration)
        }
    }

    fun playPause() {
        val isPlaying = player.playPause()

        if (isPlaying) {
            timerJob = viewModelScope.launch {
                do {
                    _state.update {
                        it.copy(playerCurrentPosition = player.currentPosition)
                    }
                    delay(1.seconds / 30)
                } while (timerJob?.isActive == true)
            }
        } else {
            timerJob?.cancel()
        }

        _state.update {
            it.copy(isPlaying = isPlaying)
        }
    }

    fun loadUri(contentUri: Uri) {
        player.loadUri(contentUri)
    }
}

//suppress because comparing by reference in this case is enough
data class PlayerViewModelState(
    val samples: ImmutableList<Int>? = null,
    val contentUri: Uri? = null,
    val isPlaying: Boolean = false,
    val playerCurrentPosition: Int = 0,
    val duration: Int = 0
)

data class PlayerState(
    val isPlaying: Boolean = false,
    val playerCurrentPosition: Int = 0,
    val duration: Int = 0
)

interface SamplesExtractor {
    suspend fun samples(uri: Uri): IntArray
}

class Player(private val context: Context) {
    private val mediaPlayer = MediaPlayer()
    val currentPosition: Int get() = mediaPlayer.currentPosition

    fun seekTo(duration: Int) {
        mediaPlayer.seekTo(duration)
    }

    fun playPause(): Boolean {
        return if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            false
        } else {
            mediaPlayer.start()
            true
        }
    }

    fun setOnCompletionListener(listener: () -> Unit) {
        mediaPlayer.setOnCompletionListener { listener() }
    }

    fun setOnPreparedListener(listener: (Int) -> Unit) {
        mediaPlayer.setOnPreparedListener { listener(mediaPlayer.duration) }
    }

    fun loadUri(contentUri: Uri) {
        mediaPlayer.setDataSource(context, contentUri)
        mediaPlayer.prepare()
    }

    fun reset() {
        mediaPlayer.pause()
        mediaPlayer.seekTo(0)
        mediaPlayer.reset()
    }
}