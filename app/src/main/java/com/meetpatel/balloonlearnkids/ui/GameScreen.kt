package com.meetpatel.balloonlearnkids.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetpatel.balloonlearnkids.Haptics
import com.meetpatel.balloonlearnkids.Prefs
import com.meetpatel.balloonlearnkids.audio.Ambience
import com.meetpatel.balloonlearnkids.audio.AnimalVoices
import com.meetpatel.balloonlearnkids.audio.Speaker
import com.meetpatel.balloonlearnkids.audio.ToneEngine
import com.meetpatel.balloonlearnkids.game.BubbleKind
import com.meetpatel.balloonlearnkids.game.GameMode
import com.meetpatel.balloonlearnkids.game.GameSound
import com.meetpatel.balloonlearnkids.game.GameWorld
import com.meetpatel.balloonlearnkids.game.LearningContent
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameScreen(
    mode: GameMode,
    tones: ToneEngine,
    ambience: Ambience,
    speaker: Speaker,
    animals: AnimalVoices,
    haptics: Haptics,
    prefs: Prefs,
    onExit: () -> Unit,
) {
    val density = LocalDensity.current.density
    // Bumping runKey starts a fresh round of the same mode ("Play Again").
    var runKey by remember { mutableIntStateOf(0) }
    val world = remember(mode, runKey) {
        GameWorld(
            twoPlayer = false,
            density = density,
            mode = mode,
            speedScale = prefs.speed,
            sizeScale = prefs.size,
        )
    }
    val highContrast = remember(runKey) { prefs.highContrast }
    val measurer = rememberTextMeasurer()
    val frame = remember { mutableLongStateOf(0L) }
    val currentExit by rememberUpdatedState(onExit)
    // The "you finished the whole set!" celebration.
    var showComplete by remember { mutableStateOf(false) }
    var completePending by remember { mutableStateOf(false) }
    val completeGuard by rememberUpdatedState(showComplete)

    // The child finished the whole set: let blasts and fanfares fill the screen
    // for a few seconds first — they earned a party — then offer the choices.
    LaunchedEffect(completePending) {
        if (completePending) {
            repeat(4) { i ->
                world.celebrate()
                if (prefs.soundEnabled) {
                    if (i % 2 == 0) tones.playFanfare(0.85f) else {
                        tones.playSparkle(0.6f)
                        tones.playChord(0, 2, 4, volume = 0.5f)
                    }
                }
                kotlinx.coroutines.delay(1100)
            }
            showComplete = true
            completePending = false
        }
    }
    // Mirrors world.level as Compose state so the "Level N" label updates on change.
    var levelShown by remember { mutableIntStateOf(0) }
    // Mirrors world.target so the prompt updates and the new target is spoken.
    var targetShown by remember { mutableStateOf("") }
    // Mirrors world.targetIndex so the A–Z / 1–20 strip updates. Shapes show
    // their glyphs in the strip, everything else its own text.
    var indexShown by remember { mutableIntStateOf(-1) }
    val stripLabels = remember(mode) {
        when (mode) {
            GameMode.SHAPES -> LearningContent.shapes.map { it.glyph }
            GameMode.ANIMALS -> LearningContent.animals.map { it.glyph }
            else -> world.sequence
        }
    }
    // True while a clap of thunder is already sounding, so one flash makes one roll.
    var thunderRolling by remember { mutableStateOf(false) }

    // Animals: play the call, wait for it to finish, then say the name.
    var pendingAnimal by remember { mutableStateOf<String?>(null) }
    var animalTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(animalTick) {
        val name = pendingAnimal ?: return@LaunchedEffect
        if (prefs.soundEnabled) {
            // First the name on its own…
            val spoke = kotlinx.coroutines.CompletableDeferred<Unit>()
            val speaking = speaker.say(name, flush = true) { spoke.complete(Unit) }
            if (speaking) {
                kotlinx.coroutines.withTimeoutOrNull(2500) { spoke.await() }
            }
            kotlinx.coroutines.delay(150)
            // …then the animal's own call, in full.
            if (animals.play(name)) {
                kotlinx.coroutines.delay(animals.durationMs(name) + 250L)
            }
        }
        // Only now does the next animal float up.
        world.advanceTarget()
    }

    // First visit to this mode: a hand points at the balloon to tap, for the
    // first three taps only. After that it is remembered as seen, forever.
    val modeKey = remember(mode) { mode.name }
    var hintsLeft by remember(mode) {
        mutableIntStateOf(if (prefs.tutorialSeen(modeKey)) 0 else TUTORIAL_HINTS)
    }

    // Start a fresh score for this play, so the grown-ups' progress page can
    // follow along live rather than only after the game ends.
    LaunchedEffect(mode, runKey) { prefs.sessionStart(mode.name) }

    // Say the game's name once on entry, then the first target follows.
    LaunchedEffect(mode, runKey) {
        if (world.isLearning && prefs.soundEnabled) {
            val name = when (mode) {
                GameMode.LETTERS -> "Letters"
                GameMode.NUMBERS -> "Numbers"
                GameMode.COLORS -> "Colors"
                GameMode.SHAPES -> "Shapes"
                GameMode.ANIMALS -> "Animals"
                else -> ""
            }
            if (name.isNotEmpty()) speaker.say(name, flush = true)
        }
    }
    // The "A for Apple" reward card, shown briefly after a correct answer.
    var rewardWord by remember { mutableStateOf<String?>(null) }
    var rewardEmoji by remember { mutableStateOf<String?>(null) }
    var rewardColor by remember { mutableStateOf<Color?>(null) }
    var rewardTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(rewardTick) {
        if (rewardTick > 0) {
            kotlinx.coroutines.delay(1700)
            rewardWord = null
            rewardEmoji = null
            rewardColor = null
        }
    }

    // The flying value: on a right answer the letter/number/colour lifts off
    // from the popped balloon and sails into its slot in the strip above.
    val stripSlots = remember { mutableStateMapOf<Int, Offset>() }
    var flyer by remember(runKey) { mutableStateOf<Flyer?>(null) }
    var flyTick by remember { mutableIntStateOf(0) }
    val flyProg = remember { Animatable(0f) }
    var pulseIndex by remember(runKey) { mutableIntStateOf(-1) }
    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(flyTick) {
        val f = flyer ?: return@LaunchedEffect
        flyProg.snapTo(0f)
        flyProg.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        // Landed: the slot lights up and gives a happy bounce.
        pulseIndex = f.toIndex
        flyer = null
        pulseScale.snapTo(1.6f)
        pulseScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    // Colours mode: the whole screen washes with the found colour.
    var washColor by remember(runKey) { mutableStateOf<Color?>(null) }
    var washFrom by remember { mutableStateOf(Offset.Zero) }
    var washTick by remember { mutableIntStateOf(0) }
    val washProg = remember { Animatable(0f) }

    LaunchedEffect(washTick) {
        if (washTick > 0 && washColor != null) {
            washProg.snapTo(0f)
            washProg.animateTo(1f, tween(1100))
            washColor = null
        }
    }

    // Soft wind and the odd bird, only while a game is on screen and only if the
    // grown-up has left sound on. Leaving the game stops it.
    DisposableEffect(Unit) {
        if (prefs.musicEnabled) ambience.start()
        onDispose { ambience.stop() }
    }

    LaunchedEffect(world) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) world.update((now - last) / 1_000_000_000f)
                last = now
                frame.longValue = now
                if (world.level != levelShown) levelShown = world.level
                if (world.targetIndex != indexShown) indexShown = world.targetIndex
                // Keep the soundscape matching the sky: birds by day, owls at night.
                val sky = Palette.themeAt(world.time)
                ambience.setNight(sky.night)
                // Thunder, timed to the exact frame the sky flashes.
                val flash = lightningFlash(world.time, sky.storm)
                if (flash > 0.5f && !thunderRolling) {
                    thunderRolling = true
                    if (prefs.soundEnabled) ambience.playThunder()
                } else if (flash < 0.05f) {
                    thunderRolling = false
                }
                // When the asked-for target changes, show it and say it aloud. Use
                // the queue (not flush) so it follows any "A for Apple" reward.
                if (world.target != targetShown) {
                    targetShown = world.target
                    if (prefs.soundEnabled && targetShown.isNotEmpty()) speaker.say(targetShown, flush = false)
                }
                // Non-tap sounds queued during the tick (a balloon floating away).
                if (world.pendingSounds.isNotEmpty()) {
                    if (prefs.soundEnabled) {
                        world.pendingSounds.forEach { s ->
                            when (s) {
                                GameSound.BALLOON_AWAY -> tones.playBoing(0.4f)
                            }
                        }
                    }
                    world.pendingSounds.clear()
                }
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
                                if (completeGuard) continue
                                val pop = world.popAt(change.position.x, change.position.y)
                                if (pop == null) {
                                    // Empty sky: never a mistake, but the world
                                    // still answers with a sparkle and a chime.
                                    world.touchSky(change.position.x, change.position.y)
                                    if (prefs.soundEnabled) tones.playNote(7, 0.22f)
                                    if (prefs.hapticsEnabled) haptics.tick(false)
                                }
                                if (pop != null) {
                                    // Record the point straight away: a right
                                    // answer when learning, any pop in free play.
                                    if (!world.isLearning || pop.correct) {
                                        prefs.sessionAdd(mode.name)
                                    }
                                    // The hand has done its job once the child
                                    // has tapped a few times; remember that.
                                    if (hintsLeft > 0 && (!world.isLearning || pop.correct)) {
                                        hintsLeft--
                                        if (hintsLeft == 0) prefs.markTutorialSeen(modeKey)
                                    }
                                    // Rewards show even when sound is off.
                                    if (pop.correct) {
                                        rewardWord = pop.rewardWord
                                        rewardEmoji = pop.rewardEmoji
                                        rewardColor = pop.rewardColor
                                        if (pop.rewardWord != null || pop.rewardEmoji != null) rewardTick++
                                        // Send the found value flying into its slot.
                                        if (pop.completedIndex >= 0) {
                                            val itemColor = if (mode == GameMode.COLORS) {
                                                LearningContent.colors.getOrNull(pop.completedIndex)?.color
                                                    ?: Color.White
                                            } else Palette.Gold
                                            flyer = Flyer(
                                                label = if (mode == GameMode.COLORS) null
                                                        else stripLabels.getOrNull(pop.completedIndex),
                                                color = itemColor,
                                                from = Offset(pop.x, pop.y),
                                                toIndex = pop.completedIndex,
                                            )
                                            flyTick++
                                        }
                                        // Colours: flood the screen with the found colour.
                                        if (mode == GameMode.COLORS) {
                                            washColor = LearningContent.colors
                                                .firstOrNull { it.name == pop.spokenReward }?.color
                                            washFrom = Offset(pop.x, pop.y)
                                            washTick++
                                        }
                                    }
                                    if (pop.completedSet && !completePending) completePending = true
                                    if (prefs.soundEnabled) {
                                        tones.playPop(pop.loudness * 0.6f)
                                        // Each kind of bubble has its own voice.
                                        when (pop.kind) {
                                            BubbleKind.BALLOON -> tones.playBoing(0.7f)
                                            BubbleKind.STAR -> {
                                                tones.playNote(pop.noteIndex, pop.loudness)
                                                tones.playSparkle(0.4f)
                                            }
                                            BubbleKind.HEART -> tones.playChord(2, 4, volume = 0.5f)
                                            else -> tones.playNote(pop.noteIndex, pop.loudness)
                                        }
                                        if (pop.special) tones.playSparkle(0.6f)
                                        if (pop.butterfly) tones.playChord(0, 2, 4, volume = 0.5f)
                                        if (pop.leveledUp) tones.playFanfare(0.85f)
                                        // Learning modes: say the value, cheer a match.
                                        if (pop.correct) {
                                            if (!pop.leveledUp) tones.playFanfare(0.7f)
                                            // The animal calls out first; its name
                                            // follows only once the call is done,
                                            // so the two never talk over each other.
                                            if (mode == GameMode.ANIMALS && pop.rewardWord != null) {
                                                pendingAnimal = pop.rewardWord
                                                animalTick++
                                            } else if (pop.spokenReward != null) {
                                                speaker.say(pop.spokenReward, flush = true)
                                            }
                                        } else if (pop.spoken != null) {
                                            speaker.say(pop.spoken)
                                        }
                                    }
                                    if (prefs.hapticsEnabled) {
                                        haptics.tick(pop.butterfly || pop.special || pop.leveledUp || pop.correct)
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            if (frame.longValue >= 0L && world.width > 0f) {
                drawWorld(world, density, measurer, highContrast)
            }
        }

        run {
            // The whole A–Z / 1–20 across the top: done ones bright, the current
            // one boxed, the rest faded — so a child sees the journey and what's next.
            ProgressStrip(
                mode = mode,
                sequence = stripLabels,
                // While a value is flying to its slot, the strip holds its
                // breath; it advances when the flyer lands.
                currentIndex = flyer?.toIndex ?: indexShown,
                pulseIndex = pulseIndex,
                pulseScale = pulseScale.value,
                onSlotPositioned = { i, pos -> stripSlots[i] = pos },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp),
            )
            // Below it, a bigger "find this" prompt.
            // The "find this" card — a bright cloud so the target always pops
            // out from the sky behind it.
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 46.dp)
                    .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(50))
                    .border(3.dp, Color(0xFF4D9BFF), RoundedCornerShape(50))
                    .padding(horizontal = 22.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (mode == GameMode.COLORS) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(world.targetColor, CircleShape)
                            .border(2.dp, Palette.Ink.copy(alpha = 0.25f), CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(targetShown, color = Palette.Ink, fontSize = 21.sp, fontWeight = FontWeight.Black)
                } else if (mode == GameMode.SHAPES || mode == GameMode.ANIMALS) {
                    Text(
                        if (mode == GameMode.ANIMALS) LearningContent.animalFor(targetShown)
                        else LearningContent.glyphFor(targetShown),
                        color = Palette.Ink, fontSize = 27.sp, fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(targetShown, color = Palette.Ink, fontSize = 21.sp, fontWeight = FontWeight.Black)
                } else {
                    Text(targetShown, color = Palette.Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // First-run hand: points at the balloon to tap and mimes the tap, for
        // the first few taps of a child's first visit to this mode.
        if (hintsLeft > 0) {
            Canvas(Modifier.fillMaxSize()) {
                if (frame.longValue >= 0L) {
                    world.hintBubble()?.let { b ->
                        drawTapHand(Offset(b.x, b.y), world.time * 0.75f, density, measurer)
                    }
                }
            }
        }

        // Colours mode: a wave of the found colour floods out from the popped
        // balloon and fills the whole screen, then melts away.
        washColor?.let { wc ->
            Canvas(Modifier.fillMaxSize()) {
                val p = washProg.value
                if (p > 0f && p < 1f) {
                    val maxDim = kotlin.math.max(size.width, size.height)
                    val radius = maxDim * 1.35f * (p * 1.6f).coerceAtMost(1f)
                    val alpha = if (p < 0.55f) 0.92f else 0.92f * (1f - (p - 0.55f) / 0.45f)
                    drawCircle(wc.copy(alpha = alpha.coerceIn(0f, 1f)), radius, washFrom)
                }
            }
        }

        // The found value flying up into its slot in the strip, along a little
        // arc — leaving a trail of fading golden sparkles behind it.
        flyer?.let { f ->
            Canvas(Modifier.fillMaxSize()) {
                val target = stripSlots[f.toIndex] ?: f.from
                val arcLift = 70.dp.toPx()
                for (k in 1..6) {
                    val pk = flyProg.value - k * 0.055f
                    if (pk <= 0f) continue
                    val tx = f.from.x + (target.x - f.from.x) * pk
                    val ty = f.from.y + (target.y - f.from.y) * pk -
                        kotlin.math.sin(pk * 3.1416f) * arcLift
                    drawCircle(
                        Palette.Gold.copy(alpha = (1f - k / 7f) * 0.6f),
                        (5f - k * 0.5f) * density,
                        Offset(tx, ty),
                    )
                }
            }
            Box(
                Modifier.graphicsLayer {
                    val target = stripSlots[f.toIndex] ?: f.from
                    val p = flyProg.value
                    val x = f.from.x + (target.x - f.from.x) * p
                    val y = f.from.y + (target.y - f.from.y) * p -
                        kotlin.math.sin(p * 3.1416f) * 70.dp.toPx()
                    translationX = x - 27.dp.toPx()
                    translationY = y - 27.dp.toPx()
                    val sc = 1.6f - 1.1f * p
                    scaleX = sc
                    scaleY = sc
                }
            ) {
                if (f.label != null) {
                    Text(
                        text = f.label,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            shadow = Shadow(Palette.Ink.copy(alpha = 0.6f), Offset(0f, 4f), 7f)
                        ),
                        modifier = Modifier.size(54.dp),
                    )
                } else {
                    Canvas(Modifier.size(54.dp)) {
                        drawCircle(f.color, size.minDimension * 0.32f, Offset(size.width / 2f, size.height / 2f))
                        drawCircle(
                            Color.White,
                            size.minDimension * 0.32f,
                            Offset(size.width / 2f, size.height / 2f),
                            style = Stroke(width = 2.5f * density),
                        )
                    }
                }
            }
        }

        // The reward: a huge picture and word that spring straight onto the
        // scene after a right answer — no card behind them, like the balloon
        // burst itself turned into the word.
        AnimatedVisibility(
            visible = rewardEmoji != null || rewardWord != null,
            enter = fadeIn() + scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                initialScale = 0.25f,
            ),
            exit = fadeOut() + scaleOut(targetScale = 1.2f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Rays of light spinning behind the word, so the reward feels
                // like something arriving rather than text appearing.
                Canvas(Modifier.size(300.dp)) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val spin = (rewardTick % 8) * 4f
                    for (k in 0 until 16) {
                        val a = Math.toRadians((k * 22.5f + spin).toDouble()).toFloat()
                        val inner = size.minDimension * 0.16f
                        val outer = size.minDimension * (if (k % 2 == 0) 0.46f else 0.36f)
                        drawLine(
                            Color.White.copy(alpha = 0.30f),
                            Offset(c.x + cos(a) * inner, c.y + sin(a) * inner),
                            Offset(c.x + cos(a) * outer, c.y + sin(a) * outer),
                            strokeWidth = 12f,
                            cap = StrokeCap.Round,
                        )
                    }
                    drawCircle(Color.White.copy(alpha = 0.18f), size.minDimension * 0.22f, c)
                }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                rewardEmoji?.let {
                    Text(
                        text = it,
                        fontSize = if (it.length > 6) 34.sp else 100.sp,
                        textAlign = TextAlign.Center,
                        // A shape glyph is plain text with no colour of its own, so
                        // it wears the balloon's colour; a real emoji keeps its own.
                        color = rewardColor ?: Color.Unspecified,
                        style = if (rewardColor != null) {
                            TextStyle(shadow = Shadow(Color.White, Offset(0f, 0f), 26f))
                        } else TextStyle.Default,
                    )
                }
                rewardWord?.let {
                    Text(
                        text = it,
                        // In numbers mode the number itself is the whole show.
                        fontSize = if (mode == GameMode.NUMBERS) 110.sp else 40.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            shadow = Shadow(Palette.Ink.copy(alpha = 0.55f), Offset(0f, 5f), 10f)
                        ),
                    )
                }
            }
            }
        }

        // Finished the whole set! The party has already played out — now the
        // well-done and two big choices, straight over the live scene.
        if (showComplete) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // One tidy card holds the whole well-done moment.
                Column(
                    Modifier
                        .background(Color(0xFF5BC0F0), RoundedCornerShape(28.dp))
                        .border(3.dp, Color.White, RoundedCornerShape(28.dp))
                        .padding(horizontal = 34.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🎉", fontSize = 64.sp)
                    Text(
                        text = "Great job!",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = TextStyle(
                            shadow = Shadow(Palette.Ink.copy(alpha = 0.5f), Offset(0f, 4f), 8f)
                        ),
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        CompleteButton("Play Again", Color(0xFF35C978)) { showComplete = false; runKey++ }
                        CompleteButton("Menu", Color(0xFF4D9BFF)) { currentExit() }
                    }
                }
            }
        }

        // A simple tap now leaves the game (per request). It is small and in the
        // corner so it is still out of a toddler's usual reach.
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.45f), CircleShape)
                .clickable(onClick = currentExit),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(20.dp)) { drawHouseGlyph() }
        }
    }
}

