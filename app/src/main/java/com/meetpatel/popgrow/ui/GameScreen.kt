package com.meetpatel.popgrow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.meetpatel.popgrow.Haptics
import com.meetpatel.popgrow.Prefs
import com.meetpatel.popgrow.audio.ToneEngine
import com.meetpatel.popgrow.game.GameWorld

@Composable
fun GameScreen(
    twoPlayer: Boolean,
    tones: ToneEngine,
    haptics: Haptics,
    prefs: Prefs,
    onExit: () -> Unit,
) {
    val density = LocalDensity.current.density
    val world = remember(twoPlayer) { GameWorld(twoPlayer = twoPlayer, density = density) }
    val frame = remember { mutableLongStateOf(0L) }
    val currentExit by rememberUpdatedState(onExit)

    LaunchedEffect(world) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) world.update((now - last) / 1_000_000_000f)
                last = now
                frame.longValue = now
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { world.resize(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(world) {
                    awaitPointerEventScope {
                        while (true) {
                            // Handled on the Initial pass and per-pointer, so two
                            // children tapping at the exact same moment both get a
                            // pop. A single-tap gesture detector would drop one.
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            for (change in event.changes) {
                                if (!change.changedToDownIgnoreConsumed()) continue
                                val pop = world.popAt(change.position.x, change.position.y)
                                if (pop != null) {
                                    if (prefs.soundEnabled) {
                                        tones.playNote(pop.noteIndex, pop.loudness)
                                        if (pop.butterfly) tones.playChord(0, 2, 4, volume = 0.5f)
                                    }
                                    if (prefs.hapticsEnabled) haptics.tick(pop.butterfly)
                                }
                            }
                        }
                    }
                }
        ) {
            if (frame.longValue >= 0L && world.width > 0f) drawWorld(world, density)
        }

        // Deliberately small and in a corner: reachable for an adult, awkward for
        // a child, and it still needs a full hold before it does anything.
        HoldToConfirm(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            diameter = 48.dp,
            holdMillis = 800,
            ringColor = Palette.Ink,
            onConfirmed = currentExit,
        ) {
            Canvas(Modifier.size(20.dp)) { drawHouseGlyph() }
        }
    }
}

private fun DrawScope.drawHouseGlyph() {
    val w = size.width
    val h = size.height
    val sw = w * 0.14f
    drawLine(Palette.InkSoft, Offset(w * 0.08f, h * 0.48f), Offset(w * 0.5f, h * 0.1f), sw, StrokeCap.Round)
    drawLine(Palette.InkSoft, Offset(w * 0.5f, h * 0.1f), Offset(w * 0.92f, h * 0.48f), sw, StrokeCap.Round)
    drawLine(Palette.InkSoft, Offset(w * 0.2f, h * 0.45f), Offset(w * 0.2f, h * 0.9f), sw, StrokeCap.Round)
    drawLine(Palette.InkSoft, Offset(w * 0.8f, h * 0.45f), Offset(w * 0.8f, h * 0.9f), sw, StrokeCap.Round)
    drawLine(Palette.InkSoft, Offset(w * 0.16f, h * 0.9f), Offset(w * 0.84f, h * 0.9f), sw, StrokeCap.Round)
}
