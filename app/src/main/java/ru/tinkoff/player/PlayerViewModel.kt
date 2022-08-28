package ru.tinkoff.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val samplesExtractor: SamplesExtractor
) : ViewModel() {
    private val _state = MutableStateFlow(PlayerState())

    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var samplesExtractorJob: Job? = null

    fun onTrackPicked(result: Uri?) {
        //todo error handling
        if (result == null) return

        _state.update { it.copy(contentUri = result) }

        //todo cancellation
        viewModelScope.launch {
            val samples = samplesExtractor.samples(result)
            _state.update { it.copy(samples = samples) }
        }
    }

    fun pickAnotherTrack() {
        _state.update { it.copy(samples = null, contentUri = null) }
    }
}

//suppress because comparing by reference in this case is enough
@Suppress("ArrayInDataClass")
data class PlayerState(
    val samples: IntArray? = null,
    val contentUri: Uri? = null,
    val isPlaying: Boolean = true
)

interface SamplesExtractor {
    suspend fun samples(uri: Uri): IntArray
}