package com.meetpatel.popgrow.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetpatel.popgrow.Haptics
import com.meetpatel.popgrow.Prefs
import com.meetpatel.popgrow.R
import com.meetpatel.popgrow.audio.ToneEngine
import com.meetpatel.popgrow.game.GameMode
import com.meetpatel.popgrow.game.GameWorld
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    prefs: Prefs,
    tones: ToneEngine,
    haptics: Haptics,
    onStart: (mode: GameMode) -> Unit,
) {
    val density = LocalDensity.current.density
    val world = remember { GameWorld(twoPlayer = false, density = density) }
    val frame = remember { mutableLongStateOf(0L) }

    var showSettings by remember { mutableStateOf(false) }
    var sound by remember { mutableStateOf(prefs.soundEnabled) }
    var haptic by remember { mutableStateOf(prefs.hapticsEnabled) }

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

    // Everything is laid out from the real screen size, so it fits a phone, a
    // tablet, a laptop window or a TV — portrait or landscape — with nothing
    // hanging off the edges.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxHeight >= maxWidth
        val screenW = maxWidth
        val screenH = maxHeight

        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { world.resize(it.width.toFloat(), it.height.toFloat()) }
        ) {
            if (frame.longValue >= 0L && world.width > 0f) drawScenery(world, density)
        }

        if (portrait) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BouncyTitle(fontSize = if (screenW < 380.dp) 22 else 27)
                Text(
                    text = stringResource(R.string.subtitle),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.95f),
                )
                Spacer(Modifier.height(6.dp))
                RainbowArch(
                    prefs, tones, haptics,
                    Modifier
                        .fillMaxWidth()
                        .height(screenH * 0.34f),
                )
                Spacer(Modifier.height(16.dp))
                ModeGrid(onStart)
            }
        } else {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    BouncyTitle(fontSize = 24)
                    Text(
                        text = stringResource(R.string.subtitle),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.95f),
                    )
                    Spacer(Modifier.height(14.dp))
                    ModeGrid(onStart)
                }
                RainbowArch(
                    prefs, tones, haptics,
                    Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                )
            }
        }

        // Parental corner: hold to open, so a child cannot open Settings.
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
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
                    SettingRow(stringResource(R.string.haptics), haptic) {
                        haptic = it
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

/**
 * Seven poppable balloons standing in a rainbow arch. The balloon size and the
 * arch radius are worked out from the space available, so the arch always fits.
 */
@Composable
private fun RainbowArch(prefs: Prefs, tones: ToneEngine, haptics: Haptics, modifier: Modifier) {
    val colors = Palette.Rainbow + Palette.Warm[3]
    BoxWithConstraints(modifier) {
        val bw = minOf(maxWidth * 0.19f, maxHeight * 0.46f).coerceIn(54.dp, 120.dp)
        val bh = bw * 1.34f
        val half = maxWidth / 2
        val maxRx = half - bw / 2 - 2.dp
        val maxRy = (maxHeight - bh - 4.dp) / 0.72f
        val arcR = minOf(maxRx, maxRy)
        val baseY = maxHeight - bh / 2 - 2.dp

        colors.forEachIndexed { i, col ->
            val a = Math.toRadians(180.0 - i * 30.0)
            val bx = half + arcR * cos(a).toFloat() - bw / 2
            val by = baseY - (arcR * 0.72f) * sin(a).toFloat() - bh / 2
            PoppableBalloon(
                color = col,
                phase = i / 7f,
                faceStyle = i % 4,
                balloonWidth = bw,
                balloonHeight = bh,
                prefs = prefs,
                tones = tones,
                haptics = haptics,
                modifier = Modifier.offset(bx, by),
            )
        }
    }
}

/**
 * A balloon that lives to be popped: tap it and it bursts into a ring of
 * confetti with a pop and a boing — then a new one springs up from below,
 * so the arch is never empty for long.
 */
@Composable
private fun PoppableBalloon(
    color: Color,
    phase: Float,
    faceStyle: Int,
    balloonWidth: Dp,
    balloonHeight: Dp,
    prefs: Prefs,
    tones: ToneEngine,
    haptics: Haptics,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    var alive by remember { mutableStateOf(true) }
    val burst = remember { Animatable(0f) }
    val enter = remember { Animatable(0f) }
    val interaction = remember { MutableInteractionSource() }

    // A new balloon isn't just "there" — it inflates like a real one: little
    // puffs, each bigger than the last, until it pops full with a wobble.
    val inflate: suspend () -> Unit = {
        enter.animateTo(
            1f,
            keyframes {
                durationMillis = 1400
                0.00f at 0
                0.42f at 350
                0.36f at 500
                0.72f at 850
                0.66f at 1000
                1.07f at 1250
            }
        )
    }

    // The arch assembles itself: each balloon inflates in turn on arrival.
    LaunchedEffect(Unit) {
        delay((phase * 520).toLong())
        inflate()
    }

    val t = rememberInfiniteTransition(label = "arc$phase")
    val bob by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "bob"
    )

    Canvas(
        modifier
            .size(width = balloonWidth, height = balloonHeight)
            .graphicsLayer {
                translationY = sin((bob + phase) * 6.2832f) * 5.dp.toPx() +
                    (1f - enter.value) * 46.dp.toPx()
                scaleX = 0.05f + 0.95f * enter.value
                scaleY = 0.05f + 0.95f * enter.value
                rotationZ = sin((bob + phase) * 6.2832f + 1.2f) * 3f
            }
            .clickable(interactionSource = interaction, indication = null) {
                if (!alive) return@clickable
                alive = false
                if (prefs.soundEnabled) {
                    tones.playPop(0.7f)
                    tones.playBoing(0.5f)
                }
                if (prefs.hapticsEnabled) haptics.tick(false)
                scope.launch {
                    burst.snapTo(0.01f)
                    burst.animateTo(1f, tween(420, easing = LinearEasing))
                    delay(450)
                    burst.snapTo(0f)
                    enter.snapTo(0f)
                    alive = true
                    inflate()
                }
            }
    ) {
        if (alive) {
            drawCuteBalloon(
                c = Offset(size.width / 2f, size.height * 0.42f),
                r = size.width * 0.40f,
                color = color,
                dpUnit = density,
                wiggle = bob + phase,
                faceStyle = faceStyle,
            )
        }
        val p = burst.value
        if (p > 0f && p < 1f) {
            val cx = size.width / 2f
            val cy = size.height * 0.42f
            // A white shockwave ring plus flecks flying out.
            drawCircle(
                Color.White.copy(alpha = (1f - p) * 0.7f),
                p * size.width * 0.52f,
                Offset(cx, cy),
                style = Stroke(width = (3f * (1f - p) + 1f) * density)
            )
            for (k in 0 until 10) {
                val ang = k * 0.6283f
                val dist = p * size.width * 0.55f
                drawCircle(
                    if (k % 2 == 0) color else Color.White,
                    (3f + (k % 3)) * density * (1f - p * 0.5f),
                    Offset(cx + cos(ang) * dist, cy + sin(ang) * dist),
                    alpha = 1f - p,
                )
            }
        }
    }
}

