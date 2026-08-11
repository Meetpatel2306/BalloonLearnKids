package com.meetpatel.balloonlearnkids.game

import androidx.compose.ui.graphics.Color

/** The look of a bubble. All kinds pop identically — the variety is purely for
 * delight, so the sky never looks like the same nine bubbles twice. */
enum class BubbleKind { BUBBLE, BALLOON, STAR, HEART, RAINBOW }

/** Shape of a single confetti fleck. */
enum class ParticleShape { CIRCLE, STAR, HEART, SPARKLE, RIBBON, RING, SHRED }

/** Which little creature wanders the garden when it fills up. */
enum class VisitorKind { BUTTERFLY, BEE, LADYBUG, BIRD }

/** The shape a planted flower takes, for a garden that isn't all one bloom. */
enum class FlowerKind { ROUND, TULIP, DAISY }

/** A friendly animal that strolls along the grass. */
enum class GroundCritterKind { PUPPY, CAT, BUNNY, DUCK }

/**
 * A bubble drifting up the screen. [baseX] is the column it rises along; the
 * sway is applied on top so the motion is lazy and organic rather than a
 * straight line — moving targets that wobble are far more inviting to tap.
 */
class Bubble(
    val id: Long,
    val lane: Int,
    val baseX: Float,
    var y: Float,
    val radius: Float,
    val riseSpeed: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val swayPhase: Float,
    val color: Color,
    val noteIndex: Int,
    val kind: BubbleKind = BubbleKind.BUBBLE,
    val face: Int = 0,
    /** In a learning mode, the text shown on the bubble ("3", "B"); empty otherwise. */
    val label: String = "",
    /** What this bubble counts as when matching the current target (a number,
     * letter, or colour name). Empty in free play. */
    val matchKey: String = "",
) {
    var x: Float = baseX
        private set

    /** The rare celebration bubble. Popping it earns a bigger party. */
    val special: Boolean get() = kind == BubbleKind.RAINBOW

    fun advance(dt: Float, time: Float) {
        y -= riseSpeed * dt
        x = baseX + swayAmp * kotlin.math.sin(swayFreq * time + swayPhase)
    }
}

/** One pop's worth of confetti. Short-lived, purely decorative. */
class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    val color: Color,
    val maxLife: Float,
    val shape: ParticleShape = ParticleShape.CIRCLE,
    var rotation: Float = 0f,
    val spin: Float = 0f,
    /** Lighter gravity lets celebration sparkles hang and float. */
    val gravityScale: Float = 1f,
    /** Sparkles flicker as they drift. */
    val twinkle: Boolean = false,
) {
    var life: Float = maxLife

    fun advance(dt: Float, gravity: Float) {
        vy += gravity * gravityScale * dt
        vx *= 0.99f
        x += vx * dt
        y += vy * dt
        rotation += spin * dt
        life -= dt
    }

    val alive: Boolean get() = life > 0f
    val fade: Float get() = (life / maxLife).coerceIn(0f, 1f)
}

/**
 * A flower in the garden. This is the whole reward loop: one pop, one flower,
 * permanent (until the row fills up). No score, no counter — the garden *is*
 * the score, and it is legible to a child who cannot read a number.
 */
class Flower(
    val lane: Int,
    val x: Float,
    val stemHeight: Float,
    val petalRadius: Float,
    val petalCount: Int,
    val petalColor: Color,
    val centerColor: Color,
    val swayPhase: Float,
    val bornAt: Float,
    val kind: FlowerKind = FlowerKind.ROUND,
) {
    var fadeStartedAt: Float = -1f

    fun growth(time: Float): Float {
        val t = ((time - bornAt) / GROW_SECONDS).coerceIn(0f, 1f)
        // Ease-out-back: the flower overshoots slightly then settles, which reads
        // as "sproing" rather than "appeared".
        val inv = t - 1f
        return 1f + inv * inv * ((OVERSHOOT + 1f) * inv + OVERSHOOT)
    }

    fun alpha(time: Float): Float {
        if (fadeStartedAt < 0f) return 1f
        return (1f - (time - fadeStartedAt) / FADE_SECONDS).coerceIn(0f, 1f)
    }

    fun expired(time: Float): Boolean =
        fadeStartedAt >= 0f && time - fadeStartedAt > FADE_SECONDS

    companion object {
        const val GROW_SECONDS = 0.5f
        const val FADE_SECONDS = 1.2f
        private const val OVERSHOOT = 1.7f
    }
}

