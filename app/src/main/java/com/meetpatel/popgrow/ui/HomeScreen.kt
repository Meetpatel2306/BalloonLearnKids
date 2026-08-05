package com.meetpatel.popgrow.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetpatel.popgrow.Prefs
import com.meetpatel.popgrow.R
import com.meetpatel.popgrow.game.GameWorld

@Composable
fun HomeScreen(
    prefs: Prefs,
    onStart: (twoPlayer: Boolean) -> Unit,
) {
    val density = LocalDensity.current.density
    val world = remember { GameWorld(twoPlayer = false, density = density) }
    val frame = remember { mutableLongStateOf(0L) }

    var showSettings by remember { mutableStateOf(false) }
    var sound by remember { mutableStateOf(prefs.soundEnabled) }
    var haptics by remember { mutableStateOf(prefs.hapticsEnabled) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) world.updateScenery((now - last) / 1_000_000_000f)
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
        ) {
            // Reading the frame counter inside the draw lambda re-runs only the
            // draw phase each frame, never recomposition.
            if (frame.longValue >= 0L && world.width > 0f) drawScenery(world, density)
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.title),
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Palette.Ink.copy(alpha = 0.35f),
                        offset = Offset(0f, 4f),
                        blurRadius = 10f
                    )
                ),
            )
            Text(
                text = stringResource(R.string.subtitle),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModeButton(
                    label = stringResource(R.string.mode_solo),
                    faces = 1,
                    colors = Palette.Warm,
                    phase = 0f,
                    onClick = { onStart(false) },
                )
                ModeButton(
                    label = stringResource(R.string.mode_duo),
                    faces = 2,
                    colors = Palette.Cool,
                    phase = 0.5f,
                    onClick = { onStart(true) },
                )
            }
        }

        // Parental gate. Tiny, low-contrast and hold-only, so it reads as
        // furniture to a child and as a button to an adult who is looking for one.
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.grownups_only),
                fontSize = 11.sp,
                color = Palette.Ink.copy(alpha = 0.55f),
            )
            Spacer(Modifier.width(8.dp))
            HoldToConfirm(
                diameter = 44.dp,
                holdMillis = 900,
                ringColor = Palette.Ink,
                onConfirmed = { showSettings = true },
            ) {
                Canvas(Modifier.size(20.dp)) { drawGearGlyph(Palette.InkSoft) }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    SettingRow(stringResource(R.string.sound), sound) {
                        sound = it
                        prefs.soundEnabled = it
                    }
                    Spacer(Modifier.height(8.dp))
                    SettingRow(stringResource(R.string.haptics), haptics) {
                        haptics = it
                        prefs.hapticsEnabled = it
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 16.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/**
 * The mode buttons are bubbles with faces, not words. A child who cannot read
 * still learns "one face = me alone, two faces = me and someone" in a single
 * session, and the text underneath is really there for the adult.
 */
@Composable
private fun ModeButton(
    label: String,
    faces: Int,
    colors: List<Color>,
    phase: Float,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "bob")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = (phase * 1100).toInt()),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobValue"
    )
    val interaction = remember { MutableInteractionSource() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(148.dp)
                .scale(0.97f + 0.05f * bob)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(Color.Black.copy(alpha = 0.10f), r * 0.96f, Offset(c.x, c.y + 6.dp.toPx()))
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.70f)),
                        center = Offset(c.x - r * 0.3f, c.y - r * 0.3f),
                        radius = r * 1.5f
                    ),
                    radius = r * 0.96f,
                    center = c
                )
                if (faces == 1) {
                    drawFaceBubble(c, r * 0.56f, colors[0], density)
                } else {
                    drawFaceBubble(Offset(c.x - r * 0.34f, c.y + r * 0.06f), r * 0.42f, colors[0], density)
                    drawFaceBubble(Offset(c.x + r * 0.34f, c.y - r * 0.10f), r * 0.42f, colors[2], density)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .background(Color.White.copy(alpha = 0.88f), RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
        }
    }
}

private fun DrawScope.drawFaceBubble(center: Offset, radius: Float, color: Color, dpUnit: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(lerp(color, Color.White, 0.32f), color),
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.34f),
            radius = radius * 1.4f
        ),
        radius = radius,
        center = center
    )
    val eyeR = radius * 0.12f
    val eyeY = center.y - radius * 0.10f
    drawCircle(Palette.Ink, eyeR, Offset(center.x - radius * 0.28f, eyeY))
    drawCircle(Palette.Ink, eyeR, Offset(center.x + radius * 0.28f, eyeY))
    drawArc(
        color = Palette.Ink,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.32f, center.y - radius * 0.05f),
        size = Size(radius * 0.64f, radius * 0.50f),
        style = Stroke(width = 2.5f * dpUnit, cap = StrokeCap.Round)
    )
    drawCircle(Color.White.copy(alpha = 0.75f), radius * 0.18f, Offset(center.x - radius * 0.40f, center.y - radius * 0.46f))
}

private fun DrawScope.drawGearGlyph(color: Color) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f
    drawCircle(color, r * 0.55f, c, style = Stroke(width = r * 0.30f))
    for (i in 0 until 6) {
        val a = i * Math.PI.toFloat() / 3f
        val x = c.x + kotlin.math.cos(a) * r * 0.82f
        val y = c.y + kotlin.math.sin(a) * r * 0.82f
        drawCircle(color, r * 0.16f, Offset(x, y))
    }
}
