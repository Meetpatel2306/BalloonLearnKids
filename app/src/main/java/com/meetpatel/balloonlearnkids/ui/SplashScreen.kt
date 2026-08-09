package com.meetpatel.balloonlearnkids.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetpatel.balloonlearnkids.audio.ToneEngine
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class SBalloon(var x: Float, var y: Float, val vy: Float, val r: Float, val color: Color, val phase: Float, val style: Int)
private class SConfetti(var x: Float, var y: Float, var vx: Float, var vy: Float, val r: Float, val color: Color, var life: Float, val maxLife: Float)

private class SplashState {
    var w = 0f
    var h = 0f
    var t = 0f
    val balloons = ArrayList<SBalloon>()
    val confetti = ArrayList<SConfetti>()
    var spawnTimer = 0f
    var popTimer = 0.9f
    var spawnIndex = 0
    val rnd = Random(42)
}

/**
 * The opening moment: a sunny sky with drifting clouds and a rainbow over the
 * grass, seven rainbow balloons sailing up and popping into confetti with a
 * cheerful boing, the title bouncing letter by letter — then straight to the menu.
 */
@Composable
fun SplashScreen(onDone: () -> Unit, tones: ToneEngine, soundEnabled: Boolean) {
    val density = LocalDensity.current.density
    val state = remember { SplashState() }
    val frame = remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        delay(3200)
        onDone()
    }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                last = now
                step(state, dt, density, tones, soundEnabled)
                frame.longValue = now
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { state.w = it.width.toFloat(); state.h = it.height.toFloat() }
        ) {
            if (frame.longValue >= 0L && state.w > 0f) drawSplashScene(state, density)
        }

        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BouncyTitle(fontSize = 34)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Pop the balloons!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.95f),
            )
        }

        // Three little balloons inflating in turn — the loading wink.
        Canvas(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 42.dp)
                .size(width = 110.dp, height = 48.dp)
        ) {
            val colors = listOf(Palette.Warm[0], Palette.Cool[1], Palette.Cool[4])
            for (i in 0 until 3) {
                val s = 0.72f + 0.28f * (0.5f + 0.5f * sin(state.t * 4f - i * 1.1f))
                drawCuteBalloon(
                    c = Offset(size.width * (0.18f + 0.32f * i), size.height * 0.42f),
                    r = 9.dp.toPx() * s,
                    color = colors[i],
                    dpUnit = density,
                    wiggle = state.t * 0.3f + i * 0.3f,
                    face = false,
                )
            }
        }
    }
}

private fun step(s: SplashState, dt: Float, d: Float, tones: ToneEngine, soundEnabled: Boolean) {
    s.t += dt
    if (s.w <= 0f) return

    // Keep seven balloons in the air, cycling through the rainbow.
    s.spawnTimer -= dt
    val colors7 = Palette.Rainbow + Palette.Warm[3]
    if (s.balloons.size < 7 && s.spawnTimer <= 0f) {
        s.spawnTimer = 0.30f
        val r = (22f + s.rnd.nextFloat() * 10f) * d
        val idx = s.spawnIndex++
        s.balloons += SBalloon(
            x = s.w * (0.10f + s.rnd.nextFloat() * 0.80f),
            y = s.h + r * 2f,
            vy = (95f + s.rnd.nextFloat() * 70f) * d,
            r = r,
            color = colors7[idx % colors7.size],
            phase = s.rnd.nextFloat(),
            style = idx % 4,
        )
    }
    s.balloons.forEach { b ->
        b.y -= b.vy * dt
        b.x += sin(s.t * 1.4f + b.phase * 6.28f) * 18f * d * dt
    }

    // Pop the highest balloon once it reaches the open sky.
    s.popTimer -= dt
    if (s.popTimer <= 0f) {
        val target = s.balloons.filter { it.y < s.h * 0.55f }.minByOrNull { it.y }
        if (target != null) {
            s.popTimer = 0.5f
            s.balloons.remove(target)
            if (soundEnabled) {
                tones.playPop(0.55f)
                tones.playBoing(0.4f)
            }
            repeat(14) {
                val a = s.rnd.nextFloat() * 6.283f
                val sp = (60f + s.rnd.nextFloat() * 220f) * d
                s.confetti += SConfetti(
                    x = target.x, y = target.y,
                    vx = cos(a) * sp, vy = sin(a) * sp,
                    r = (2.5f + s.rnd.nextFloat() * 4f) * d,
                    color = if (s.rnd.nextBoolean()) target.color else Color.White,
                    life = 0.6f, maxLife = 0.6f,
                )
            }
        } else {
            s.popTimer = 0.12f
        }
    }
    s.balloons.removeAll { it.y < -it.r * 3f }

    s.confetti.forEach {
        it.vy += 900f * d * dt
        it.x += it.vx * dt
        it.y += it.vy * dt
        it.life -= dt
    }
    s.confetti.removeAll { it.life <= 0f }
}

