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
import androidx.compose.ui.graphics.lerp
import com.meetpatel.popgrow.game.Butterfly
import com.meetpatel.popgrow.game.Cloud
import com.meetpatel.popgrow.game.Flower
import com.meetpatel.popgrow.game.GameWorld
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Everything is drawn with vector primitives — there is not a single bitmap in
 * the app. That keeps the APK tiny and means the art is pin-sharp on every
 * screen density from a cheap phone to a 12" tablet.
 */

private const val SKY_STAGE_SECONDS = 30f

/** Sky, sun, clouds, hills and grass. Shared by the menu and the game. */
fun DrawScope.drawScenery(world: GameWorld, dpUnit: Float) {
    drawSky(world.time)
    drawSun(world.time, dpUnit)
    world.clouds.forEach { drawCloud(it, dpUnit) }
    drawHills(world, dpUnit)
    drawGround(world)
}

fun DrawScope.drawWorld(world: GameWorld, dpUnit: Float) {
    drawScenery(world, dpUnit)
    if (world.twoPlayer) drawDivider(world, dpUnit)
    world.flowers.forEach { drawFlower(it, world, dpUnit) }
    world.bubbles.forEach { drawBubble(it, dpUnit) }
    world.butterflies.forEach { drawButterfly(it) }
    world.particles.forEach {
        drawCircle(it.color, it.radius * it.fade, Offset(it.x, it.y), alpha = it.fade)
    }
}

// ------------------------------------------------------------------ background

private fun DrawScope.drawSky(time: Float) {
    val stages = Palette.SkyTops.size
    val pos = (time / SKY_STAGE_SECONDS) % stages
    val i = pos.toInt()
    val t = pos - i
    val j = (i + 1) % stages
    val top = lerp(Palette.SkyTops[i], Palette.SkyTops[j], t)
    val bottom = lerp(Palette.SkyBottoms[i], Palette.SkyBottoms[j], t)
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
}

private fun DrawScope.drawSun(time: Float, dpUnit: Float) {
    val center = Offset(size.width * 0.84f, size.height * 0.16f)
    val base = 30f * dpUnit
    val pulse = 1f + 0.05f * sin(time * 0.7f)
    drawCircle(Palette.Sun.copy(alpha = 0.16f), base * 2.3f * pulse, center)
    drawCircle(Palette.Sun.copy(alpha = 0.26f), base * 1.6f * pulse, center)
    drawCircle(Palette.Sun.copy(alpha = 0.92f), base * pulse, center)
}

private fun DrawScope.drawCloud(cloud: Cloud, dpUnit: Float) {
    val r = 22f * dpUnit * cloud.scale
    val a = 0.82f
    drawCircle(Palette.Cloud.copy(alpha = a), r, Offset(cloud.x, cloud.y))
    drawCircle(Palette.Cloud.copy(alpha = a), r * 0.78f, Offset(cloud.x - r * 1.05f, cloud.y + r * 0.22f))
    drawCircle(Palette.Cloud.copy(alpha = a), r * 0.68f, Offset(cloud.x + r * 1.1f, cloud.y + r * 0.28f))
    drawCircle(Palette.Cloud.copy(alpha = a), r * 0.9f, Offset(cloud.x + r * 0.35f, cloud.y - r * 0.35f))
}

private fun DrawScope.drawHills(world: GameWorld, dpUnit: Float) {
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
    hill(Palette.HillFar, 34f * dpUnit, 0.4f, 26f * dpUnit)
    hill(Palette.HillNear, 12f * dpUnit, 2.1f, 18f * dpUnit)
}

private fun DrawScope.drawGround(world: GameWorld) {
    val g = world.groundY
    drawRect(
        Brush.verticalGradient(
            listOf(Palette.Grass, Palette.GrassDark),
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
    for (i in 0 until flower.petalCount) {
        val angle = i * 2.0 * PI / flower.petalCount + world.time * 0.12
        val px = tipX + cos(angle).toFloat() * pr * 0.95f
        val py = tipY + sin(angle).toFloat() * pr * 0.95f
        drawCircle(flower.petalColor.copy(alpha = alpha), pr, Offset(px, py))
    }
    drawCircle(flower.centerColor.copy(alpha = alpha), pr * 0.66f, Offset(tipX, tipY))
}

// --------------------------------------------------------------------- bubbles

private fun DrawScope.drawBubble(bubble: com.meetpatel.popgrow.game.Bubble, dpUnit: Float) {
    val c = Offset(bubble.x, bubble.y)
    val r = bubble.radius

    // Soft drop shadow gives the bubble weight without an outline.
    drawCircle(Color.Black.copy(alpha = 0.07f), r, Offset(c.x + 2f * dpUnit, c.y + 4f * dpUnit))

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                lerp(bubble.color, Color.White, 0.35f),
                bubble.color,
                lerp(bubble.color, Color.Black, 0.10f),
            ),
            center = Offset(c.x - r * 0.30f, c.y - r * 0.34f),
            radius = r * 1.45f
        ),
        radius = r,
        center = c
    )
    drawCircle(Color.White.copy(alpha = 0.5f), r, c, style = Stroke(width = 3f * dpUnit))

    // A face. Small children reliably reach for eyes before they reach for shapes,
    // and it turns "a target" into "a friend" — which is the entire point here.
    val eyeR = r * 0.105f
    val eyeY = c.y - r * 0.10f
    drawCircle(Palette.Ink, eyeR, Offset(c.x - r * 0.27f, eyeY))
    drawCircle(Palette.Ink, eyeR, Offset(c.x + r * 0.27f, eyeY))
    drawCircle(Color.White.copy(alpha = 0.9f), eyeR * 0.38f, Offset(c.x - r * 0.31f, eyeY - eyeR * 0.35f))
    drawCircle(Color.White.copy(alpha = 0.9f), eyeR * 0.38f, Offset(c.x + r * 0.23f, eyeY - eyeR * 0.35f))

    drawArc(
        color = Palette.Ink,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(c.x - r * 0.32f, c.y - r * 0.06f),
        size = Size(r * 0.64f, r * 0.50f),
        style = Stroke(width = 3f * dpUnit, cap = StrokeCap.Round)
    )

    // Glossy highlights, drawn last so they sit on top of the face.
    drawCircle(Color.White.copy(alpha = 0.75f), r * 0.20f, Offset(c.x - r * 0.40f, c.y - r * 0.46f))
    drawCircle(Color.White.copy(alpha = 0.45f), r * 0.09f, Offset(c.x - r * 0.14f, c.y - r * 0.60f))
}

private fun DrawScope.drawButterfly(b: Butterfly) {
    val flap = 0.30f + 0.70f * abs(b.wing)
    val w = b.size * flap
    val h = b.size * 1.35f
    drawOval(
        b.color.copy(alpha = 0.92f),
        topLeft = Offset(b.x - w, b.y - h / 2f),
        size = Size(w, h),
        style = Fill
    )
    drawOval(
        b.color.copy(alpha = 0.92f),
        topLeft = Offset(b.x, b.y - h / 2f),
        size = Size(w, h),
        style = Fill
    )
    drawOval(
        Palette.Ink,
        topLeft = Offset(b.x - b.size * 0.09f, b.y - b.size * 0.5f),
        size = Size(b.size * 0.18f, b.size)
    )
}
