package com.meetpatel.popgrow.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.meetpatel.popgrow.game.Bubble
import com.meetpatel.popgrow.game.BubbleKind
import com.meetpatel.popgrow.game.Butterfly
import com.meetpatel.popgrow.game.Cloud
import com.meetpatel.popgrow.game.Floater
import com.meetpatel.popgrow.game.FloaterKind
import com.meetpatel.popgrow.game.Flower
import com.meetpatel.popgrow.game.FlowerKind
import com.meetpatel.popgrow.game.GameWorld
import com.meetpatel.popgrow.game.GroundCritter
import com.meetpatel.popgrow.game.GroundCritterKind
import com.meetpatel.popgrow.game.Particle
import com.meetpatel.popgrow.game.ParticleShape
import com.meetpatel.popgrow.game.Ripple
import com.meetpatel.popgrow.game.SkyRider
import com.meetpatel.popgrow.game.VisitorKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * Everything is drawn with vector primitives — there is not a single bitmap in
 * the app. That keeps the APK tiny and means the art is pin-sharp on every
 * screen density from a cheap phone to a 12" tablet.
 */

private const val SKY_STAGE_SECONDS = 30f

/** Sky, sun, clouds, hills and grass. Shared by the menu and the game. The
 * current level chooses the whole colour scheme, so levelling up changes the
 * world; a slow drift within a level keeps it alive between changes. */
fun DrawScope.drawScenery(world: GameWorld, dpUnit: Float) {
    // The background drifts through all the scenes on its own, changing colour
    // continuously over time rather than only when the level changes.
    val theme = Palette.themeAt(world.time)
    drawSky(world.time, theme)
    drawTwinkles(world.time, theme, dpUnit)
    drawShootingStar(world.time, theme, dpUnit)
    drawSunOrMoon(world.time, theme, dpUnit)
    drawRainbow(world.time, dpUnit)
    world.skyRiders.forEach { drawSkyRider(it, dpUnit) }
    world.clouds.forEach { drawCloud(it, theme, dpUnit) }
    drawFarBirds(world.time, theme, dpUnit)
    drawHills(world, theme, dpUnit)
    drawGround(world, theme)
    drawGrassTufts(world, theme, dpUnit)
    drawFireflies(world, theme, dpUnit)
    world.floaters.forEach { drawFloater(it, dpUnit) }
}

fun DrawScope.drawWorld(world: GameWorld, dpUnit: Float, measurer: TextMeasurer? = null) {
    drawScenery(world, dpUnit)
    if (world.twoPlayer) drawDivider(world, dpUnit)
    world.flowers.forEach { drawFlower(it, world, dpUnit) }
    world.groundCritters.forEach { drawGroundCritter(it, dpUnit) }
    // If the child seems stuck (a few wrong taps), the target balloon waves for
    // attention — never a penalty, just a friendlier hint.
    val urgent = world.missStreak >= 3
    world.bubbles.forEach { b ->
        val highlight = world.isLearning && b.matchKey.isNotEmpty() && b.matchKey == world.target
        if (highlight && urgent) {
            rotate(sin(world.time * 8f) * 6f, Offset(b.x, b.y)) {
                drawBubble(b, world.time, dpUnit, measurer, highlight = true, urgent = true)
            }
        } else {
            drawBubble(b, world.time, dpUnit, measurer, highlight)
        }
    }
    world.ripples.forEach { drawRipple(it, dpUnit) }
    world.butterflies.forEach { drawVisitor(it, dpUnit) }
    world.particles.forEach { drawParticle(it) }
    // Free play shows the star meter; learning modes show the A–Z / 1–20 strip
    // (drawn by the UI layer) instead.
    if (!world.isLearning) drawLevelMeter(world, dpUnit)
}

/** A tiny row of stars at the top that fills as the child pops, then empties on
 * level-up. No numbers — just stars lighting up, which a toddler reads at a glance. */
private fun DrawScope.drawLevelMeter(world: GameWorld, dpUnit: Float) {
    val stars = 5
    val gap = 40f * dpUnit
    val cx0 = size.width / 2f - gap * (stars - 1) / 2f
    val cy = 34f * dpUnit
    val r = 15f * dpUnit
    val filled = world.levelProgress * stars

    // A soft rounded backing so the stars read against any sky.
    val padX = 26f * dpUnit
    drawRoundRectCompat(
        cx0 - padX, cy - r - 8f * dpUnit,
        gap * (stars - 1) + padX * 2f, r * 2f + 16f * dpUnit,
        (r + 8f * dpUnit), Color.Black.copy(alpha = 0.14f)
    )

    for (i in 0 until stars) {
        val cx = cx0 + i * gap
        // Empty star: a soft outline.
        drawPath(starPath(cx, cy, r, r * 0.45f, 5, 0f), Color.White.copy(alpha = 0.4f))
        drawPath(starPath(cx, cy, r, r * 0.45f, 5, 0f), Color.White, style = Stroke(width = 2f * dpUnit))
        val f = (filled - i).coerceIn(0f, 1f)
        if (f > 0f) {
            val fr = 0.4f + 0.6f * f
            drawPath(starPath(cx, cy, r * fr, r * 0.45f * fr, 5, 0f), Palette.Gold)
            drawPath(starPath(cx, cy, r * fr, r * 0.45f * fr, 5, 0f), Color(0xFFFF9F1C), style = Stroke(width = 1.5f * dpUnit))
        }
    }
}

// ------------------------------------------------------------------ background

private fun DrawScope.drawSky(time: Float, theme: Palette.SkyTheme) {
    // A gentle within-level drift: the sky deepens a touch and lifts again over
    // roughly a minute, so a single long game still visibly breathes.
    val drift = 0.5f + 0.5f * sin(time / SKY_STAGE_SECONDS * PI.toFloat())
    val top = lerp(theme.skyTop, lerp(theme.skyTop, theme.skyBottom, 0.35f), drift * 0.5f)
    val bottom = lerp(theme.skyBottom, lerp(theme.skyBottom, theme.skyTop, 0.25f), drift * 0.4f)
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
}

