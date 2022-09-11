package ru.tinkoff.player.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.tinkoff.player.Dependencies
import ru.tinkoff.player.ui.theme.PlayerTheme

@Composable
fun PlayerScreen(dependencies: Dependencies) {
    PlayerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val viewModel = viewModel(initializer = { dependencies.getPlayerViewModel() })

            val state by viewModel.state.collectAsState()

            val samples = state.samples
            val contentUri = state.contentUri

            LaunchedEffect(contentUri) {
                if (contentUri == null) return@LaunchedEffect
                viewModel.loadUri(contentUri)
            }

            DisposableEffect(Unit){
                onDispose {
                    viewModel.resetPlayer()
                }
            }

            val launcher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { result ->
                    viewModel.onTrackPicked(result)
                }

            AnimatedVisibility(
                visible = contentUri != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BackHandler {
                    viewModel.resetPlayer()
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    if (!samples.isNullOrEmpty()) {
                        val barsCount = 500
                        val progressByPlayer =
                            (state.playerState.playerCurrentPosition.toFloat() / state.playerState.duration * barsCount).toInt()
                        CanvasWaveSeekBar(
                            mediaPlayerDuration = state.playerState.duration,
                            onPositionChange = {
                                viewModel.seekTo(it)
                            },
                            barsCount = barsCount,
                            barsRelativeHeights = remember(samples, barsCount) {
                                barsRelativeHeights(samples, barsCount)
                            },
                            progressByPlayer = progressByPlayer
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.playPause()
                        }) {
                        if (state.playerState.isPlaying)
                            Text(text = "Pause")
                        else
                            Text(text = "Play")
                    }
                }
            }

            AnimatedVisibility(
                visible = contentUri == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Button(
                    modifier = Modifier.wrapContentSize(),
                    onClick = {
                        launcher.launch("audio/*")
                    }) {
                    Text(text = "Pick a song")
                }
            }
        }
    }
}