/**
 * Occasional visitor that flutters across a lane when its garden gets full.
 * The class is still named Butterfly (the first and default visitor), but
 * [kind] lets it also arrive as a bee, ladybug or bird for variety.
 */
class Butterfly(
    var x: Float,
    val baseY: Float,
    val vx: Float,
    val bobAmp: Float,
    val bobFreq: Float,
    val phase: Float,
    val color: Color,
    val size: Float,
    val kind: VisitorKind = VisitorKind.BUTTERFLY,
) {
    var y: Float = baseY
    var wing: Float = 0f

    fun advance(dt: Float, time: Float) {
        x += vx * dt
        y = baseY + bobAmp * kotlin.math.sin(bobFreq * time + phase)
        wing = kotlin.math.sin(time * 16f + phase)
    }
}

/** Slow parallax cloud. Wraps around instead of ever disappearing. */
class Cloud(
    var x: Float,
    val y: Float,
    val scale: Float,
    val speed: Float,
)

/** A quick expanding ring of light left behind by every pop — the satisfying
 * "shockwave" that makes a tap feel like it landed. */
class Ripple(
    val x: Float,
    val y: Float,
    val startRadius: Float,
    val maxLife: Float,
    val color: Color,
) {
    var life: Float = maxLife
    fun advance(dt: Float) { life -= dt }
    val alive: Boolean get() = life > 0f
    /** 0 at birth, 1 at death. */
    val t: Float get() = (1f - life / maxLife).coerceIn(0f, 1f)
}

/** Kinds of gentle things that drift across the sky in the background. */
enum class FloaterKind { PETAL, LEAF, SPARKLE }

/** A soft, slow ambient drifter — a petal, a leaf, a wandering sparkle. Purely
 * atmospheric; wraps around the screen so there is always a little life in the air. */
class Floater(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    var rotation: Float,
    val spin: Float,
    val size: Float,
    val color: Color,
    val kind: FloaterKind,
) {
    fun advance(dt: Float, w: Float, h: Float, margin: Float) {
        x += vx * dt
        y += vy * dt
        rotation += spin * dt
        if (x < -margin) x = w + margin else if (x > w + margin) x = -margin
        if (y < -margin) y = h + margin else if (y > h + margin) y = -margin
    }
}

/** A friendly animal ambling along the grass. Purely for delight; it cannot be
 * tapped and has no effect on play — just something lovely to watch go by. */
class GroundCritter(
    var x: Float,
    val baseY: Float,
    val vx: Float,
    val size: Float,
    val color: Color,
    val kind: GroundCritterKind,
) {
    var step: Float = 0f
    fun advance(dt: Float) {
        x += vx * dt
        step += kotlin.math.abs(vx) * dt * 0.05f
    }
}

/** What is crossing the sky up there. */
enum class RiderKind { HOT_AIR, KITE, PLANE }

/** Something friendly drifting slowly across the top of the sky: a hot-air
 *  balloon, a kite on a string, or a little paper plane. */
class SkyRider(
    var x: Float,
    val y: Float,
    val speed: Float,
    val scale: Float,
    val color: Color,
    val kind: RiderKind = RiderKind.HOT_AIR,
) {
    fun advance(dt: Float, w: Float, margin: Float) {
        x += speed * dt
        if (x - margin > w) x = -margin
    }
}