/** A scatter of twinkling stars, out over night-time themes. */
private fun DrawScope.drawTwinkles(time: Float, theme: Palette.SkyTheme, dpUnit: Float) {
    if (!theme.night) return
    for (i in 0 until 22) {
        val fx = frac(sin(i * 12.9898f) * 43758.55f)
        val fy = frac(sin(i * 4.1414f) * 27182.82f)
        val x = fx * size.width
        val y = fy * size.height * 0.55f
        val tw = 0.4f + 0.6f * (0.5f + 0.5f * sin(time * 3f + i * 1.7f))
        drawCircle(Palette.Twinkle.copy(alpha = tw * 0.9f), (0.9f + 0.7f * tw) * dpUnit, Offset(x, y))
    }
}

/** A soft rainbow that swells in and out on a very slow cycle. */
private fun DrawScope.drawRainbow(time: Float, dpUnit: Float) {
    val a = ((sin(time * 0.045f) - 0.5f) * 0.7f).coerceIn(0f, 0.3f)
    if (a <= 0.01f) return
    val cx = size.width * 0.5f
    val cy = size.height * 0.92f
    val rOuter = size.width * 0.7f
    val band = 9f * dpUnit
    Palette.Rainbow.forEachIndexed { i, col ->
        val rr = rOuter - i * band
        drawArc(
            color = col.copy(alpha = a),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - rr, cy - rr),
            size = Size(rr * 2f, rr * 2f),
            style = Stroke(width = band)
        )
    }
}

