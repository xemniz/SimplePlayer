@file:OptIn(ExperimentalComposeUiApi::class)

package ru.tinkoff.player.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AndroidViewConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import java.lang.Float.max
import kotlin.random.Random

@Composable
fun WaveSeekBar(
    mediaPlayerCurrentPosition: Int,
    mediaPlayerDuration: Int,
    samples: IntArray?,
    onPositionChange: (Int) -> Unit
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(120.dp)
            .padding(horizontal = 16.dp),
        factory = {
            WaveformSeekBar(it).apply {
                waveProgressColor = android.graphics.Color.BLUE
                waveWidth = 2f
                onProgressChanged = object : SeekBarOnProgressChanged {
                    override fun onProgressChanged(
                        waveformSeekBar: WaveformSeekBar,
                        progress: Float,
                        fromUser: Boolean
                    ) {
                        if (!fromUser) return

                        val newPosition =
                            (mediaPlayerDuration.toFloat() / 100 * progress).toInt()

                        onPositionChange(newPosition)
                    }
                }
            }
        }, update = { bar ->
            if (bar.sample !== samples)
                samples?.let {
                    bar.setSampleFrom(it)
                }

            bar.progress =
                mediaPlayerCurrentPosition.toFloat() / mediaPlayerDuration.toFloat() * 100
        })
}

@Composable
fun CanvasWaveSeekBar(
    mediaPlayerCurrentPosition: Int,
    mediaPlayerDuration: Int,
    samples: IntArray?,
    onPositionChange: (Int) -> Unit
) {
    if (samples == null) return
    if (samples.isEmpty()) return
    val max = samples.maxOrNull() ?: return
    if (max == 0) return

    val barsCount = 1000
    val samplesInBar = samples.size.toFloat() / barsCount

    var width by remember {
        mutableStateOf(0)
    }

    val currentProgressFromPlayer = mediaPlayerCurrentPosition.toFloat() / mediaPlayerDuration
    var progressWhileDragging: Float? by remember { mutableStateOf(null) }
    val current = progressWhileDragging ?: (currentProgressFromPlayer)
    val progressInBars = barsCount * current

    val viewConfiguration = LocalViewConfiguration.current

    val color = Color.Cyan
    val progressColor = Color.Blue

    var slopConsumed by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(120.dp)
            .padding(horizontal = 16.dp)
            .pointerInput(mediaPlayerDuration) {
                detectTapGestures(
                    onPress = {
                        progressWhileDragging = it.x / width
                        slopConsumed = false
                    },
                    onTap = {
                        progressWhileDragging = null
                        val progress = it.x / width
                        val newPosition =
                            (mediaPlayerDuration.toFloat() * progress).toInt()
                        onPositionChange(newPosition)
                    },
                )
            }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    if (!slopConsumed) {
                        slopConsumed = true
                        val slopAbs = viewConfiguration.touchSlop
                        val slop = if (delta > 0) slopAbs else -slopAbs

                        progressWhileDragging = progressWhileDragging!! + slop / width
                    }
                    progressWhileDragging = progressWhileDragging!! + delta / width
                },
                onDragStopped = {
                    val progress = progressWhileDragging!!
                    progressWhileDragging = null
                    val newPosition = (mediaPlayerDuration.toFloat() * progress).toInt()
                    onPositionChange(newPosition)
                }
            )
            .onGloballyPositioned {
                width = it.size.width
            },
        verticalAlignment = Alignment.CenterVertically
    ) {

        repeat(barsCount) { bar ->
            val sampleIndex = ((bar + 1) * samplesInBar).toInt()
            val sample = (samples.getOrNull(sampleIndex) ?: 0).toFloat()

            Box(
                modifier = Modifier
                    .fillMaxHeight(fraction = max(sample / max, .01f))
                    .weight(1f)
                    .background(
                        color = if (bar < progressInBars) progressColor else color,
                        shape = RectangleShape
                    ),
            )
        }
    }
}

@Preview
@Composable
fun PreviewNewWaveSeekBar() {
    val samples = (1..1000).map { it.mod(77) }.toIntArray()
    CanvasWaveSeekBar(
        mediaPlayerCurrentPosition = 500,
        mediaPlayerDuration = 1000,
        samples = samples,
        onPositionChange = {}
    )
}

@Preview
@Composable
fun PreviewNewWaveSeekBarRand() {
    val samples = (1..1000).map { Random.nextInt() }.toIntArray()
    CanvasWaveSeekBar(
        mediaPlayerCurrentPosition = 500,
        mediaPlayerDuration = 1000,
        samples = samples,
        onPositionChange = {}
    )
}

@Preview
@Composable
fun PreviewNewWaveSeekBar100() {
    val samples = (1..1000).map { 100 }.toIntArray()
    CanvasWaveSeekBar(
        mediaPlayerCurrentPosition = 1000,
        mediaPlayerDuration = 1000,
        samples = samples,
        onPositionChange = {}
    )
}

@Preview
@Composable
fun PreviewNewWaveSeekBar100And1() {
    val samples = ((1..1000).map { 1 } + listOf(100)).toIntArray()
    CanvasWaveSeekBar(
        mediaPlayerCurrentPosition = 0,
        mediaPlayerDuration = 1000,
        samples = samples,
        onPositionChange = {}
    )
}