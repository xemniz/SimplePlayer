@file:OptIn(ExperimentalPermissionsApi::class, ExperimentalTime::class)

package ru.tinkoff.player.screen

import android.Manifest
import android.media.MediaPlayer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import ru.tinkoff.player.Dependencies
import ru.tinkoff.player.ui.theme.PlayerTheme
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@Composable
fun PlayerScreen(dependencies: Dependencies) {
    PlayerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val context = LocalContext.current
            val mediaPlayer = remember { MediaPlayer() }
            val viewModel = viewModel(initializer = { dependencies.getPlayerViewModel() })

            val state by viewModel.state.collectAsState()

            val samples = state.samples
            val contentUri = state.contentUri
            var mediaPlayerCurrentPosition by remember { mutableStateOf(0) }
            var isPlaying by remember { mutableStateOf(false) }

            LaunchedEffect(mediaPlayer) {
                mediaPlayer.setOnCompletionListener { isPlaying = false }
            }

            LaunchedEffect(contentUri) {
                if (contentUri == null) return@LaunchedEffect
                mediaPlayer.setDataSource(context, contentUri)
                mediaPlayer.prepare()
            }

            val launcher =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { result ->
                    viewModel.onTrackPicked(result)
                }

            AnimatedVisibility(
                visible = contentUri != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BackHandler {
                    mediaPlayer.pause()
                    mediaPlayer.seekTo(0)
                    mediaPlayer.reset()
                    isPlaying = false
                    mediaPlayerCurrentPosition = 0
                    viewModel.pickAnotherTrack()
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CanvasWaveSeekBar(
                        mediaPlayerCurrentPosition = mediaPlayerCurrentPosition,
                        mediaPlayerDuration = mediaPlayer.duration,
                        samples = samples,
                        onPositionChange = {
                            mediaPlayer.seekTo(it)
                            mediaPlayerCurrentPosition = it
                        }
                    )

                    val storagePermissionState = rememberPermissionState(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    when (storagePermissionState.hasPermission) {
                        true -> {
                            if (isPlaying) {
                                LaunchedEffect(Unit) {
                                    while (mediaPlayer.isPlaying) {
                                        mediaPlayerCurrentPosition =
                                            mediaPlayer.currentPosition
                                        delay(1.seconds / 30)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (mediaPlayer.isPlaying) {
                                        mediaPlayer.pause()
                                        isPlaying = false
                                        return@Button
                                    }
                                    mediaPlayer.start()
                                    isPlaying = true
                                }) {
                                if (isPlaying)
                                    Text(text = "Pause")
                                else
                                    Text(text = "Play")
                            }
                        }
                        false -> {
                            Column {
                                val textToShow =
                                    if (storagePermissionState.shouldShowRationale) {
                                        "Please grant the permission."
                                    } else {
                                        "Storage permission required for this feature to be available. " +
                                                "Please grant the permission"
                                    }
                                Text(textToShow)
                                Button(onClick = { storagePermissionState.launchPermissionRequest() }) {
                                    Text("Request permission")
                                }
                            }
                        }
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
                        launcher.launch("*/*")
                    }) {
                    Text(text = "Pick a song")
                }
            }
        }
    }
}