/** A smiling sun over day themes; a sleepy moon over night themes. */
private fun DrawScope.drawSunOrMoon(time: Float, theme: Palette.SkyTheme, dpUnit: Float) {
    val center = Offset(size.width * 0.84f, size.height * 0.16f)
    val base = 30f * dpUnit

    if (!theme.night) {
        val pulse = 1f + 0.05f * sin(time * 0.7f)
        for (i in 0 until 12) {
            val a = i * PI.toFloat() / 6f + time * 0.15f
            val inner = base * 1.25f * pulse
            val outer = base * (1.7f + 0.12f * sin(time * 2f + i)) * pulse
            drawLine(
                theme.sun.copy(alpha = 0.5f),
                Offset(center.x + cos(a) * inner, center.y + sin(a) * inner),
                Offset(center.x + cos(a) * outer, center.y + sin(a) * outer),
                strokeWidth = 3f * dpUnit, cap = StrokeCap.Round
            )
        }
        drawCircle(theme.sun.copy(alpha = 0.16f), base * 2.3f * pulse, center)
        drawCircle(theme.sun.copy(alpha = 0.26f), base * 1.6f * pulse, center)
        drawCircle(theme.sun.copy(alpha = 0.92f), base * pulse, center)
        val eyeY = center.y - base * 0.12f
        drawCircle(Palette.SunFace, base * 0.09f, Offset(center.x - base * 0.3f, eyeY))
        drawCircle(Palette.SunFace, base * 0.09f, Offset(center.x + base * 0.3f, eyeY))
        drawCircle(Palette.Cheek.copy(alpha = 0.5f), base * 0.12f, Offset(center.x - base * 0.5f, center.y + base * 0.12f))
        drawCircle(Palette.Cheek.copy(alpha = 0.5f), base * 0.12f, Offset(center.x + base * 0.5f, center.y + base * 0.12f))
        drawArc(
            Palette.SunFace, 20f, 140f, false,
            topLeft = Offset(center.x - base * 0.32f, center.y - base * 0.02f),
            size = Size(base * 0.64f, base * 0.5f),
            style = Stroke(width = 3f * dpUnit, cap = StrokeCap.Round)
        )
    } else {
        drawCircle(Palette.Moon.copy(alpha = 0.18f), base * 1.7f, center)
        drawCircle(Palette.Moon, base, center)
        drawCircle(Palette.MoonFace.copy(alpha = 0.15f), base * 0.2f, Offset(center.x + base * 0.3f, center.y - base * 0.3f))
        drawCircle(Palette.MoonFace.copy(alpha = 0.12f), base * 0.14f, Offset(center.x - base * 0.35f, center.y + base * 0.25f))
        val eyeY = center.y - base * 0.1f
        for (dx in listOf(-0.3f, 0.3f)) {
            drawArc(
                Palette.MoonFace, 200f, 140f, false,
                topLeft = Offset(center.x + dx * base - base * 0.12f, eyeY - base * 0.1f),
                size = Size(base * 0.24f, base * 0.2f),
                style = Stroke(width = 2.5f * dpUnit, cap = StrokeCap.Round)
            )
        }
        drawArc(
            Palette.MoonFace, 25f, 130f, false,
            topLeft = Offset(center.x - base * 0.22f, center.y + base * 0.02f),
            size = Size(base * 0.44f, base * 0.34f),
            style = Stroke(width = 2.5f * dpUnit, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawCloud(cloud: Cloud, theme: Palette.SkyTheme, dpUnit: Float) {
    val r = 22f * dpUnit * cloud.scale
    val a = theme.cloudAlpha
    drawCircle(Palette.Cloud.copy(alpha = a), r, Offset(cloud.x, cloud.y))
    drawCircle(Palette.Cloud.copy(alpha = a), r * 0.78f, Offset(cloud.x - r * 1.05f, cloud.y + r * 0.22f))
    drawCircle(Palette.Cloud.copy(alpha = a), r * 0.68f, Offset(cloud.x + r * 1.1f, cloud.y + r * 0.28f))
    drawCircle(Palette.Cloud.copy(alpha = a), r * 0.9f, Offset(cloud.x + r * 0.35f, cloud.y - r * 0.35f))
}

private fun DrawScope.drawHills(world: GameWorld, theme: Palette.SkyTheme, dpUnit: Float) {
    val g = world.groundY
    val w = size.width
    val h = size.height
    fun hill(color: Color, lift: Float, phase: Float, amplitude: Float) {
        val path = Path()
        path.moveTo(0f, h)
        path.lineTo(0f, g - lift)
        var x = 0f
        val step = w / 12f
        while (x < w) {
            val cx = x + step / 2f
            val cy = g - lift - amplitude * (1f + sin(phase + x / w * 6f)) / 2f
            path.quadraticTo(cx, cy, x + step, g - lift)
            x += step
        }
        path.lineTo(w, h)
        path.close()
        drawPath(path, color)
    }
    hill(theme.hillFar, 34f * dpUnit, 0.4f, 26f * dpUnit)
    hill(theme.hillNear, 12f * dpUnit, 2.1f, 18f * dpUnit)
}

private fun DrawScope.drawGround(world: GameWorld, theme: Palette.SkyTheme) {
    val g = world.groundY
    drawRect(
        Brush.verticalGradient(
            listOf(theme.grass, theme.grassDark),
            startY = g,
            endY = size.height
        ),
        topLeft = Offset(0f, g),
        size = Size(size.width, size.height - g)
    )
}

private fun DrawScope.drawDivider(world: GameWorld, dpUnit: Float) {
    val x = size.width / 2f
    drawLine(
        color = Color.White.copy(alpha = 0.55f),
        start = Offset(x, 0f),
        end = Offset(x, world.groundY),
        strokeWidth = 3f * dpUnit,
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(10f * dpUnit, 12f * dpUnit)
        )
    )
}

// ---------------------------------------------------------------------- garden

private fun DrawScope.drawFlower(flower: Flower, world: GameWorld, dpUnit: Float) {
    val alpha = flower.alpha(world.time)
    if (alpha <= 0f) return
    val g = flower.growth(world.time).coerceAtLeast(0f)
    if (g <= 0.01f) return

    val baseY = world.groundY + 2f * dpUnit
    val height = flower.stemHeight * g
    val sway = sin(world.time * 1.5f + flower.swayPhase) * 5f * dpUnit * g
    val tipX = flower.x + sway
    val tipY = baseY - height

    val stem = Path().apply {
        moveTo(flower.x, baseY)
        quadraticTo(flower.x + sway * 0.35f, baseY - height * 0.55f, tipX, tipY)
    }
    drawPath(
        stem,
        Palette.Stem.copy(alpha = alpha),
        style = Stroke(width = 4.5f * dpUnit, cap = StrokeCap.Round)
    )

    // One leaf, alternating side per flower so a full row does not look stamped.
    val leafUp = flower.petalCount % 2 == 0
    val leafY = baseY - height * 0.45f
    val leafX = flower.x + sway * 0.2f
    val lw = 13f * dpUnit * g
    val lh = 7f * dpUnit * g
    drawOval(
        Palette.Leaf.copy(alpha = alpha),
        topLeft = Offset(if (leafUp) leafX else leafX - lw, leafY - lh / 2f),
        size = Size(lw, lh)
    )

    val pr = flower.petalRadius * g
    val petal = flower.petalColor.copy(alpha = alpha)
    val centre = flower.centerColor.copy(alpha = alpha)
    when (flower.kind) {
        FlowerKind.ROUND -> {
            for (i in 0 until flower.petalCount) {
                val angle = i * 2.0 * PI / flower.petalCount + world.time * 0.12
                val px = tipX + cos(angle).toFloat() * pr * 0.95f
                val py = tipY + sin(angle).toFloat() * pr * 0.95f
                drawCircle(petal, pr, Offset(px, py))
            }
            drawCircle(centre, pr * 0.66f, Offset(tipX, tipY))
        }
        FlowerKind.DAISY -> {
            // Slender petals radiating out — a classic daisy.
            for (i in 0 until 9) {
                val angle = i * 2.0 * PI / 9.0 + world.time * 0.1
                val px = tipX + cos(angle).toFloat() * pr * 0.9f
                val py = tipY + sin(angle).toFloat() * pr * 0.9f
                rotate((angle * 57.2958).toFloat(), Offset(px, py)) {
                    drawOval(petal, topLeft = Offset(px - pr * 0.75f, py - pr * 0.3f), size = Size(pr * 1.5f, pr * 0.6f))
                }
            }
            drawCircle(centre, pr * 0.6f, Offset(tipX, tipY))
        }
        FlowerKind.TULIP -> {
            // A little cup of three petals.
            drawCircle(petal, pr * 0.9f, Offset(tipX, tipY + pr * 0.2f))
            val cup = Path().apply {
                moveTo(tipX - pr, tipY + pr * 0.2f)
                quadraticTo(tipX - pr * 0.9f, tipY - pr * 1.2f, tipX, tipY - pr * 0.2f)
                quadraticTo(tipX + pr * 0.9f, tipY - pr * 1.2f, tipX + pr, tipY + pr * 0.2f)
                quadraticTo(tipX, tipY + pr * 0.9f, tipX - pr, tipY + pr * 0.2f)
                close()
            }
            drawPath(cup, petal)
            drawLine(centre, Offset(tipX, tipY - pr * 0.6f), Offset(tipX, tipY + pr * 0.4f), 2f * dpUnit)
        }
    }
}

// --------------------------------------------------------------------- bubbles

private fun DrawScope.drawBubble(bubble: Bubble, time: Float, dpUnit: Float, measurer: TextMeasurer? = null, highlight: Boolean = false, urgent: Boolean = false) {
    val c = Offset(bubble.x, bubble.y)
    // A gentle breathing pulse so even a still bubble feels alive.
    val r = bubble.radius * (1f + 0.03f * sin(time * 2.5f + bubble.swayPhase))

    // The answer being asked for gets a pulsing golden halo, so a child who
    // can't yet read still knows which bubble to reach for. When they seem
    // stuck, the halo turns brighter and faster.
    if (highlight) {
        val pulse = 0.5f + 0.5f * sin(time * (if (urgent) 9f else 5f))
        val glowAlpha = if (urgent) 0.30f + 0.30f * pulse else 0.18f + 0.22f * pulse
        val glowSize = r * (1.45f + (if (urgent) 0.22f else 0.12f) * pulse)
        drawCircle(Palette.Gold.copy(alpha = glowAlpha.coerceAtMost(0.6f)), glowSize, c)
        drawCircle(
            Palette.Gold.copy(alpha = 0.85f), r * 1.16f, c,
            style = Stroke(width = (if (urgent) 6f else 4f) * dpUnit)
        )
    }

    // Soft drop shadow gives every kind some weight without an outline.
    drawCircle(Color.Black.copy(alpha = 0.07f), r, Offset(c.x + 2f * dpUnit, c.y + 4f * dpUnit))

    // Learning bubbles that carry a number or letter: a real balloon with the
    // character printed on it, instead of a face.
    if (bubble.label.isNotEmpty() && measurer != null) {
        drawBalloonBody(c, r, bubble.color, dpUnit)
        drawBubbleLabel(bubble.label, c, bubble.radius, dpUnit, measurer)
        return
    }

    // Balloons blink every few seconds, each on its own rhythm.
    val blink = blinkOf(time, bubble.swayPhase)
    when (bubble.kind) {
        BubbleKind.BALLOON -> {
            drawBalloonBody(c, r, bubble.color, dpUnit)
            drawFace(c, r * 0.88f, bubble.face, dpUnit, blink)
        }
        BubbleKind.STAR -> {
            drawStarBody(c, r, bubble.color, time, dpUnit)
            drawFace(c, r * 0.60f, bubble.face, dpUnit, blink)
        }
        BubbleKind.HEART -> {
            drawHeartBody(c, r, bubble.color, dpUnit)
            drawFace(Offset(c.x, c.y - r * 0.10f), r * 0.58f, bubble.face, dpUnit, blink)
        }
        BubbleKind.RAINBOW -> {
            drawRainbowBody(c, r, time, dpUnit)
            drawFace(c, r, bubble.face, dpUnit, blink)
        }
        BubbleKind.BUBBLE -> {
            drawRoundBody(c, r, bubble.color, dpUnit)
            drawFace(c, r, bubble.face, dpUnit, blink)
        }
    }
}

/** 1 = eyes open; briefly ~0.1 while blinking. Staggered per balloon by phase. */
private fun blinkOf(time: Float, phase: Float): Float {
    val t = ((time * 0.34f + phase * 0.16f) % 1f + 1f) % 1f
    return if (t > 0.91f) 0.12f else 1f
}

private fun DrawScope.drawBubbleLabel(text: String, c: Offset, baseRadius: Float, dpUnit: Float, measurer: TextMeasurer) {
    val fontSize = (baseRadius * 1.15f / dpUnit)
    val layout = measurer.measure(
        text = text,
        style = TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Black, color = Palette.Ink)
    )
    drawText(
        layout,
        topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f)
    )
}

private fun DrawScope.drawRoundBody(c: Offset, r: Float, color: Color, dpUnit: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                lerp(color, Color.White, 0.35f),
                color,
                lerp(color, Color.Black, 0.10f),
            ),
            center = Offset(c.x - r * 0.30f, c.y - r * 0.34f),
            radius = r * 1.45f
        ),
        radius = r,
        center = c
    )
    drawCircle(Color.White.copy(alpha = 0.5f), r, c, style = Stroke(width = 3f * dpUnit))
    gloss(c, r)
}

