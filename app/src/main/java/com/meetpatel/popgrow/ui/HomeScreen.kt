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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    // The maths parental gate; non-null while the question box is open.
    var gate by remember { mutableStateOf<GateQuestion?>(null) }

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
                BouncyTitle(fontSize = if (screenW < 380.dp) 27 else 33)
                Text(
                    text = stringResource(R.string.subtitle),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.95f),
                )
                // A little parade of friends, so the top of the screen is warm
                // and inviting rather than just words.
                Text("🐶 🐱 🐰 🐻 🦁", fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                RainbowArch(
                    prefs, tones, haptics,
                    Modifier
                        .fillMaxWidth()
                        .height(screenH * 0.34f),
                )
                Spacer(Modifier.height(16.dp))
                ModeGrid(prefs, tones, haptics, onStart)
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
                    BouncyTitle(fontSize = 26)
                    Text(
                        text = stringResource(R.string.subtitle),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.95f),
                    )
                    Spacer(Modifier.height(14.dp))
                    ModeGrid(prefs, tones, haptics, onStart)
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

        // Parental corner: a simple tap opens a little maths question — easy
        // for a grown-up, a wall for a toddler. Wrong answer just closes it.
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
            // A clear dark gear on a light disc, so a grown-up can always find it.
            Box(
                Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    .border(2.dp, Palette.Ink.copy(alpha = 0.5f), CircleShape)
                    .clickable { gate = makeGateQuestion() },
                contentAlignment = Alignment.Center,
            ) {
                Text("⚙️", fontSize = 24.sp)
            }
        }
    }

    // The parental gate: one small sum, four answers. Only the right answer
    // opens Settings; any wrong tap simply closes the box.
    gate?.let { q ->
        AlertDialog(
            onDismissRequest = { gate = null },
            title = { Text(stringResource(R.string.gate_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.gate_hint),
                        fontSize = 14.sp,
                        color = Palette.InkSoft,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = q.text,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = Palette.Ink,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        q.options.forEach { opt ->
                            Box(
                                Modifier
                                    .background(Color(0xFF4D9BFF), RoundedCornerShape(50))
                                    .clickable {
                                        val right = opt == q.answer
                                        gate = null
                                        if (right) showSettings = true
                                    }
                                    .padding(horizontal = 18.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = opt.toString(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showSettings) {
        SettingsPanel(prefs) { showSettings = false }
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

/** The six games, each its own balloon. Tap one: it bursts, then the game opens. */
@Composable
private fun ModeGrid(prefs: Prefs, tones: ToneEngine, haptics: Haptics, onStart: (GameMode) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Each balloon wears the thing it teaches: A, 1, a shape, an animal.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ModeBalloon(stringResource(R.string.mode_az), Color(0xFF35C978), 0L, prefs, tones, haptics, symbol = "A") {
                onStart(GameMode.LETTERS)
            }
            ModeBalloon(stringResource(R.string.mode_120), Color(0xFF4D9BFF), 90L, prefs, tones, haptics, symbol = "1") {
                onStart(GameMode.NUMBERS)
            }
            ModeBalloon(stringResource(R.string.mode_colors), Color.White, 180L, prefs, tones, haptics, rainbow = true) {
                onStart(GameMode.COLORS)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ModeBalloon(stringResource(R.string.mode_shapes), Color(0xFFA98BF0), 270L, prefs, tones, haptics, symbol = "★") {
                onStart(GameMode.SHAPES)
            }
            ModeBalloon(stringResource(R.string.mode_animals), Color(0xFFFF9F43), 360L, prefs, tones, haptics, symbol = "🐶") {
                onStart(GameMode.ANIMALS)
            }
        }
    }
}

/**
 * A game's balloon on the menu. It floats in on arrival, bobs on its string,
 * and when tapped it bursts into confetti with a pop before the game opens —
 * so choosing a game is itself a little balloon-popping moment.
 */
@Composable
private fun ModeBalloon(
    label: String,
    color: Color,
    appearDelay: Long,
    prefs: Prefs,
    tones: ToneEngine,
    haptics: Haptics,
    rainbow: Boolean = false,
    symbol: String? = null,
    onStart: () -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val scope = rememberCoroutineScope()
    val enter = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    var popped by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }

    val t = rememberInfiniteTransition(label = label)
    val bob by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "bob"
    )
    val phase = appearDelay / 900f

    LaunchedEffect(Unit) {
        delay(appearDelay)
        enter.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .size(width = 92.dp, height = 116.dp)
                .graphicsLayer {
                    translationY = sin((bob + phase) * 6.2832f) * 5.dp.toPx() +
                        (1f - enter.value) * 40.dp.toPx()
                    val s = (0.2f + 0.8f * enter.value) * (1f - burst.value * 0.35f)
                    scaleX = s
                    scaleY = s
                    rotationZ = sin((bob + phase) * 6.2832f + 1.2f) * 2.5f
                    alpha = 1f - burst.value
                }
                .clickable(interactionSource = interaction, indication = null) {
                    if (popped) return@clickable
                    popped = true
                    if (prefs.soundEnabled) {
                        tones.playPop(0.75f)
                        tones.playBoing(0.5f)
                    }
                    if (prefs.hapticsEnabled) haptics.tick(false)
                    scope.launch {
                        burst.animateTo(1f, tween(260, easing = LinearEasing))
                        onStart()
                    }
                }
        ) {
            val centre = Offset(size.width / 2f, size.height * 0.42f)
            val radius = size.width * 0.40f
            drawCuteBalloon(
                c = centre,
                r = radius,
                color = color,
                dpUnit = density,
                wiggle = bob + phase,
                rainbow = rainbow,
                // A balloon carrying a symbol shows it instead of a face.
                face = symbol == null,
            )
            symbol?.let { s ->
                val layout = measurer.measure(
                    text = s,
                    style = TextStyle(
                        fontSize = (radius * 1.05f / density).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                )
                drawText(
                    layout,
                    topLeft = Offset(
                        centre.x - layout.size.width / 2f,
                        centre.y - layout.size.height / 2f,
                    )
                )
            }
            // Confetti flying out as the balloon gives way.
            val p = burst.value
            if (p > 0f) {
                val c = Offset(size.width / 2f, size.height * 0.42f)
                drawCircle(
                    Color.White.copy(alpha = (1f - p) * 0.7f),
                    p * size.width * 0.6f, c,
                    style = Stroke(width = (3f * (1f - p) + 1f) * density)
                )
                for (k in 0 until 10) {
                    val a = k * 0.6283f
                    val d = p * size.width * 0.62f
                    drawCircle(
                        if (k % 2 == 0) color else Color.White,
                        (3f + (k % 3)) * density * (1f - p * 0.5f),
                        Offset(c.x + cos(a) * d, c.y + sin(a) * d),
                        alpha = 1f - p,
                    )
                }
            }
        }
        Box(
            Modifier
                .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Palette.Ink)
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

/**
 * The grown-ups' panel: a big friendly card of switches and sliders. Every
 * control changes the game for real — nothing here is decoration.
 */
@Composable
private fun SettingsPanel(prefs: Prefs, onClose: () -> Unit) {
    var sound by remember { mutableStateOf(prefs.soundEnabled) }
    var music by remember { mutableStateOf(prefs.musicEnabled) }
    var haptic by remember { mutableStateOf(prefs.hapticsEnabled) }
    var contrast by remember { mutableStateOf(prefs.highContrast) }
    var speed by remember { mutableFloatStateOf(prefs.speed) }
    var balloonSize by remember { mutableFloatStateOf(prefs.size) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var showProgress by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF5BC0F0), RoundedCornerShape(26.dp))
                    .border(3.dp, Color.White, RoundedCornerShape(26.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = TextStyle(shadow = Shadow(Palette.Ink.copy(alpha = 0.45f), Offset(0f, 3f), 6f)),
                )
                Spacer(Modifier.height(6.dp))

                ToggleRow(stringResource(R.string.music), music) { music = it; prefs.musicEnabled = it }
                ToggleRow(stringResource(R.string.sound), sound) { sound = it; prefs.soundEnabled = it }
                ToggleRow(stringResource(R.string.haptics), haptic) { haptic = it; prefs.hapticsEnabled = it }
                ToggleRow(stringResource(R.string.high_contrast), contrast) { contrast = it; prefs.highContrast = it }

                Spacer(Modifier.height(2.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    SliderColumn(
                        label = stringResource(R.string.speed),
                        value = speed,
                        range = Prefs.SPEED_MIN..Prefs.SPEED_MAX,
                        modifier = Modifier.weight(1f),
                    ) { speed = it; prefs.speed = it }
                    SliderColumn(
                        label = stringResource(R.string.size),
                        value = balloonSize,
                        range = Prefs.SIZE_MIN..Prefs.SIZE_MAX,
                        modifier = Modifier.weight(1f),
                    ) { balloonSize = it; prefs.size = it }
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PanelButton(stringResource(R.string.progress), Modifier.weight(1f)) { showProgress = true }
                    PanelButton(stringResource(R.string.privacy), Modifier.weight(1f)) { showPrivacy = true }
                    PanelButton(stringResource(R.string.terms), Modifier.weight(1f)) { showTerms = true }
                }
                Text(
                    text = stringResource(R.string.settings_note),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }

            // The round close button, sitting on the panel's corner.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(46.dp)
                    .background(Color(0xFFE53935), CircleShape)
                    .border(3.dp, Color.White, CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(18.dp)) {
                    val w = size.width
                    drawLine(Color.White, Offset(0f, 0f), Offset(w, w), 4.5f * density, StrokeCap.Round)
                    drawLine(Color.White, Offset(w, 0f), Offset(0f, w), 4.5f * density, StrokeCap.Round)
                }
            }
        }
    }

    if (showProgress) {
        ProgressPanel(prefs) { showProgress = false }
    }

    if (showPrivacy) {
        LegalPage(
            title = stringResource(R.string.privacy_title),
            body = stringResource(R.string.privacy_full),
        ) { showPrivacy = false }
    }

    if (showTerms) {
        LegalPage(
            title = stringResource(R.string.terms_title),
            body = stringResource(R.string.terms_full),
        ) { showTerms = false }
    }
}

/**
 * The grown-ups' progress page: for every game, how many times it was played
 * and the best, average and lowest score. Scores are written as each point is
 * earned, so a game in progress already shows up here.
 */
@Composable
private fun ProgressPanel(prefs: Prefs, onClose: () -> Unit) {
    val rows = remember {
        listOf(
            GameMode.COLORS to "Colors",
            GameMode.LETTERS to "A – Z",
            GameMode.NUMBERS to "1 – 20",
            GameMode.SHAPES to "Shapes",
            GameMode.ANIMALS to "Animals",
        ).map { (mode, label) -> label to prefs.scores(mode.name) }
    }
    var cleared by remember { mutableStateOf(false) }
    val played = rows.filter { it.second.isNotEmpty() }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF5BC0F0), RoundedCornerShape(26.dp))
                    .border(3.dp, Color.White, RoundedCornerShape(26.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 18.dp),
            ) {
                Text(
                    text = stringResource(R.string.progress),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = TextStyle(shadow = Shadow(Palette.Ink.copy(alpha = 0.45f), Offset(0f, 3f), 6f)),
                )
                Spacer(Modifier.height(12.dp))

                if (cleared || played.isEmpty()) {
                    Text(
                        text = stringResource(R.string.progress_none),
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 18.dp),
                    )
                } else {
                    // Column headings.
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        Spacer(Modifier.weight(1.5f))
                        StatHead(stringResource(R.string.col_plays))
                        StatHead(stringResource(R.string.col_best))
                        StatHead(stringResource(R.string.col_avg))
                        StatHead(stringResource(R.string.col_low))
                    }
                    played.forEach { (label, scores) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = label,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1.5f),
                                style = TextStyle(shadow = Shadow(Palette.Ink.copy(alpha = 0.3f), Offset(0f, 2f), 3f)),
                            )
                            StatCell(scores.size.toString())
                            StatCell(scores.max().toString())
                            StatCell((scores.sum() / scores.size).toString())
                            StatCell(scores.min().toString())
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                PanelButton(stringResource(R.string.reset_scores), Modifier.fillMaxWidth()) {
                    prefs.clearScores()
                    cleared = true
                }
                Text(
                    text = stringResource(R.string.progress_note),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }

            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(46.dp)
                    .background(Color(0xFFE53935), CircleShape)
                    .border(3.dp, Color.White, CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(18.dp)) {
                    val w = size.width
                    drawLine(Color.White, Offset(0f, 0f), Offset(w, w), 4.5f * density, StrokeCap.Round)
                    drawLine(Color.White, Offset(w, 0f), Offset(0f, w), 4.5f * density, StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatHead(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun RowScope.StatCell(text: String) {
    Text(
        text = text,
        fontSize = 17.sp,
        fontWeight = FontWeight.Black,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f),
    )
}

/**
 * A full-page notice — the privacy policy or the terms — shown inside the app
 * rather than sending anyone to a web browser, which keeps a child safely in
 * the game. One large OK button closes it; there is nothing else to press.
 */
@Composable
private fun LegalPage(title: String, body: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .background(Color(0xFF5BC0F0), RoundedCornerShape(26.dp))
                .border(3.dp, Color.White, RoundedCornerShape(26.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                style = TextStyle(shadow = Shadow(Palette.Ink.copy(alpha = 0.45f), Offset(0f, 3f), 6f)),
            )
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(16.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = body,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Palette.Ink,
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.55f)
                    .background(Color(0xFF35C978), RoundedCornerShape(50))
                    .border(2.dp, Color.White, RoundedCornerShape(50))
                    .clickable(onClick = onClose)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.ok),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    style = TextStyle(shadow = Shadow(Palette.Ink.copy(alpha = 0.35f), Offset(0f, 3f), 5f)),
                )
            }
        }
    }
}

/** A label on the left, a chunky switch on the right. */
@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            style = TextStyle(shadow = Shadow(Palette.Ink.copy(alpha = 0.35f), Offset(0f, 2f), 4f)),
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF43C463),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF9AA6B2),
                uncheckedBorderColor = Color.White.copy(alpha = 0.7f),
                checkedBorderColor = Color.White.copy(alpha = 0.7f),
            ),
        )
    }
}

