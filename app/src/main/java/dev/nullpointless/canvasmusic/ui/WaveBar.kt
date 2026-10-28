package dev.nullpointless.canvasmusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated sine-wave progress bar.
 * The played portion is drawn in white, the remainder in dim gray, with a knob
 * riding the wave at the playhead. The wave drifts horizontally while playing.
 * Supports tap-to-seek and drag-to-seek.
 */
@Composable
fun WaveProgress(
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            withFrameNanos { t ->
                phase = (t / 1_600_000_000f) % 1f
            }
        }
    }

    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val shown = (if (dragging) dragFraction else progress).coerceIn(0f, 1f)
    val density = LocalDensity.current

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragging = false
                        onSeek(dragFraction)
                    },
                    onDragCancel = { dragging = false }
                ) { change, dragAmount ->
                    change.consume()
                    dragFraction = (dragFraction + dragAmount / size.width).coerceIn(0f, 1f)
                }
            }
    ) {
        val mid = size.height / 2f
        val amp = with(density) { 5.dp.toPx() }
        val lambda = with(density) { 54.dp.toPx() }
        val strokeW = with(density) { 4.dp.toPx() }
        val twoPi = (2.0 * PI).toFloat()
        val step = 5f

        fun yAt(x: Float): Float = mid + amp * sin((x / lambda) * twoPi + phase * twoPi)

        val path = Path().apply {
            moveTo(0f, yAt(0f))
            var x = step
            while (x < size.width) {
                lineTo(x, yAt(x))
                x += step
            }
            lineTo(size.width, yAt(size.width))
        }

        drawPath(path, Color(0xFF5E5E5E), style = Stroke(strokeW, cap = StrokeCap.Round))

        val px = size.width * shown
        clipRect(right = px) {
            drawPath(path, Color.White, style = Stroke(strokeW, cap = StrokeCap.Round))
        }

        drawCircle(Color.White, radius = strokeW * 1.25f, center = Offset(px, yAt(px)))
    }
}

/**
 * Thin straight progress line used on the mini player.
 */
@Composable
fun ThinProgress(progress: Float, modifier: Modifier = Modifier, color: Color = White, track: Color = Color(0xFF4D4D4D)) {
    Canvas(modifier = modifier.fillMaxWidth().height(2.dp)) {
        drawLine(
            color = track,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height
        )
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width * progress.coerceIn(0f, 1f), size.height / 2f),
            strokeWidth = size.height
        )
    }
}