private fun DrawScope.drawBalloonBody(c: Offset, r: Float, color: Color, dpUnit: Float) {
    val rx = r * 0.92f
    val ry = r * 1.12f

    // The dangling string, drawn first so the balloon sits on top of its knot.
    val knot = Offset(c.x, c.y + ry)
    val string = Path().apply {
        moveTo(knot.x, knot.y)
        quadraticTo(knot.x + r * 0.35f, knot.y + r * 0.5f, knot.x - r * 0.12f, knot.y + r * 1.1f)
    }
    drawPath(string, Palette.InkSoft.copy(alpha = 0.55f), style = Stroke(width = 2f * dpUnit, cap = StrokeCap.Round))

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(lerp(color, Color.White, 0.38f), color, lerp(color, Color.Black, 0.10f)),
            center = Offset(c.x - rx * 0.30f, c.y - ry * 0.34f),
            radius = ry * 1.4f
        ),
        topLeft = Offset(c.x - rx, c.y - ry),
        size = Size(rx * 2f, ry * 2f)
    )
    // A little triangular knot at the bottom.
    val tri = Path().apply {
        moveTo(c.x, knot.y - r * 0.06f)
        lineTo(c.x - r * 0.12f, knot.y + r * 0.12f)
        lineTo(c.x + r * 0.12f, knot.y + r * 0.12f)
        close()
    }
    drawPath(tri, lerp(color, Color.Black, 0.12f))
    drawOval(
        Color.White.copy(alpha = 0.5f),
        topLeft = Offset(c.x - rx, c.y - ry),
        size = Size(rx * 2f, ry * 2f),
        style = Stroke(width = 3f * dpUnit)
    )
    gloss(Offset(c.x, c.y - r * 0.1f), r)
}

private fun DrawScope.drawStarBody(c: Offset, r: Float, color: Color, time: Float, dpUnit: Float) {
    val rot = time * 0.25f
    val path = starPath(c.x, c.y, r * 1.02f, r * 0.5f, 5, rot)
    drawPath(
        path,
        brush = Brush.radialGradient(
            colors = listOf(lerp(color, Color.White, 0.4f), color, lerp(color, Palette.Gold, 0.35f)),
            center = Offset(c.x - r * 0.25f, c.y - r * 0.3f),
            radius = r * 1.5f
        )
    )
    drawPath(path, Color.White.copy(alpha = 0.5f), style = Stroke(width = 3f * dpUnit))
    gloss(c, r * 0.7f)
}

private fun DrawScope.drawHeartBody(c: Offset, r: Float, color: Color, dpUnit: Float) {
    val path = heartPath(c.x, c.y, r * 0.82f)
    drawPath(
        path,
        brush = Brush.radialGradient(
            colors = listOf(lerp(color, Color.White, 0.38f), color, lerp(color, Color.Black, 0.10f)),
            center = Offset(c.x - r * 0.25f, c.y - r * 0.35f),
            radius = r * 1.6f
        )
    )
    drawPath(path, Color.White.copy(alpha = 0.5f), style = Stroke(width = 3f * dpUnit))
    gloss(Offset(c.x, c.y - r * 0.2f), r * 0.8f)
}

