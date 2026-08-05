package com.meetpatel.popgrow.game

import androidx.compose.ui.graphics.Color

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
) {
    var x: Float = baseX
        private set

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
) {
    var life: Float = maxLife

    fun advance(dt: Float, gravity: Float) {
        vy += gravity * dt
        vx *= 0.99f
        x += vx * dt
        y += vy * dt
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

/** Occasional visitor that flutters across a lane when its garden gets full. */
class Butterfly(
    var x: Float,
    val baseY: Float,
    val vx: Float,
    val bobAmp: Float,
    val bobFreq: Float,
    val phase: Float,
    val color: Color,
    val size: Float,
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
