package ru.tinkoff.player.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.lang.Float.max
import kotlin.random.Random

@Composable
fun CanvasWaveSeekBar(
    mediaPlayerDuration: Int,
    onPositionChange: (Int) -> Unit,
    barsCount: Int,
    barsRelativeHeights: ImmutableList<Float>,
    progressByPlayer: Int
) {
    var width by remember {
        mutableStateOf(0)
    }

    var progressWhileDragging: Int? by remember { mutableStateOf(null) }
    val progressToShow = progressWhileDragging ?: (progressByPlayer)

    val viewConfiguration = LocalViewConfiguration.current

    var slopConsumed by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(120.dp)
            .padding(horizontal = 16.dp)
            .pointerInput(barsRelativeHeights) {
                detectTapGestures(
                    onPress = {
                        progressWhileDragging = (it.x / width * barsCount).toInt()
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

                        progressWhileDragging =
                            ((progressWhileDragging!! + slop / width) * barsCount).toInt()
                    }
                    progressWhileDragging =
                        ((progressWhileDragging!! + delta / width) * barsCount).toInt()
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
        Bars(
            progressInBars = progressToShow.toInt(),
            barRelativeHeights = barsRelativeHeights
        )
    }
}

@Composable
private fun RowScope.Bars(
    progressInBars: Int,
    barRelativeHeights: ImmutableList<Float>,
) {
    val color = Color.Cyan
    val progressColor = Color.Blue

    barRelativeHeights.forEachIndexed { bar, h ->
        val fraction = max(h, .01f)
        val barColor = if (bar < progressInBars) progressColor else color
        Bar(fraction, barColor)
    }
}

@Composable
private fun RowScope.Bar(fraction: Float, barColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxHeight(fraction = fraction)
            .weight(1f)
            .background(
                color = barColor,
                shape = RectangleShape
            ),
    )
}

fun barsRelativeHeights(samples: List<Int>, barsCount: Int): ImmutableList<Float> {
    val max = samples.maxOrNull() ?: return emptyList<Float>().toImmutableList()
    if (max == 0) return emptyList<Float>().toImmutableList()
    val samplesInBar = samples.size.toFloat() / barsCount
    return List(barsCount) { bar ->
        val sampleIndex = ((bar + 1) * samplesInBar).toInt()
        val sample = (samples.getOrNull(sampleIndex) ?: 0).toFloat()
        sample / max
    }.toImmutableList()
}

@Preview
@Composable
fun PreviewNewWaveSeekBar() {
    val samples = (1..1000).map { it.mod(77) }.toImmutableList()
    CanvasWaveSeekBar(
        mediaPlayerDuration = 1000,
        {},
        1000,
        remember(samples, 1000) {
            barsRelativeHeights(samples, 1000)
        },
        (500.toFloat() / 1000 * 1000).toInt()
    )
}

@Preview
@Composable
fun PreviewNewWaveSeekBarRand() {
    val samples = (1..1000).map { Random.nextInt() }.toImmutableList()
    CanvasWaveSeekBar(
        mediaPlayerDuration = 1000,
        {},
        1000,
        remember(samples, 1000) {
            barsRelativeHeights(samples, 1000)
        },
        (500.toFloat() / 1000 * 1000).toInt()
    )
}

@Preview
@Composable
fun PreviewNewWaveSeekBar100() {
    val samples = (1..1000).map { 100 }.toImmutableList()
    CanvasWaveSeekBar(
        mediaPlayerDuration = 1000,
        {},
        1000,
        remember(samples, 1000) {
            barsRelativeHeights(samples, 1000)
        },
        (1000.toFloat() / 1000 * 1000).toInt()
    )
}

@Preview
@Composable
fun PreviewNewWaveSeekBar100And1() {
    val samples = ((1..1000).map { 1 } + listOf(100)).toImmutableList()
    CanvasWaveSeekBar(
        mediaPlayerDuration = 1000,
        {},
        1000,
        remember(samples, 1000) {
            barsRelativeHeights(samples, 1000)
        },
        (0.toFloat() / 1000 * 1000).toInt()
    )
}