/** The five game buttons, each wearing a little balloon, springing in one by one. */
@Composable
private fun ModeGrid(onStart: (GameMode) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeButton(stringResource(R.string.mode_play), SolidColor(Color(0xFFFF5252)), Color(0xFFFF5252), appearDelay = 0L) {
                onStart(GameMode.FREE_PLAY)
            }
            ModeButton(
                stringResource(R.string.mode_colors),
                Brush.horizontalGradient(Palette.Rainbow + Palette.Warm[3]),
                Color.White,
                rainbow = true,
                appearDelay = 90L,
            ) { onStart(GameMode.COLORS) }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeButton(stringResource(R.string.mode_az), SolidColor(Color(0xFF35C978)), Color(0xFF35C978), appearDelay = 180L) {
                onStart(GameMode.LETTERS)
            }
            ModeButton(stringResource(R.string.mode_120), SolidColor(Color(0xFF4D9BFF)), Color(0xFF4D9BFF), appearDelay = 270L) {
                onStart(GameMode.NUMBERS)
            }
        }
        Spacer(Modifier.height(10.dp))
        ModeButton(stringResource(R.string.mode_shapes), SolidColor(Color(0xFFA98BF0)), Color(0xFFA98BF0), appearDelay = 360L) {
            onStart(GameMode.SHAPES)
        }
    }
}

/** One chunky mode button with a mini balloon badge; springs in on arrival and
 * squashes when pressed. */
@Composable
private fun ModeButton(
    label: String,
    bg: Brush,
    balloonColor: Color,
    rainbow: Boolean = false,
    appearDelay: Long = 0L,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.9f else 1f, label = "press")
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(appearDelay)
        appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Row(
        Modifier
            .graphicsLayer {
                val sc = press * appear.value
                scaleX = sc
                scaleY = sc
            }
            .background(bg, RoundedCornerShape(30.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            // Generous padding makes the whole pill an easy target for a
            // toddler's finger — comfortably past the 2 cm guideline.
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(width = 36.dp, height = 48.dp)) {
            drawCuteBalloon(
                c = Offset(size.width / 2f, size.height * 0.42f),
                r = size.width * 0.44f,
                color = balloonColor,
                dpUnit = density,
                rainbow = rainbow,
                face = false,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            fontSize = 21.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            style = TextStyle(
                shadow = Shadow(Palette.Ink.copy(alpha = 0.4f), Offset(0f, 3f), 5f)
            ),
        )
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 16.sp)
        Spacer(Modifier.width(24.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun DrawScope.drawGearGlyph(color: Color) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f
    drawCircle(color, r * 0.55f, c, style = Stroke(width = r * 0.30f))
    for (i in 0 until 6) {
        val a = i * Math.PI.toFloat() / 3f
        val x = c.x + cos(a) * r * 0.82f
        val y = c.y + sin(a) * r * 0.82f
        drawCircle(color, r * 0.16f, Offset(x, y))
    }
}
