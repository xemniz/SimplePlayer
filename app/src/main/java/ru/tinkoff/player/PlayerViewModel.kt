package ru.tinkoff.player

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerViewModelState(
    val samples: ImmutableList<Int>? = null,
    val contentUri: Uri? = null,
    var playerState: PlayerState = PlayerState()
)

@Stable
class PlayerViewModel(
    private val samplesExtractor: SamplesExtractor,
    private val player: Player
) : ViewModel() {
    private val _state = MutableStateFlow(PlayerViewModelState())

    val state: StateFlow<PlayerViewModelState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            player.state.collect { playerState ->
                _state.update { it.copy(playerState = playerState) }
            }
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

    fun resetPlayer() {
        player.reset()

        _state.update {
            it.copy(
                samples = null,
                contentUri = null,
            )
        }
    }

    fun seekTo(duration: Int) {
        player.seekTo(duration)
    }

    fun playPause() {
        player.playPause()
    }

    fun loadUri(contentUri: Uri) {
        player.loadUri(contentUri)
    }
}

interface SamplesExtractor {
    suspend fun samples(uri: Uri): IntArray
}