/** A big, friendly button for the well-done screen. */
@Composable
private fun CompleteButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .background(color, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 30.dp, vertical = 15.dp)
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(
                shadow = Shadow(Palette.Ink.copy(alpha = 0.35f), Offset(0f, 3f), 5f)
            ),
        )
    }
}

/**
 * The alphabet or number line across the top. Popped answers stay gold, the one
 * being asked for is boxed and bright, and the rest are faded — so a child can
 * see how far they've come and what comes next.
 */
@Composable
private fun ProgressStrip(
    mode: GameMode,
    sequence: List<String>,
    currentIndex: Int,
    pulseIndex: Int,
    pulseScale: Float,
    onSlotPositioned: (Int, Offset) -> Unit,
    modifier: Modifier,
) {
    // A soft white ribbon, so the row stays readable whatever colour the sky is.
    Row(
        modifier
            .background(Color.White.copy(alpha = 0.86f), RoundedCornerShape(50))
            .border(2.dp, Color.White, RoundedCornerShape(50))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        sequence.forEachIndexed { i, label ->
            val done = i < currentIndex
            val current = i == currentIndex
            // Each slot reports where it is, so a popped value can fly to it;
            // the slot that just received one gives a springy bounce.
            var slotMod = Modifier.onGloballyPositioned { onSlotPositioned(i, it.boundsInRoot().center) }
            if (i == pulseIndex) {
                slotMod = slotMod.graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
            }
            if (mode == GameMode.COLORS) {
                val col = LearningContent.colors[i].color
                val alpha = if (current || done) 1f else 0.3f
                Box(
                    slotMod
                        .size(if (current) 24.dp else 16.dp)
                        .background(col.copy(alpha = alpha), CircleShape)
                )
            } else {
                Text(
                    text = label,
                    color = when {
                        current -> Color.White
                        // Found ones stay a warm gold; the rest wait quietly.
                        done -> Color(0xFFE08900)
                        else -> Palette.Ink.copy(alpha = 0.32f)
                    },
                    fontSize = if (current) 20.sp else 14.sp,
                    fontWeight = if (current) FontWeight.Black else FontWeight.Bold,
                    modifier = if (current) {
                        slotMod
                            .background(Color(0xFF4D9BFF), RoundedCornerShape(10.dp))
                            .padding(horizontal = 7.dp, vertical = 1.dp)
                    } else slotMod,
                )
            }
        }
    }
}

/** How many taps the first-run pointing hand accompanies, per mode. */
private const val TUTORIAL_HINTS = 3

/** One value in flight from a popped balloon to its slot in the strip. */
private class Flyer(
    val label: String?,
    val color: Color,
    val from: Offset,
    val toIndex: Int,
)

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