private fun DrawScope.drawRainbowBody(c: Offset, r: Float, time: Float, dpUnit: Float) {
    // A pulsing golden halo announces the rare celebration bubble.
    val halo = 0.5f + 0.5f * sin(time * 4f)
    drawCircle(Palette.Gold.copy(alpha = 0.10f + 0.14f * halo), r * (1.5f + 0.15f * halo), c)

    val ring = Palette.Rainbow + Palette.Rainbow.first()
    drawCircle(brush = Brush.sweepGradient(ring, c), radius = r, center = c)
    // Soften the centre so the face still reads clearly on top.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.0f)),
            center = c, radius = r * 0.9f
        ),
        radius = r, center = c
    )
    drawCircle(Color.White.copy(alpha = 0.7f), r, c, style = Stroke(width = 3.5f * dpUnit))
    gloss(c, r)
}

private fun DrawScope.gloss(c: Offset, r: Float) {
    drawCircle(Color.White.copy(alpha = 0.75f), r * 0.20f, Offset(c.x - r * 0.40f, c.y - r * 0.46f))
    drawCircle(Color.White.copy(alpha = 0.45f), r * 0.09f, Offset(c.x - r * 0.14f, c.y - r * 0.60f))
}

/**
 * A face. Small children reliably reach for eyes before shapes, and it turns
 * "a target" into "a friend". A handful of expressions keeps the crowd lively.
 */