/** A titled slider — used for balloon speed and balloon size. */
@Composable
private fun SliderColumn(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onChange: (Float) -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            style = TextStyle(shadow = Shadow(Palette.Ink.copy(alpha = 0.35f), Offset(0f, 2f), 4f)),
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFFFFD54A),
                inactiveTrackColor = Color.White.copy(alpha = 0.45f),
            ),
        )
    }
}

/** A white pill button along the bottom of the panel. */
@Composable
private fun PanelButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(2.dp, Palette.Ink.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Palette.Ink, textAlign = TextAlign.Center)
    }
}

/** One little sum for the parental gate: the question, its answer, and four
 * choices (the answer plus three near-misses, shuffled). */
private class GateQuestion(val text: String, val answer: Int, val options: List<Int>)

private fun makeGateQuestion(): GateQuestion {
    val rnd = kotlin.random.Random.Default
    val a = rnd.nextInt(1, 10)
    val b = rnd.nextInt(1, 10)
    return if (rnd.nextBoolean()) {
        GateQuestion("$a + $b = ?", a + b, gateOptions(a + b))
    } else {
        val big = maxOf(a, b)
        val small = minOf(a, b)
        GateQuestion("$big - $small = ?", big - small, gateOptions(big - small))
    }
}

private fun gateOptions(answer: Int): List<Int> {
    val rnd = kotlin.random.Random.Default
    val set = linkedSetOf(answer)
    while (set.size < 4) {
        val candidate = answer + rnd.nextInt(-3, 4)
        if (candidate >= 0) set.add(candidate)
    }
    return set.shuffled()
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
