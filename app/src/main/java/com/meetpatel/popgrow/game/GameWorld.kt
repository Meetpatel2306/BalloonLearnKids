package com.meetpatel.popgrow.game

import com.meetpatel.popgrow.ui.Palette
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

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
    private val random: Random = Random.Default,
) {

    var width: Float = 0f
        private set
    var height: Float = 0f
        private set
    var time: Float = 0f
        private set

    val bubbles = mutableListOf<Bubble>()
    val particles = mutableListOf<Particle>()
    val flowers = mutableListOf<Flower>()
    val butterflies = mutableListOf<Butterfly>()
    val clouds = mutableListOf<Cloud>()

    private var nextId = 0L
    private val laneCount = if (twoPlayer) 2 else 1
    private val spawnCooldown = FloatArray(laneCount)
    private val flowersSinceButterfly = IntArray(laneCount)

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
    }

    fun update(dtRaw: Float) {
        if (width <= 0f) return
        // Clamp dt so a dropped frame or a resume from background never teleports
        // every bubble across the screen at once.
        val dt = dtRaw.coerceIn(0f, MAX_STEP)
        time += dt

        bubbles.forEach { it.advance(dt, time) }
        bubbles.removeAll { it.y + it.radius < 0f }

        particles.forEach { it.advance(dt, dp(GRAVITY_DP)) }
        particles.removeAll { !it.alive }

        butterflies.forEach { it.advance(dt, time) }
        butterflies.removeAll { it.x < -dp(80f) || it.x > width + dp(80f) }

        clouds.forEach {
            it.x += it.speed * dt
            if (it.x - dp(90f) * it.scale > width) it.x = -dp(90f) * it.scale
        }

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
        val minR = dp(MIN_RADIUS_DP)
        val maxR = dp(if (twoPlayer) MAX_RADIUS_DUO_DP else MAX_RADIUS_SOLO_DP)
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

        bubbles += Bubble(
            id = nextId++,
            lane = lane,
            baseX = baseX,
            y = height + radius + random.nextFloat() * dp(60f),
            radius = radius,
            riseSpeed = dp(RISE_MIN_DP) + random.nextFloat() * dp(RISE_RANGE_DP),
            swayAmp = swayAmp,
            swayFreq = 0.6f + random.nextFloat() * 0.7f,
            swayPhase = random.nextFloat() * 6.28f,
            color = colors[random.nextInt(colors.size)],
            noteIndex = noteIndex,
        )
    }

    // -------------------------------------------------------------------- input

    /** What a single tap produced, for the UI layer to turn into sound and haptics. */
    data class Pop(val noteIndex: Int, val loudness: Float, val butterfly: Boolean)

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
        val butterfly = plantFlower(bubble)

        val minR = dp(MIN_RADIUS_DP)
        val maxR = dp(MAX_RADIUS_SOLO_DP)
        val sizeT = ((bubble.radius - minR) / (maxR - minR)).coerceIn(0f, 1f)
        return Pop(
            noteIndex = bubble.noteIndex,
            loudness = 0.6f + 0.4f * sizeT,
            butterfly = butterfly,
        )
    }

    private fun burst(b: Bubble) {
        repeat(PARTICLES_PER_POP) {
            val angle = random.nextFloat() * 6.283185f
            val speed = dp(70f) + random.nextFloat() * dp(190f)
            particles += Particle(
                x = b.x,
                y = b.y,
                vx = kotlin.math.cos(angle) * speed,
                vy = kotlin.math.sin(angle) * speed,
                radius = dp(2.5f) + random.nextFloat() * dp(5f),
                color = b.color,
                maxLife = 0.45f + random.nextFloat() * 0.35f,
            )
        }
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

    private fun releaseButterfly(lane: Int, color: androidx.compose.ui.graphics.Color) {
        val leftToRight = random.nextBoolean()
        butterflies += Butterfly(
            x = if (leftToRight) laneStart(lane) - dp(40f) else laneEnd(lane) + dp(40f),
            baseY = groundY - dp(60f) - random.nextFloat() * height * 0.25f,
            vx = (if (leftToRight) 1f else -1f) * (dp(55f) + random.nextFloat() * dp(45f)),
            bobAmp = dp(14f) + random.nextFloat() * dp(16f),
            bobFreq = 1.4f + random.nextFloat() * 0.8f,
            phase = random.nextFloat() * 6.28f,
            color = color,
            size = dp(13f) + random.nextFloat() * dp(6f),
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
        private const val NOTE_COUNT = 10
        private const val CLOUD_COUNT = 5
        private const val MAX_STEP = 1f / 20f
    }
}