private fun DrawScope.drawFace(center: Offset, r: Float, face: Int, dpUnit: Float, blink: Float = 1f) {
    val eyeR = r * 0.105f
    val eyeY = center.y - r * 0.10f
    val lx = center.x - r * 0.27f
    val rx = center.x + r * 0.27f
    val stroke = 3f * dpUnit

    fun openEye(x: Float) {
        // The eye squashes shut for a moment when the balloon blinks.
        drawOval(Palette.Ink, Offset(x - eyeR, eyeY - eyeR * blink), Size(eyeR * 2f, eyeR * 2f * blink))
        if (blink > 0.5f) {
            drawCircle(Color.White.copy(alpha = 0.9f), eyeR * 0.38f, Offset(x - eyeR * 0.35f, eyeY - eyeR * 0.35f))
        }
    }
    fun closedEye(x: Float) {
        // A cheerful upward "^" arc for a wink or a giggle.
        drawArc(
            color = Palette.Ink,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(x - eyeR * 1.3f, eyeY - eyeR),
            size = Size(eyeR * 2.6f, eyeR * 2.2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }

    when (face) {
        2 -> { openEye(lx); closedEye(rx) }   // wink
        3 -> { closedEye(lx); closedEye(rx) } // giggle
        else -> { openEye(lx); openEye(rx) }
    }

    if (face == 1) {
        // A big open grin.
        drawArc(
            color = Palette.Ink,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(center.x - r * 0.28f, center.y - r * 0.02f),
            size = Size(r * 0.56f, r * 0.5f)
        )
        drawCircle(Color(0xFFFF7A8A), r * 0.09f, Offset(center.x, center.y + r * 0.2f))
    } else {
        drawArc(
            color = Palette.Ink,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(center.x - r * 0.32f, center.y - r * 0.06f),
            size = Size(r * 0.64f, r * 0.50f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

// ------------------------------------------------------------------- particles

private fun DrawScope.drawParticle(p: Particle) {
    val flicker = if (p.twinkle) 0.45f + 0.55f * abs(sin(p.life * 26f)) else 1f
    val alpha = p.fade * flicker
    if (alpha <= 0.01f) return
    val col = p.color.copy(alpha = alpha)
    val s = p.radius * (0.7f + 0.5f * p.fade)
    val pivot = Offset(p.x, p.y)
    when (p.shape) {
        ParticleShape.CIRCLE -> drawCircle(col, s, pivot)
        ParticleShape.STAR -> rotate(p.rotation * 57.2958f, pivot) {
            drawPath(starPath(p.x, p.y, s * 1.4f, s * 0.6f, 5, 0f), col)
        }
        ParticleShape.HEART -> rotate(p.rotation * 57.2958f, pivot) {
            drawPath(heartPath(p.x, p.y, s * 1.1f), col)
        }
        ParticleShape.SPARKLE -> rotate(p.rotation * 57.2958f, pivot) {
            drawPath(starPath(p.x, p.y, s * 1.7f, s * 0.4f, 4, 0f), col)
        }
    }
}

// -------------------------------------------------------------------- visitors

private fun DrawScope.drawVisitor(b: Butterfly, dpUnit: Float) {
    when (b.kind) {
        VisitorKind.BUTTERFLY -> drawButterfly(b)
        VisitorKind.BEE -> drawBee(b, dpUnit)
        VisitorKind.LADYBUG -> drawLadybug(b, dpUnit)
        VisitorKind.BIRD -> drawBird(b, dpUnit)
    }
}

private fun DrawScope.drawButterfly(b: Butterfly) {
    val flap = 0.30f + 0.70f * abs(b.wing)
    val w = b.size * flap
    val h = b.size * 1.35f
    drawOval(b.color.copy(alpha = 0.92f), Offset(b.x - w, b.y - h / 2f), Size(w, h), style = Fill)
    drawOval(b.color.copy(alpha = 0.92f), Offset(b.x, b.y - h / 2f), Size(w, h), style = Fill)
    drawOval(Palette.Ink, Offset(b.x - b.size * 0.09f, b.y - b.size * 0.5f), Size(b.size * 0.18f, b.size))
}

private fun DrawScope.drawBee(b: Butterfly, dpUnit: Float) {
    val flap = 0.4f + 0.6f * abs(b.wing)
    val ww = b.size * 0.9f * flap
    // Two translucent wings that flutter above the body.
    drawOval(Color.White.copy(alpha = 0.6f), Offset(b.x - ww, b.y - b.size * 0.95f), Size(ww, b.size * 0.8f))
    drawOval(Color.White.copy(alpha = 0.6f), Offset(b.x, b.y - b.size * 0.95f), Size(ww, b.size * 0.8f))

    val bw = b.size * 1.5f
    val bh = b.size
    drawOval(Palette.Bee, Offset(b.x - bw / 2f, b.y - bh / 2f), Size(bw, bh))
    // Stripes.
    for (i in -1..1) {
        val sx = b.x + i * bw * 0.22f
        drawLine(Palette.BeeStripe, Offset(sx, b.y - bh * 0.42f), Offset(sx, b.y + bh * 0.42f), 3f * dpUnit)
    }
    // Head with a friendly dot eye.
    drawCircle(Palette.BeeStripe, bh * 0.42f, Offset(b.x + bw * 0.5f, b.y))
    drawCircle(Color.White, bh * 0.12f, Offset(b.x + bw * 0.56f, b.y - bh * 0.08f))
}

private fun DrawScope.drawLadybug(b: Butterfly, dpUnit: Float) {
    val body = b.size * 1.3f
    // Red dome.
    drawCircle(Palette.Ladybug, body, Offset(b.x, b.y))
    // Wing split down the middle.
    drawLine(Palette.Ink, Offset(b.x, b.y - body), Offset(b.x, b.y + body), 2.5f * dpUnit)
    // Spots.
    val spots = listOf(-0.45f to -0.35f, 0.45f to -0.35f, -0.4f to 0.35f, 0.4f to 0.35f)
    spots.forEach { (dx, dy) ->
        drawCircle(Palette.Ink, body * 0.2f, Offset(b.x + dx * body, b.y + dy * body))
    }
    // Head.
    drawCircle(Palette.Ink, body * 0.42f, Offset(b.x, b.y - body * 0.95f))
}

private fun DrawScope.drawBird(b: Butterfly, dpUnit: Float) {
    val body = b.size
    // Two flapping wing arcs give the classic "m" bird silhouette.
    val lift = b.wing * body * 0.5f
    val wing = Path().apply {
        moveTo(b.x - body * 1.4f, b.y + lift)
        quadraticTo(b.x - body * 0.5f, b.y - body * 0.9f, b.x, b.y)
        quadraticTo(b.x + body * 0.5f, b.y - body * 0.9f, b.x + body * 1.4f, b.y + lift)
    }
    drawPath(wing, b.color, style = Stroke(width = 4.5f * dpUnit, cap = StrokeCap.Round))
    drawCircle(b.color, body * 0.42f, Offset(b.x, b.y))
    drawCircle(Palette.Ink, body * 0.1f, Offset(b.x + body * 0.18f, b.y - body * 0.08f))
    // Little orange beak.
    val beak = Path().apply {
        moveTo(b.x + body * 0.35f, b.y)
        lineTo(b.x + body * 0.7f, b.y - body * 0.08f)
        lineTo(b.x + body * 0.35f, b.y + body * 0.18f)
        close()
    }
    drawPath(beak, Palette.Bee)
}

// ------------------------------------------------------------ ambient extras

/** A rare streak of light across a night-time sky. */
private fun DrawScope.drawShootingStar(time: Float, theme: Palette.SkyTheme, dpUnit: Float) {
    if (!theme.night) return
    val period = 24f
    val p = (time % period) / period
    val window = 0.1f
    if (p > window) return
    val t = p / window
    val x = size.width * (0.12f + 0.7f * t)
    val y = size.height * (0.10f + 0.16f * t)
    val len = 46f * dpUnit
    val a = sin(PI.toFloat() * t)
    drawLine(Palette.Twinkle.copy(alpha = a * 0.55f), Offset(x - len, y - len * 0.5f), Offset(x, y), 2.2f * dpUnit, StrokeCap.Round)
    drawCircle(Palette.Twinkle.copy(alpha = a), 2.6f * dpUnit, Offset(x, y))
}

/** A friendly animal strolling the grass — drawn facing right, then mirrored to
 * match the way it is walking. */
private fun DrawScope.drawGroundCritter(c: GroundCritter, dpUnit: Float) {
    val sx = if (c.vx >= 0f) 1f else -1f
    scale(sx, 1f, Offset(c.x, c.baseY)) {
        drawCritterFacingRight(c, dpUnit)
    }
}

private fun DrawScope.drawCritterFacingRight(c: GroundCritter, dpUnit: Float) {
    val s = c.size
    val hop = if (c.kind == GroundCritterKind.BUNNY) abs(sin(c.step)) * s * 0.5f else 0f
    val feetY = c.baseY - hop
    val cx = c.x
    val bodyCy = feetY - s * 0.9f
    val ink = Palette.Ink
    val col = c.color

    // Tail (the back of the animal is to the left).
    when (c.kind) {
        GroundCritterKind.PUPPY ->
            drawLine(col, Offset(cx - s, bodyCy), Offset(cx - s * 1.5f, bodyCy - s * 0.5f), 4f * dpUnit, StrokeCap.Round)
        GroundCritterKind.CAT -> {
            val tail = Path().apply {
                moveTo(cx - s * 0.9f, bodyCy)
                quadraticTo(cx - s * 1.6f, bodyCy - s * 0.2f, cx - s * 1.4f, bodyCy - s * 1.1f)
            }
            drawPath(tail, col, style = Stroke(width = 4f * dpUnit, cap = StrokeCap.Round))
        }
        GroundCritterKind.BUNNY -> drawCircle(Color.White, s * 0.28f, Offset(cx - s * 0.95f, bodyCy + s * 0.2f))
        GroundCritterKind.DUCK -> {}
    }

    // Legs, stepping in a little walk cycle.
    if (c.kind == GroundCritterKind.DUCK) {
        for (i in 0..1) {
            val lx = cx + (i * 2 - 1) * s * 0.3f
            val off = sin(c.step + i * PI.toFloat()) * s * 0.15f
            drawLine(Color(0xFFF6913E), Offset(lx, feetY - s * 0.4f), Offset(lx + off, feetY), 3f * dpUnit, StrokeCap.Round)
        }
    } else {
        for (i in 0..3) {
            val lx = cx + (i - 1.5f) * s * 0.45f
            val off = sin(c.step + i * (PI.toFloat() / 2f)) * s * 0.15f
            drawLine(col, Offset(lx, feetY - s * 0.5f), Offset(lx + off, feetY), 4f * dpUnit, StrokeCap.Round)
        }
    }

    // Body.
    drawOval(col, topLeft = Offset(cx - s, bodyCy - s * 0.6f), size = Size(s * 2f, s * 1.2f))

    // Head at the front (right).
    val headC = Offset(cx + s * 0.95f, bodyCy - s * 0.35f)
    drawCircle(col, s * 0.62f, headC)

    // Ears or beak.
    when (c.kind) {
        GroundCritterKind.PUPPY ->
            drawOval(col.darker(), topLeft = Offset(headC.x - s * 0.7f, headC.y - s * 0.2f), size = Size(s * 0.5f, s * 0.85f))
        GroundCritterKind.CAT -> {
            drawPath(Path().apply {
                moveTo(headC.x - s * 0.1f, headC.y - s * 0.5f)
                lineTo(headC.x - s * 0.4f, headC.y - s * 1.0f)
                lineTo(headC.x - s * 0.55f, headC.y - s * 0.4f); close()
            }, col)
            drawPath(Path().apply {
                moveTo(headC.x + s * 0.3f, headC.y - s * 0.5f)
                lineTo(headC.x + s * 0.55f, headC.y - s * 1.0f)
                lineTo(headC.x + s * 0.6f, headC.y - s * 0.35f); close()
            }, col)
        }
        GroundCritterKind.BUNNY -> {
            drawOval(col, topLeft = Offset(headC.x - s * 0.1f, headC.y - s * 1.35f), size = Size(s * 0.28f, s * 1.1f))
            drawOval(col, topLeft = Offset(headC.x + s * 0.32f, headC.y - s * 1.35f), size = Size(s * 0.28f, s * 1.1f))
        }
        GroundCritterKind.DUCK ->
            drawPath(Path().apply {
                moveTo(headC.x + s * 0.5f, headC.y)
                lineTo(headC.x + s * 1.1f, headC.y - s * 0.12f)
                lineTo(headC.x + s * 0.5f, headC.y + s * 0.24f); close()
            }, Color(0xFFF6913E))
    }

    // A happy face.
    drawCircle(ink, s * 0.09f, Offset(headC.x + s * 0.2f, headC.y - s * 0.1f))
    if (c.kind != GroundCritterKind.DUCK) {
        drawCircle(ink, s * 0.08f, Offset(headC.x + s * 0.55f, headC.y + s * 0.05f))
        drawArc(
            ink, 10f, 80f, false,
            topLeft = Offset(headC.x, headC.y),
            size = Size(s * 0.5f, s * 0.4f),
            style = Stroke(width = 2f * dpUnit, cap = StrokeCap.Round)
        )
    }
}

/** A big, cheerful striped hot-air balloon drifting across the top of the sky. */
private fun DrawScope.drawSkyRider(rider: SkyRider, dpUnit: Float) {
    val s = dpUnit * rider.scale
    val cx = rider.x
    val cy = rider.y
    val rw = 26f * s
    val rh = 32f * s

    // Ropes down to the basket.
    val basketY = cy + rh + 16f * s
    drawLine(Palette.InkSoft.copy(alpha = 0.6f), Offset(cx - rw * 0.5f, cy + rh * 0.6f), Offset(cx - 5f * s, basketY), 1.5f * dpUnit)
    drawLine(Palette.InkSoft.copy(alpha = 0.6f), Offset(cx + rw * 0.5f, cy + rh * 0.6f), Offset(cx + 5f * s, basketY), 1.5f * dpUnit)

    // Striped envelope, striped by clipping vertical bands to the balloon oval.
    val env = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(cx - rw, cy - rh, cx + rw, cy + rh))
    }
    clipPath(env) {
        drawRect(rider.color.copy(alpha = 0.92f), topLeft = Offset(cx - rw, cy - rh), size = Size(rw * 2f, rh * 2f))
        var i = -3
        while (i <= 3) {
            if (i % 2 == 0) {
                drawRect(Color.White.copy(alpha = 0.85f), topLeft = Offset(cx + i * rw * 0.28f, cy - rh), size = Size(rw * 0.28f, rh * 2f))
            }
            i++
        }
    }
    drawOval(Color.White.copy(alpha = 0.35f), topLeft = Offset(cx - rw, cy - rh), size = Size(rw * 2f, rh * 2f), style = Stroke(width = 2f * dpUnit))

    // The little basket.
    drawRoundRectCompat(cx - 6f * s, basketY, 12f * s, 9f * s, 2f * s, Color(0xFF9B6B3E))
}

/** Small grass tufts and the odd toadstool along the ground, for a fuller garden. */
private fun DrawScope.drawGrassTufts(world: GameWorld, theme: Palette.SkyTheme, dpUnit: Float) {
    val g = world.groundY
    val w = size.width
    val bladeColor = lerp(theme.grassDark, Color.Black, 0.15f)
    val n = (w / (46f * dpUnit)).toInt().coerceIn(6, 44)
    for (i in 0 until n) {
        val fx = frac(sin(i * 7.13f) * 991.73f)
        val x = (i + 0.15f + fx * 0.7f) * (w / n)
        val h = (8f + 6f * frac(sin(i * 3.1f) * 55.3f)) * dpUnit
        for (b in -1..1) {
            val bx = x + b * 3f * dpUnit
            val tip = Offset(bx + b * 4f * dpUnit, g - h)
            val blade = Path().apply {
                moveTo(x, g + 2f * dpUnit)
                quadraticTo(bx, g - h * 0.5f, tip.x, tip.y)
            }
            drawPath(blade, bladeColor.copy(alpha = 0.8f), style = Stroke(width = 2.5f * dpUnit, cap = StrokeCap.Round))
        }
        // A rare little red toadstool.
        if (i % 7 == 3) {
            val mx = x + 10f * dpUnit
            drawRoundRectCompat(mx - 2f * dpUnit, g - 8f * dpUnit, 4f * dpUnit, 8f * dpUnit, 2f * dpUnit, Color(0xFFFFF3E0))
            drawArc(
                Palette.Ladybug, 180f, 180f, true,
                topLeft = Offset(mx - 6f * dpUnit, g - 12f * dpUnit),
                size = Size(12f * dpUnit, 12f * dpUnit)
            )
            drawCircle(Color.White, 1.4f * dpUnit, Offset(mx - 2f * dpUnit, g - 8.5f * dpUnit))
            drawCircle(Color.White, 1.2f * dpUnit, Offset(mx + 2f * dpUnit, g - 9f * dpUnit))
        }
    }
}

/** Glowing fireflies that come out over night themes, low over the grass. */
private fun DrawScope.drawFireflies(world: GameWorld, theme: Palette.SkyTheme, dpUnit: Float) {
    if (!theme.night) return
    val g = world.groundY
    for (i in 0 until 9) {
        val fx = frac(sin(i * 21.11f) * 3971.1f)
        val drift = sin(world.time * 0.5f + i) * 0.06f
        val x = ((fx + drift) % 1f).let { if (it < 0) it + 1f else it } * size.width
        val y = g - (10f + 40f * frac(sin(i * 5.7f) * 88.1f)) * dpUnit + sin(world.time * 1.3f + i * 2f) * 8f * dpUnit
        val glow = 0.35f + 0.65f * (0.5f + 0.5f * sin(world.time * 4f + i * 1.7f))
        drawCircle(Palette.Firefly.copy(alpha = glow * 0.25f), 6f * dpUnit, Offset(x, y))
        drawCircle(Palette.Firefly.copy(alpha = glow), 2f * dpUnit, Offset(x, y))
    }
}

/** A single drifting petal, leaf or wandering sparkle. */
private fun DrawScope.drawFloater(f: Floater, dpUnit: Float) {
    val a = 0.55f
    val col = f.color.copy(alpha = a)
    val pivot = Offset(f.x, f.y)
    rotate(f.rotation * 57.2958f, pivot) {
        when (f.kind) {
            FloaterKind.PETAL -> drawOval(col, topLeft = Offset(f.x - f.size * 0.6f, f.y - f.size), size = Size(f.size * 1.2f, f.size * 2f))
            FloaterKind.LEAF -> {
                drawOval(f.color.copy(alpha = a), topLeft = Offset(f.x - f.size, f.y - f.size * 0.5f), size = Size(f.size * 2f, f.size))
                drawLine(Palette.Stem.copy(alpha = a * 0.7f), Offset(f.x - f.size, f.y), Offset(f.x + f.size, f.y), 1f * dpUnit)
            }
            FloaterKind.SPARKLE -> drawPath(starPath(f.x, f.y, f.size, f.size * 0.4f, 4, 0f), col)
        }
    }
}

/** The expanding shockwave ring left by a pop. */
private fun DrawScope.drawRipple(r: Ripple, dpUnit: Float) {
    val prog = r.t
    val radius = r.startRadius * (1f + 1.3f * prog)
    val alpha = (1f - prog) * 0.5f
    if (alpha <= 0.01f) return
    drawCircle(
        r.color.copy(alpha = alpha),
        radius,
        Offset(r.x, r.y),
        style = Stroke(width = (3f * (1f - prog) + 1f) * dpUnit)
    )
}

// --------------------------------------------------------------------- helpers

private fun DrawScope.drawRoundRectCompat(x: Float, y: Float, w: Float, h: Float, r: Float, color: Color) {
    drawRoundRect(
        color,
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
    )
}

private fun frac(v: Float): Float = v - floor(v)

private fun Color.darker(): Color = lerp(this, Color.Black, 0.2f)

/** A few far-off birds gliding across the daytime sky, wings flapping. */
private fun DrawScope.drawFarBirds(time: Float, theme: Palette.SkyTheme, dpUnit: Float) {
    if (theme.night) return
    val col = Palette.Ink.copy(alpha = 0.30f)
    for (i in 0 until 3) {
        val speed = (14f + i * 5f) * dpUnit
        val s = (7f + i * 2f) * dpUnit
        val x = ((time * speed + i * size.width / 3f) % (size.width + s * 4f)) - s * 2f
        val y = size.height * (0.15f + 0.05f * i) + sin(time * 1.2f + i * 2f) * 6f * dpUnit
        val flap = sin(time * 7f + i * 1.7f) * s * 0.45f
        val p = Path().apply {
            moveTo(x - s, y)
            quadraticTo(x - s / 2f, y - s * 0.7f - flap, x, y)
            quadraticTo(x + s / 2f, y - s * 0.7f - flap, x + s, y)
        }
        drawPath(p, col, style = Stroke(width = 2f * dpUnit, cap = StrokeCap.Round))
    }
}

private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float, points: Int, rot: Float): Path {
    val p = Path()
    val n = points * 2
    for (i in 0 until n) {
        val rr = if (i % 2 == 0) outer else inner
        val a = rot + i * PI.toFloat() / points - PI.toFloat() / 2f
        val x = cx + cos(a) * rr
        val y = cy + sin(a) * rr
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

private fun heartPath(cx: Float, cy: Float, s: Float): Path {
    val p = Path()
    p.moveTo(cx, cy + s * 0.95f)
    p.cubicTo(cx - s * 1.25f, cy + s * 0.1f, cx - s * 0.95f, cy - s * 0.95f, cx, cy - s * 0.3f)
    p.cubicTo(cx + s * 0.95f, cy - s * 0.95f, cx + s * 1.25f, cy + s * 0.1f, cx, cy + s * 0.95f)
    p.close()
    return p
}