private fun DrawScope.drawSplashScene(s: SplashState, dpUnit: Float) {
    val w = size.width
    val h = size.height
    val t = s.t

    // Sky.
    drawRect(Brush.verticalGradient(listOf(Color(0xFF6EC9F7), Color(0xFFD9F3FF))))

    // A smiling sun with slowly turning rays.
    val sc = Offset(w * 0.85f, h * 0.10f)
    val sr = 30f * dpUnit
    for (i in 0 until 12) {
        val a = i * PI.toFloat() / 6f + t * 0.3f
        drawLine(
            Color(0xFFFFD54A).copy(alpha = 0.8f),
            Offset(sc.x + cos(a) * sr * 1.25f, sc.y + sin(a) * sr * 1.25f),
            Offset(sc.x + cos(a) * sr * 1.75f, sc.y + sin(a) * sr * 1.75f),
            4f * dpUnit, StrokeCap.Round
        )
    }
    drawCircle(Color(0xFFFFE066), sr, sc)
    drawCircle(Palette.Ink, sr * 0.08f, Offset(sc.x - sr * 0.3f, sc.y - sr * 0.12f))
    drawCircle(Palette.Ink, sr * 0.08f, Offset(sc.x + sr * 0.3f, sc.y - sr * 0.12f))
    drawArc(
        Palette.Ink, 20f, 140f, false,
        topLeft = Offset(sc.x - sr * 0.35f, sc.y - sr * 0.10f),
        size = Size(sr * 0.7f, sr * 0.55f),
        style = Stroke(width = 3f * dpUnit, cap = StrokeCap.Round)
    )

    // Clouds drifting across.
    for (i in 0 until 3) {
        val speed = 12f + i * 6f
        val cw = 90f * dpUnit
        val cx = ((t * speed * dpUnit + i * w / 3f) % (w + cw * 2f)) - cw
        val cy = h * (0.13f + 0.08f * i)
        drawSplashCloud(Offset(cx, cy), 0.8f + 0.25f * i, dpUnit)
    }

    // Rainbow rising behind the grass.
    val rc = Offset(w / 2f, h * 0.99f)
    val band = 10f * dpUnit
    Palette.Rainbow.forEachIndexed { i, col ->
        val rr = w * 0.46f - i * band
        drawArc(
            col.copy(alpha = 0.95f), 180f, 180f, false,
            topLeft = Offset(rc.x - rr, rc.y - rr),
            size = Size(rr * 2f, rr * 2f),
            style = Stroke(width = band)
        )
    }

    // Grassy hills.
    drawOval(Color(0xFF57BE6B), Offset(-w * 0.2f, h * 0.90f), Size(w * 1.4f, h * 0.4f))
    drawOval(Color(0xFF3EA855), Offset(-w * 0.25f, h * 0.95f), Size(w * 1.5f, h * 0.4f))

    // The balloons and their confetti.
    s.balloons.forEach {
        drawCuteBalloon(Offset(it.x, it.y), it.r, it.color, dpUnit, wiggle = t * 0.6f + it.phase, faceStyle = it.style)
    }
    s.confetti.forEach {
        drawCircle(it.color.copy(alpha = (it.life / it.maxLife).coerceIn(0f, 1f)), it.r, Offset(it.x, it.y))
    }
}

private fun DrawScope.drawSplashCloud(c: Offset, scale: Float, dpUnit: Float) {
    val r = 22f * dpUnit * scale
    val col = Color.White.copy(alpha = 0.9f)
    drawCircle(col, r, c)
    drawCircle(col, r * 0.78f, Offset(c.x - r * 1.05f, c.y + r * 0.22f))
    drawCircle(col, r * 0.68f, Offset(c.x + r * 1.1f, c.y + r * 0.28f))
    drawCircle(col, r * 0.9f, Offset(c.x + r * 0.35f, c.y - r * 0.35f))
}
