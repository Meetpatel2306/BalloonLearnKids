package com.meetpatel.popgrow.game

import androidx.compose.ui.graphics.Color
import com.meetpatel.popgrow.ui.Palette
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Things the world asks the audio layer to play during a tick (not a tap). */
enum class GameSound { BALLOON_AWAY }

/**
 * All game state and simulation. Deliberately free of Android and Compose types
 * so the rules can be reasoned about (and unit-tested) on their own.
 *
 * Design rules baked in here, all of them for the 2-4 age band:
 *  - bubbles are never smaller than ~1.5 cm across, plus a generous tap slop
 *  - a bubble that escapes off the top is not a loss; it is simply replaced
 *  - there is no timer, no score, no end state and nothing to get wrong
 */
class GameWorld(
    val twoPlayer: Boolean,
    private val density: Float,
    val mode: GameMode = GameMode.FREE_PLAY,
    /** Grown-up settings: how fast balloons rise, and how large they are. Size
     * only ever scales up, so the minimum touch target stays safe. */
    private val speedScale: Float = 1f,
    private val sizeScale: Float = 1f,
    private val random: Random = Random.Default,
) {

    var width: Float = 0f
        private set
    var height: Float = 0f
        private set
    var time: Float = 0f
        private set

    val isLearning: Boolean get() = mode != GameMode.FREE_PLAY

    /** The value the child is currently asked to find (a number, letter or colour
     * name). Empty in free play. */
    var target: String = ""
        private set
    /** The colour of the current target, for the on-screen prompt (colours mode). */
    var targetColor: Color = Color.White
        private set

    /** The current level, starting at 0. Drives the background theme. */
    var level: Int = 0
        private set
    private var levelPops = 0

    /** How far the child is towards the next level, 0..1 — for the star meter. */
    val levelProgress: Float get() = (levelPops.toFloat() / POPS_PER_LEVEL).coerceIn(0f, 1f)
    val popsPerLevel: Int get() = POPS_PER_LEVEL

    /** Non-tap sounds queued during a tick (e.g. a balloon floating away), for
     * the UI to play and then clear each frame. */
    val pendingSounds = mutableListOf<GameSound>()

    val bubbles = mutableListOf<Bubble>()
    val particles = mutableListOf<Particle>()
    val flowers = mutableListOf<Flower>()
    val butterflies = mutableListOf<Butterfly>()
    val clouds = mutableListOf<Cloud>()
    val ripples = mutableListOf<Ripple>()
    val floaters = mutableListOf<Floater>()
    val skyRiders = mutableListOf<SkyRider>()
    val groundCritters = mutableListOf<GroundCritter>()

    private var nextId = 0L
    private var visitorCount = 0
    // Purely-cosmetic scenery (floaters, balloon) draws from its own generator so
    // it never perturbs the deterministic, unit-tested gameplay sequence.
    private val decoRandom = Random.Default
    private val laneCount = if (twoPlayer) 2 else 1
    private val spawnCooldown = FloatArray(laneCount)
    private val flowersSinceButterfly = IntArray(laneCount)
    private var critterCooldown = 3f + decoRandom.nextFloat() * 6f

    /** Position in the A–Z / 1–20 / colour sequence, for the progress strip. */
    var targetIndex = -1
        private set

    /** The whole ordered list for the current mode, so the top strip can show
     * what's done and what's next. */
    val sequence: List<String>
        get() = when (mode) {
            GameMode.COLORS -> LearningContent.colors.map { it.name }
            GameMode.NUMBERS -> LearningContent.numbers
            GameMode.LETTERS -> LearningContent.letters
            GameMode.SHAPES -> LearningContent.shapes.map { it.name }
            GameMode.ANIMALS -> LearningContent.animals.map { it.name }
            GameMode.FREE_PLAY -> emptyList()
        }

    /** Wrong taps since the last right answer — lets the UI make the target
     * balloon wave for attention when a child seems stuck. Never a penalty. */
    var missStreak: Int = 0
        private set

    init {
        if (isLearning) chooseNewTarget()
    }

    /** Move to the next thing to ask for, in order (A, B, C… / 1, 2, 3…). Going in
     * sequence is how a child actually learns the alphabet and counting, and it
     * wraps around so a session never ends. */
    private fun chooseNewTarget() {
        when (mode) {
            GameMode.COLORS -> {
                targetIndex = (targetIndex + 1) % LearningContent.colors.size
                val pick = LearningContent.colors[targetIndex]
                target = pick.name
                targetColor = pick.color
            }
            GameMode.NUMBERS -> {
                targetIndex = (targetIndex + 1) % LearningContent.numbers.size
                target = LearningContent.numbers[targetIndex]
            }
            GameMode.LETTERS -> {
                targetIndex = (targetIndex + 1) % LearningContent.letters.size
                target = LearningContent.letters[targetIndex]
            }
            GameMode.SHAPES -> {
                targetIndex = (targetIndex + 1) % LearningContent.shapes.size
                target = LearningContent.shapes[targetIndex].name
            }
            GameMode.ANIMALS -> {
                targetIndex = (targetIndex + 1) % LearningContent.animals.size
                target = LearningContent.animals[targetIndex].name
            }
            GameMode.FREE_PLAY -> {}
        }
    }

    /** Baseline the garden grows from. */
    val groundY: Float get() = height * GROUND_FRACTION

    private fun dp(v: Float) = v * density

    fun resize(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        val first = width == 0f
        width = w
        height = h
        if (first) {
            repeat(CLOUD_COUNT) {
                clouds += Cloud(
                    x = random.nextFloat() * w,
                    y = h * (0.06f + random.nextFloat() * 0.30f),
                    scale = 0.65f + random.nextFloat() * 0.75f,
                    speed = dp(4f) + random.nextFloat() * dp(8f),
                )
            }
            repeat(FLOATER_COUNT) {
                val kind = FloaterKind.entries[decoRandom.nextInt(FloaterKind.entries.size)]
                val color = if (kind == FloaterKind.SPARKLE)
                    Palette.Sparkle[decoRandom.nextInt(Palette.Sparkle.size)]
                else Palette.Petal[decoRandom.nextInt(Palette.Petal.size)]
                floaters += Floater(
                    x = decoRandom.nextFloat() * w,
                    y = decoRandom.nextFloat() * h,
                    vx = (decoRandom.nextFloat() - 0.5f) * dp(16f),
                    vy = (decoRandom.nextFloat() - 0.5f) * dp(14f),
                    rotation = decoRandom.nextFloat() * 6.28f,
                    spin = (decoRandom.nextFloat() - 0.5f) * 1.6f,
                    size = dp(5f) + decoRandom.nextFloat() * dp(6f),
                    color = color,
                    kind = kind,
                )
            }
            skyRiders += SkyRider(
                x = decoRandom.nextFloat() * w,
                y = h * (0.08f + decoRandom.nextFloat() * 0.12f),
                speed = dp(7f) + decoRandom.nextFloat() * dp(6f),
                scale = 0.85f + decoRandom.nextFloat() * 0.4f,
                color = Palette.Confetti[decoRandom.nextInt(Palette.Confetti.size)],
            )
        }
    }

    fun laneStart(lane: Int): Float = if (twoPlayer) lane * width / 2f else 0f
    fun laneEnd(lane: Int): Float = if (twoPlayer) (lane + 1) * width / 2f else width
    fun laneOf(x: Float): Int = if (twoPlayer && x >= width / 2f) 1 else 0

    // ------------------------------------------------------------------ update

    /** Background-only tick, used by the menu screen where nothing is playable. */
    fun updateScenery(dtRaw: Float) {
        if (width <= 0f) return
        val dt = dtRaw.coerceIn(0f, MAX_STEP)
        time += dt
        clouds.forEach {
            it.x += it.speed * dt
            if (it.x - dp(90f) * it.scale > width) it.x = -dp(90f) * it.scale
        }
        floaters.forEach { it.advance(dt, width, height, dp(40f)) }
        skyRiders.forEach { it.advance(dt, width, dp(130f) * it.scale) }
        tickCritters(dt)
    }

    /** Walk the animals along, retire any that stroll off-screen, and now and
     * then send in a fresh one. Cosmetic only, so it uses [decoRandom]. */
    private fun tickCritters(dt: Float) {
        groundCritters.forEach { it.advance(dt) }
        val margin = dp(90f)
        groundCritters.removeAll { it.x < -margin || it.x > width + margin }

        critterCooldown -= dt
        if (critterCooldown <= 0f) {
            critterCooldown = CRITTER_MIN + decoRandom.nextFloat() * CRITTER_JITTER
            if (groundCritters.size < 2) spawnCritter()
        }
    }

    private fun spawnCritter() {
        val kind = GroundCritterKind.entries[decoRandom.nextInt(GroundCritterKind.entries.size)]
        val leftToRight = decoRandom.nextBoolean()
        val color = when (kind) {
            GroundCritterKind.PUPPY -> Palette.Puppy
            GroundCritterKind.CAT -> Palette.CatColors[decoRandom.nextInt(Palette.CatColors.size)]
            GroundCritterKind.BUNNY -> Palette.Bunny
            GroundCritterKind.DUCK -> Palette.Bee
        }
        groundCritters += GroundCritter(
            x = if (leftToRight) -dp(60f) else width + dp(60f),
            baseY = groundY + dp(10f) + decoRandom.nextFloat() * (height * 0.06f),
            vx = (if (leftToRight) 1f else -1f) * (dp(28f) + decoRandom.nextFloat() * dp(26f)),
            size = dp(16f) + decoRandom.nextFloat() * dp(8f),
            color = color,
            kind = kind,
        )
    }

    fun update(dtRaw: Float) {
        if (width <= 0f) return
        // Clamp dt so a dropped frame or a resume from background never teleports
        // every bubble across the screen at once.
        val dt = dtRaw.coerceIn(0f, MAX_STEP)
        time += dt

        bubbles.forEach { it.advance(dt, time) }
        // A bubble that floats off the top is never a loss. A balloon, though,
        // gives a cheerful little puff and pop as it sails away.
        val escaped = bubbles.filter { it.y + it.radius < 0f }
        for (b in escaped) {
            if (b.kind == BubbleKind.BALLOON) {
                puff(b.x, 0f, b.color)
                pendingSounds += GameSound.BALLOON_AWAY
            }
        }
        bubbles.removeAll { it.y + it.radius < 0f }

        particles.forEach { it.advance(dt, dp(GRAVITY_DP)) }
        particles.removeAll { !it.alive }

        butterflies.forEach { it.advance(dt, time) }
        butterflies.removeAll { it.x < -dp(80f) || it.x > width + dp(80f) }

        clouds.forEach {
            it.x += it.speed * dt
            if (it.x - dp(90f) * it.scale > width) it.x = -dp(90f) * it.scale
        }
        floaters.forEach { it.advance(dt, width, height, dp(40f)) }
        skyRiders.forEach { it.advance(dt, width, dp(130f) * it.scale) }
        tickCritters(dt)

        ripples.forEach { it.advance(dt) }
        ripples.removeAll { !it.alive }

        flowers.removeAll { it.expired(time) }

        for (lane in 0 until laneCount) {
            spawnCooldown[lane] -= dt
            val inLane = bubbles.count { it.lane == lane }
            if (inLane < targetBubbles && spawnCooldown[lane] <= 0f) {
                spawn(lane)
                spawnCooldown[lane] = SPAWN_MIN + random.nextFloat() * SPAWN_JITTER
            }
        }
    }

    private val targetBubbles: Int get() = if (twoPlayer) 6 else 9

    private fun spawn(lane: Int) {
        val grow = sizeScale.coerceAtLeast(1f)
        val minR = dp(MIN_RADIUS_DP) * grow
        val maxR = dp(if (twoPlayer) MAX_RADIUS_DUO_DP else MAX_RADIUS_SOLO_DP) * grow
        val radius = minR + random.nextFloat() * (maxR - minR)

        val start = laneStart(lane) + radius + dp(4f)
        val end = laneEnd(lane) - radius - dp(4f)
        val baseX = if (end <= start) (start + end) / 2f else start + random.nextFloat() * (end - start)

        val swayAmp = minOf(dp(10f) + random.nextFloat() * dp(18f), (end - start).coerceAtLeast(0f) / 2f)

        // Big bubble -> low note, small bubble -> high note. Size and pitch move
        // together, which is a real physical intuition a small child can absorb
        // without anyone explaining it.
        val sizeT = ((radius - minR) / (maxR - minR).coerceAtLeast(0.001f)).coerceIn(0f, 1f)
        val noteIndex = ((1f - sizeT) * (NOTE_COUNT - 1)).toInt().coerceIn(0, NOTE_COUNT - 1)

        val colors = Palette.forLane(lane, twoPlayer)

        // A rare rainbow bubble is the celebration; otherwise a mix of everyday
        // shapes so the sky always has something new to look at.
        val cosmeticKind = if (random.nextInt(SPECIAL_ONE_IN) == 0) {
            BubbleKind.RAINBOW
        } else when (random.nextInt(10)) {
            in 0..4 -> BubbleKind.BUBBLE
            in 5..7 -> BubbleKind.BALLOON
            8 -> BubbleKind.STAR
            else -> BubbleKind.HEART
        }

        // In a learning mode the bubbles are balloons. For numbers/letters most
        // are plain friendly balloons and exactly one carries the current target
        // (the child hunts for it); for colours every balloon wears a colour.
        var label = ""
        var matchKey = ""
        var bubbleColor = colors[random.nextInt(colors.size)]
        val kind = if (isLearning) BubbleKind.BALLOON else cosmeticKind
        if (isLearning) {
            when (mode) {
                GameMode.COLORS -> {
                    val value = colorSpawnValue()
                    matchKey = value
                    bubbleColor = LearningContent.colors.first { it.name == value }.color
                }
                GameMode.NUMBERS, GameMode.LETTERS, GameMode.SHAPES, GameMode.ANIMALS -> {
                    // Keep exactly one target balloon around; it stays the same
                    // value until the child finds it. Everything else is a plain,
                    // friendly balloon that's just fun to pop.
                    if (bubbles.none { it.matchKey == target }) {
                        label = when (mode) {
                            GameMode.SHAPES -> LearningContent.glyphFor(target)
                            GameMode.ANIMALS -> LearningContent.animalFor(target)
                            else -> target
                        }
                        matchKey = target
                    }
                }
                GameMode.FREE_PLAY -> {}
            }
        }

        // Free-play balloons are lighter than air and rise faster; learning
        // balloons keep a calm, easy-to-tap pace.
        val baseRise = (dp(RISE_MIN_DP) + random.nextFloat() * dp(RISE_RANGE_DP)) * speedScale
        val riseSpeed = if (kind == BubbleKind.BALLOON && !isLearning) baseRise * 1.5f else baseRise

        bubbles += Bubble(
            id = nextId++,
            lane = lane,
            baseX = baseX,
            y = height + radius + random.nextFloat() * dp(60f),
            radius = radius,
            riseSpeed = riseSpeed,
            swayAmp = swayAmp,
            swayFreq = 0.6f + random.nextFloat() * 0.7f,
            swayPhase = random.nextFloat() * 6.28f,
            color = bubbleColor,
            noteIndex = noteIndex,
            kind = kind,
            face = random.nextInt(FACE_COUNT),
            label = label,
            matchKey = matchKey,
        )
    }

    /** Colours mode: make sure at least one balloon of the asked-for colour is
     * always present, mixing in the other rainbow colours around it. */
    private fun colorSpawnValue(): String {
        val hasMatch = bubbles.any { it.matchKey == target }
        if (!hasMatch || random.nextFloat() < 0.4f) return target
        return LearningContent.colors[random.nextInt(LearningContent.colors.size)].name
    }

    // -------------------------------------------------------------------- input

    /** What a single tap produced, for the UI layer to turn into sound and haptics. */
    data class Pop(
        val noteIndex: Int,
        val loudness: Float,
        val butterfly: Boolean,
        val special: Boolean = false,
        val kind: BubbleKind = BubbleKind.BUBBLE,
        val leveledUp: Boolean = false,
        /** In a learning mode, the word to speak for the popped bubble; else null. */
        val spoken: String? = null,
        /** True when the popped bubble was the one being asked for. */
        val correct: Boolean = false,
        /** On a correct answer: the phrase to say ("A for Apple"), and the word +
         * picture to show as a reward. */
        val spokenReward: String? = null,
        val rewardWord: String? = null,
        val rewardEmoji: String? = null,
        /** True on the correct pop that finishes the whole A–Z / 1–20 / colour set. */
        val completedSet: Boolean = false,
        /** Where the popped bubble was, so the UI can fly its value to the strip. */
        val x: Float = 0f,
        val y: Float = 0f,
        /** Which sequence slot this correct pop just completed; -1 otherwise. */
        val completedIndex: Int = -1,
    )

    /**
     * Handles one tap. Returns null if the child hit empty sky — which does
     * nothing at all, on purpose. There is no penalty and no negative feedback
     * anywhere in this game.
     */
    fun popAt(px: Float, py: Float): Pop? {
        val slop = dp(TAP_SLOP_DP)
        var best: Bubble? = null
        var bestDist = Float.MAX_VALUE
        for (b in bubbles) {
            val d = hypot(px - b.x, py - b.y)
            if (d <= b.radius + slop && d < bestDist) {
                bestDist = d
                best = b
            }
        }
        val bubble = best ?: return null
        bubbles.remove(bubble)
        burst(bubble)
        ripples += Ripple(bubble.x, bubble.y, bubble.radius, 0.45f, Color.White)
        // Every so often a little butterfly was hiding inside and flutters free.
        if (decoRandom.nextInt(SURPRISE_ONE_IN) == 0) releaseSurprise(bubble)
        val butterfly = plantFlower(bubble)

        // Learning modes: work out what was said and whether it was the answer.
        var spoken: String? = null
        var correct = false
        var spokenReward: String? = null
        var rewardWord: String? = null
        var rewardEmoji: String? = null
        var completedSet = false
        var doneIndex = -1
        if (isLearning) {
            // Shapes and colours speak their name, letters/numbers their label.
            val value = when (mode) {
                GameMode.COLORS, GameMode.SHAPES, GameMode.ANIMALS -> bubble.matchKey
                else -> bubble.label
            }
            spoken = value.ifEmpty { null }   // plain balloons say nothing
            if (bubble.matchKey.isNotEmpty() && bubble.matchKey == target) {
                correct = true
                doneIndex = targetIndex
                // Was this the last one in the A–Z / 1–20 / colour set?
                completedSet = targetIndex >= sequence.lastIndex
                // Build the reward from the matched value before moving on.
                when (mode) {
                    GameMode.LETTERS -> {
                        val w = LearningContent.letterWords[bubble.matchKey]
                        rewardWord = w?.word
                        rewardEmoji = w?.emoji
                        spokenReward = if (w != null) "${bubble.matchKey} for ${w.word}" else bubble.matchKey
                    }
                    GameMode.NUMBERS -> {
                        // The number itself is the reward, shown huge.
                        rewardWord = bubble.matchKey
                        spokenReward = bubble.matchKey
                    }
                    GameMode.COLORS -> {
                        // No word card — the whole screen washes with the colour
                        // instead, handled by the UI layer.
                        spokenReward = bubble.matchKey
                    }
                    GameMode.SHAPES -> {
                        rewardWord = bubble.matchKey
                        rewardEmoji = LearningContent.glyphFor(bubble.matchKey)
                        spokenReward = bubble.matchKey
                    }
                    GameMode.ANIMALS -> {
                        rewardWord = bubble.matchKey
                        rewardEmoji = LearningContent.animalFor(bubble.matchKey)
                        spokenReward = bubble.matchKey
                    }
                    GameMode.FREE_PLAY -> {}
                }
                celebrateLevelUp()
                chooseNewTarget()
            }
            missStreak = if (correct) 0 else missStreak + 1
        }

        // Free play fills the meter on every pop; learning modes only on a right
        // answer, so the stars reward learning, not just tapping.
        val countsForLevel = !isLearning || correct
        var leveledUp = false
        if (countsForLevel) {
            levelPops++
            // A tiny sparkle ping at the star that just gained some shine.
            if (!isLearning) starPing()
            if (levelPops >= POPS_PER_LEVEL) {
                levelPops = 0
                level++
                leveledUp = true
                celebrateLevelUp()
                // Every fifth level earns a proper fireworks show.
                if (level % 5 == 0) celebrate()
            }
        }

        val minR = dp(MIN_RADIUS_DP)
        val maxR = dp(MAX_RADIUS_SOLO_DP)
        val sizeT = ((bubble.radius - minR) / (maxR - minR)).coerceIn(0f, 1f)
        return Pop(
            noteIndex = bubble.noteIndex,
            loudness = 0.6f + 0.4f * sizeT,
            butterfly = butterfly,
            special = bubble.special,
            kind = bubble.kind,
            leveledUp = leveledUp,
            spoken = spoken,
            correct = correct,
            spokenReward = spokenReward,
            rewardWord = rewardWord,
            rewardEmoji = rewardEmoji,
            completedSet = completedSet,
            x = bubble.x,
            y = bubble.y,
            completedIndex = doneIndex,
        )
    }

    /**
     * The balloon a first-time player should be shown: the answer in a learning
     * mode, or simply the biggest one in free play. Null when there is nothing
     * on screen yet to point at.
     */
    fun hintBubble(): Bubble? =
        if (isLearning) bubbles.firstOrNull { it.matchKey.isNotEmpty() && it.matchKey == target }
        else bubbles.maxByOrNull { it.radius }

    /** A soft sparkle at the star meter as it fills, so progress feels physical. */
    private fun starPing() {
        val stars = 5
        val gap = dp(40f)
        val cx0 = width / 2f - gap * (stars - 1) / 2f
        val idx = (levelProgress * stars).toInt().coerceIn(0, stars - 1)
        val px = cx0 + idx * gap
        val py = dp(34f)
        repeat(4) {
            val a = decoRandom.nextFloat() * 6.283f
            val sp = dp(30f) + decoRandom.nextFloat() * dp(50f)
            particles += Particle(
                x = px, y = py,
                vx = cos(a) * sp, vy = sin(a) * sp,
                radius = dp(1.8f) + decoRandom.nextFloat() * dp(2f),
                color = Palette.Sparkle[decoRandom.nextInt(Palette.Sparkle.size)],
                maxLife = 0.35f + decoRandom.nextFloat() * 0.2f,
                shape = ParticleShape.SPARKLE,
                rotation = decoRandom.nextFloat() * 6.28f,
                spin = (decoRandom.nextFloat() - 0.5f) * 8f,
                gravityScale = 0.15f,
                twinkle = true,
            )
        }
    }

    /** A bonus burst of golden sparkles, used for fast-pop streaks. */
    fun sparkleBurst(x: Float, y: Float) {
        repeat(10) {
            val a = decoRandom.nextFloat() * 6.283f
            val sp = dp(60f) + decoRandom.nextFloat() * dp(140f)
            particles += Particle(
                x = x, y = y,
                vx = cos(a) * sp, vy = sin(a) * sp,
                radius = dp(2.5f) + decoRandom.nextFloat() * dp(3f),
                color = Palette.Sparkle[decoRandom.nextInt(Palette.Sparkle.size)],
                maxLife = 0.5f + decoRandom.nextFloat() * 0.3f,
                shape = ParticleShape.SPARKLE,
                rotation = decoRandom.nextFloat() * 6.28f,
                spin = (decoRandom.nextFloat() - 0.5f) * 10f,
                gravityScale = 0.3f,
                twinkle = true,
            )
        }
    }

    /** A public celebration, used by the UI for the end-of-set party: confetti
     * rain plus proper firework bursts in the sky. */
    fun celebrate() {
        celebrateLevelUp()
        repeat(3) {
            val fx = width * (0.15f + decoRandom.nextFloat() * 0.7f)
            val fy = height * (0.12f + decoRandom.nextFloat() * 0.35f)
            val col = Palette.Rainbow[decoRandom.nextInt(Palette.Rainbow.size)]
            val n = 18
            for (k in 0 until n) {
                val ang = k * (6.2832f / n)
                val sp = dp(140f) + decoRandom.nextFloat() * dp(120f)
                particles += Particle(
                    x = fx,
                    y = fy,
                    vx = cos(ang) * sp,
                    vy = sin(ang) * sp,
                    radius = dp(2.5f) + decoRandom.nextFloat() * dp(3.5f),
                    color = if (k % 3 == 0) Color.White else col,
                    maxLife = 0.8f + decoRandom.nextFloat() * 0.4f,
                    shape = if (k % 4 == 0) ParticleShape.SPARKLE else ParticleShape.CIRCLE,
                    gravityScale = 0.35f,
                    twinkle = k % 2 == 0,
                )
            }
        }
    }

    /** A rain of confetti across the whole sky to mark a new level. */
    private fun celebrateLevelUp() {
        val n = 46
        repeat(n) {
            val x = random.nextFloat() * width
            val speed = dp(120f) + random.nextFloat() * dp(220f)
            particles += Particle(
                x = x,
                y = -random.nextFloat() * dp(60f),
                vx = (random.nextFloat() - 0.5f) * dp(80f),
                vy = speed,
                radius = dp(3f) + random.nextFloat() * dp(6f),
                color = Palette.Rainbow[random.nextInt(Palette.Rainbow.size)],
                maxLife = 1.2f + random.nextFloat() * 0.8f,
                shape = randomShape(),
                rotation = random.nextFloat() * 6.283185f,
                spin = (random.nextFloat() - 0.5f) * 16f,
                gravityScale = 0.25f,
                twinkle = random.nextBoolean(),
            )
        }
    }

    /** A small confetti puff, e.g. where a balloon sails off the top. */
    private fun puff(x: Float, y: Float, color: Color) {
        repeat(10) {
            val angle = random.nextFloat() * 6.283185f
            val speed = dp(40f) + random.nextFloat() * dp(120f)
            particles += Particle(
                x = x,
                y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed + dp(40f),
                radius = dp(2.5f) + random.nextFloat() * dp(4f),
                color = if (random.nextBoolean()) color else Palette.Confetti[random.nextInt(Palette.Confetti.size)],
                maxLife = 0.4f + random.nextFloat() * 0.3f,
                shape = randomShape(),
                rotation = random.nextFloat() * 6.283185f,
                spin = (random.nextFloat() - 0.5f) * 14f,
            )
        }
    }

    private fun burst(b: Bubble) {
        val festive = b.special
        val count = if (festive) FESTIVE_PARTICLES else PARTICLES_PER_POP
        repeat(count) {
            val angle = random.nextFloat() * 6.283185f
            val speed = dp(70f) + random.nextFloat() * dp(if (festive) 240f else 190f)
            particles += Particle(
                x = b.x,
                y = b.y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                radius = dp(2.5f) + random.nextFloat() * dp(if (festive) 6f else 5f),
                color = if (festive) Palette.Rainbow[random.nextInt(Palette.Rainbow.size)]
                        else confettiColor(b.color),
                maxLife = 0.45f + random.nextFloat() * 0.35f,
                shape = randomShape(),
                rotation = random.nextFloat() * 6.283185f,
                spin = (random.nextFloat() - 0.5f) * 18f,
            )
        }
        // A rainbow pop also sends up a fountain of slow, twinkling gold sparkles.
        if (festive) {
            repeat(SPARKLE_FOUNTAIN) {
                val angle = -1.5708f + (random.nextFloat() - 0.5f) * 1.2f
                val speed = dp(120f) + random.nextFloat() * dp(160f)
                particles += Particle(
                    x = b.x,
                    y = b.y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    radius = dp(3f) + random.nextFloat() * dp(4f),
                    color = Palette.Sparkle[random.nextInt(Palette.Sparkle.size)],
                    maxLife = 0.7f + random.nextFloat() * 0.5f,
                    shape = ParticleShape.SPARKLE,
                    rotation = random.nextFloat() * 6.283185f,
                    spin = (random.nextFloat() - 0.5f) * 8f,
                    gravityScale = 0.15f,
                    twinkle = true,
                )
            }
        }
    }

    /** Mostly the bubble's own colour, sometimes a festive accent. */
    private fun confettiColor(base: Color): Color =
        if (random.nextInt(3) == 0) base
        else Palette.Confetti[random.nextInt(Palette.Confetti.size)]

    private fun randomShape(): ParticleShape = when (random.nextInt(6)) {
        0 -> ParticleShape.STAR
        1 -> ParticleShape.HEART
        2 -> ParticleShape.SPARKLE
        else -> ParticleShape.CIRCLE
    }

    /** Returns true when this flower also earned a butterfly. */
    private fun plantFlower(b: Bubble): Boolean {
        val lane = b.lane
        val margin = dp(22f)
        val x = b.x.coerceIn(laneStart(lane) + margin, laneEnd(lane) - margin)

        flowers += Flower(
            lane = lane,
            x = x,
            stemHeight = dp(STEM_MIN_DP) + random.nextFloat() * dp(STEM_RANGE_DP),
            petalRadius = dp(9f) + random.nextFloat() * dp(6f),
            petalCount = 5 + random.nextInt(2),
            petalColor = b.color,
            centerColor = Palette.FlowerCenters[random.nextInt(Palette.FlowerCenters.size)],
            swayPhase = random.nextFloat() * 6.28f,
            bornAt = time,
            kind = FlowerKind.entries[decoRandom.nextInt(FlowerKind.entries.size)],
        )

        // Keep the row from turning into an unreadable hedge: once a lane is full,
        // the oldest flower quietly wilts away rather than the garden freezing.
        val laneFlowers = flowers.filter { it.lane == lane && it.fadeStartedAt < 0f }
        if (laneFlowers.size > MAX_FLOWERS_PER_LANE) {
            laneFlowers.minByOrNull { it.bornAt }?.fadeStartedAt = time
        }

        flowersSinceButterfly[lane]++
        if (flowersSinceButterfly[lane] >= BUTTERFLY_EVERY) {
            flowersSinceButterfly[lane] = 0
            releaseButterfly(lane, b.color)
            return true
        }
        return false
    }

    /** A small butterfly that flutters out sideways from a just-popped bubble. */
    private fun releaseSurprise(b: Bubble) {
        val leftToRight = b.x < width / 2f
        butterflies += Butterfly(
            x = b.x,
            baseY = b.y,
            vx = (if (leftToRight) 1f else -1f) * (dp(50f) + decoRandom.nextFloat() * dp(40f)),
            bobAmp = dp(10f) + decoRandom.nextFloat() * dp(10f),
            bobFreq = 1.6f + decoRandom.nextFloat() * 0.8f,
            phase = decoRandom.nextFloat() * 6.28f,
            color = b.color,
            size = dp(9f) + decoRandom.nextFloat() * dp(4f),
            kind = VisitorKind.BUTTERFLY,
        )
    }

    private fun releaseButterfly(lane: Int, color: Color) {
        // Rotate through the four creatures in order, so a child reliably meets a
        // new friend each time rather than seeing the same one twice by chance.
        val kind = VisitorKind.entries[visitorCount % VisitorKind.entries.size]
        visitorCount++

        val leftToRight = random.nextBoolean()
        // Ladybugs and bees keep their own recognisable colours; a butterfly or
        // bird takes on the colour of the bubble that summoned it.
        val bodyColor = when (kind) {
            VisitorKind.BEE -> Palette.Bee
            VisitorKind.LADYBUG -> Palette.Ladybug
            VisitorKind.BIRD -> Palette.Bird
            VisitorKind.BUTTERFLY -> color
        }
        butterflies += Butterfly(
            x = if (leftToRight) laneStart(lane) - dp(40f) else laneEnd(lane) + dp(40f),
            baseY = groundY - dp(60f) - random.nextFloat() * height * 0.25f,
            vx = (if (leftToRight) 1f else -1f) * (dp(55f) + random.nextFloat() * dp(45f)),
            bobAmp = dp(14f) + random.nextFloat() * dp(16f),
            bobFreq = 1.4f + random.nextFloat() * 0.8f,
            phase = random.nextFloat() * 6.28f,
            color = bodyColor,
            size = dp(13f) + random.nextFloat() * dp(6f),
            kind = kind,
        )
    }

    companion object {
        const val GROUND_FRACTION = 0.88f

        // A 2 cm target is ~63 dp on any Android screen (160 dp = 1 inch). These
        // radii keep every bubble at or above that, per touch-accuracy research
        // for 3-6 year olds.
        private const val MIN_RADIUS_DP = 44f
        private const val MAX_RADIUS_SOLO_DP = 74f
        private const val MAX_RADIUS_DUO_DP = 62f
        private const val TAP_SLOP_DP = 20f

        private const val RISE_MIN_DP = 26f
        private const val RISE_RANGE_DP = 22f
        private const val GRAVITY_DP = 520f

        private const val SPAWN_MIN = 0.25f
        private const val SPAWN_JITTER = 0.5f

        private const val STEM_MIN_DP = 34f
        private const val STEM_RANGE_DP = 46f
        private const val MAX_FLOWERS_PER_LANE = 24
        private const val BUTTERFLY_EVERY = 10

        private const val PARTICLES_PER_POP = 14
        private const val FESTIVE_PARTICLES = 26
        private const val SPARKLE_FOUNTAIN = 14
        private const val NOTE_COUNT = 10
        private const val CLOUD_COUNT = 5
        private const val FLOATER_COUNT = 7
        private const val MAX_STEP = 1f / 20f

        // Ground animals: seconds between arrivals, and how rarely a pop hides a
        // butterfly (1 in this many).
        private const val CRITTER_MIN = 6f
        private const val CRITTER_JITTER = 10f
        private const val SURPRISE_ONE_IN = 14

        // Pops needed to fill the star meter and reach the next level.
        private const val POPS_PER_LEVEL = 8

        // 1-in-this-many bubbles is the rainbow celebration bubble.
        private const val SPECIAL_ONE_IN = 12
        // How many happy expressions a bubble can wear.
        private const val FACE_COUNT = 4
    }
}
