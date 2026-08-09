package com.meetpatel.bubblelearnkids.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A control that only fires after a deliberate press-and-hold.
 *
 * This is the app's parental gate. A toddler jabbing at the screen will never
 * trip it, because sustaining a single touch for the better part of a second is
 * exactly the motor control this age group does not yet have — while an adult
 * does it without thinking. It doubles as the "are you sure" for leaving a game.
 */
@Composable
fun HoldToConfirm(
    modifier: Modifier = Modifier,
    diameter: Dp = 56.dp,
    holdMillis: Int = 800,
    ringColor: Color = Palette.Ink,
    onConfirmed: () -> Unit,
    content: @Composable () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pressed) {
        if (!pressed) {
            progress = 0f
            return@LaunchedEffect
        }
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val p = ((now - start) / 1_000_000f / holdMillis).coerceIn(0f, 1f)
            progress = p
            if (p >= 1f) {
                pressed = false
                onConfirmed()
                break
            }
        }
    }

    Box(
        modifier = modifier
            .size(diameter)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    var down = true
                    while (down) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) down = false
                    }
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(diameter)) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f
            drawCircle(Color.White.copy(alpha = 0.85f), size.minDimension / 2f - inset)
            drawCircle(
                color = ringColor.copy(alpha = 0.35f),
                radius = size.minDimension / 2f - inset,
                style = Stroke(width = stroke)
            )
            if (progress > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}
