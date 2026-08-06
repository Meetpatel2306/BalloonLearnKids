package com.meetpatel.popgrow.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * The cartoon balloon the menu and splash screens are built from: a glossy
 * teardrop with a knot and a wavy string, big eyes that blink, and one of four
 * expressions. Hand-drawn with vectors like everything else, so it costs the
 * APK nothing and stays pin-sharp at any size, on any screen.
 *
 * Face styles: 0 = open grin, 1 = gentle smile, 2 = surprised "o",
 * 3 = giggling with happy closed eyes.
 */
fun DrawScope.drawCuteBalloon(
    c: Offset,
    r: Float,
    color: Color,
    dpUnit: Float,
    wiggle: Float = 0f,
    rainbow: Boolean = false,
    face: Boolean = true,
    faceStyle: Int = 0,
) {
    val rx = r
    val ry = r * 1.16f
    val top = Offset(c.x - rx, c.y - ry)
    val bodySize = Size(rx * 2f, ry * 2f)
    val knotY = c.y + ry

    // Wavy string first, so the balloon sits on top of it.
    val sway = sin(wiggle * 6.2832f) * r * 0.22f
    val string = Path().apply {
        moveTo(c.x, knotY + 2f * dpUnit)
        cubicTo(
            c.x + r * 0.30f, knotY + r * 0.45f,
            c.x - r * 0.30f + sway, knotY + r * 0.85f,
            c.x + sway, knotY + r * 1.25f,
        )
    }
    drawPath(string, Color(0xFF8E99AB), style = Stroke(width = 2.2f * dpUnit, cap = StrokeCap.Round))

    // Soft shadow behind the body.
    drawOval(Color.Black.copy(alpha = 0.10f), Offset(top.x + 3f * dpUnit, top.y + 5f * dpUnit), bodySize)

    if (rainbow) {
        // Seven vertical rainbow stripes clipped to the balloon.
        val body = Path().apply {
            addOval(Rect(top.x, top.y, top.x + bodySize.width, top.y + bodySize.height))
        }
        clipPath(body) {
            val bands = Palette.Rainbow + Palette.Warm[3]
            val bw = bodySize.width / bands.size
            bands.forEachIndexed { i, col ->
                drawRect(col, Offset(top.x + i * bw, top.y), Size(bw + 1f, bodySize.height))
            }
            drawOval(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(c.x - rx * 0.35f, c.y - ry * 0.40f),
                    radius = r * 1.3f
                ),
                top, bodySize
            )
        }
    } else {
        drawOval(
            Brush.radialGradient(
                listOf(lerp(color, Color.White, 0.45f), color, lerp(color, Color.Black, 0.15f)),
                center = Offset(c.x - rx * 0.35f, c.y - ry * 0.40f),
                radius = ry * 1.35f
            ),
            topLeft = top, size = bodySize
        )
    }

    // Knot.
    val knot = Path().apply {
        moveTo(c.x, knotY - 1.5f * dpUnit)
        lineTo(c.x - r * 0.15f, knotY + r * 0.13f)
        lineTo(c.x + r * 0.15f, knotY + r * 0.13f)
        close()
    }
    drawPath(knot, if (rainbow) Color(0xFFE84D8A) else lerp(color, Color.Black, 0.18f))

    // Gloss.
    drawOval(Color.White.copy(alpha = 0.55f), Offset(c.x - rx * 0.68f, c.y - ry * 0.72f), Size(rx * 0.52f, ry * 0.36f))

    if (face) {
        // Every so often the eyes close for a moment — a blink.
        val fr = ((wiggle % 1f) + 1f) % 1f
        val blink = if (fr > 0.90f && fr < 0.97f) 0.12f else 1f

        val eyeW = r * 0.36f
        val eyeH = r * 0.44f
        val eyeY = c.y - ry * 0.16f
        val happyEyes = faceStyle == 3
        for (s in intArrayOf(-1, 1)) {
            val ex = c.x + s * r * 0.36f
            // A thin, cheerful brow.
            drawArc(
                Palette.Ink, 200f, 140f, false,
                topLeft = Offset(ex - eyeW * 0.62f, eyeY - eyeH * 1.05f),
                size = Size(eyeW * 1.24f, eyeH * 0.62f),
                style = Stroke(width = 2.6f * dpUnit, cap = StrokeCap.Round)
            )
            if (happyEyes) {
                // Closed, laughing eyes — a happy "^ ^".
                drawArc(
                    Palette.Ink, 200f, 140f, false,
                    topLeft = Offset(ex - eyeW * 0.55f, eyeY - eyeH * 0.30f),
                    size = Size(eyeW * 1.1f, eyeH * 0.66f),
                    style = Stroke(width = 2.8f * dpUnit, cap = StrokeCap.Round)
                )
            } else {
                // Big white eye with a dark pupil and a glint; squashes shut on a blink.
                drawOval(Color.White, Offset(ex - eyeW / 2f, eyeY - eyeH * blink / 2f), Size(eyeW, eyeH * blink))
                if (blink > 0.5f) {
                    drawOval(Palette.Ink, Offset(ex - eyeW * 0.26f, eyeY - eyeH * 0.12f), Size(eyeW * 0.52f, eyeH * 0.58f))
                    drawCircle(Color.White, eyeW * 0.11f, Offset(ex - eyeW * 0.08f, eyeY - eyeH * 0.02f))
                }
            }
        }
        when (faceStyle) {
            1 -> {
                // A gentle closed smile.
                drawArc(
                    Palette.Ink, 20f, 140f, false,
                    topLeft = Offset(c.x - r * 0.30f, c.y + ry * 0.02f),
                    size = Size(r * 0.60f, r * 0.42f),
                    style = Stroke(width = 3f * dpUnit, cap = StrokeCap.Round)
                )
            }
            2 -> {
                // A surprised little "o".
                drawOval(Palette.Ink, Offset(c.x - r * 0.14f, c.y + ry * 0.10f), Size(r * 0.28f, r * 0.34f))
            }
            else -> {
                // An open, happy grin with a tongue (styles 0 and 3).
                val mw = r * 0.66f
                val mh = r * 0.46f
                val my = c.y + ry * 0.16f
                drawArc(Palette.Ink, 0f, 180f, true, Offset(c.x - mw / 2f, my - mh * 0.5f), Size(mw, mh))
                drawArc(Color(0xFFFF8FA3), 0f, 180f, true, Offset(c.x - mw * 0.28f, my + mh * 0.02f), Size(mw * 0.56f, mh * 0.44f))
            }
        }
    }
}

/**
 * The first-run hint: a cartoon hand that taps at [target] over and over, with
 * expanding rings where the fingertip lands. [phase] is a free-running 0..1
 * value; the hand dips in on the first half of each cycle and lifts on the rest.
 *
 * Shown only for a child's very first taps in a mode — see Prefs.tutorialSeen.
 */
fun DrawScope.drawTapHand(target: Offset, phase: Float, dpUnit: Float) {
    val t = ((phase % 1f) + 1f) % 1f
    // Ease in to the tap, then ease back out.
    val press = if (t < 0.45f) {
        val k = t / 0.45f
        k * k * (3f - 2f * k)
    } else {
        val k = (t - 0.45f) / 0.55f
        1f - k * k * (3f - 2f * k)
    }

    // Where the fingertip lands, and where the hand rests between taps.
    val tip = Offset(target.x + 7f * dpUnit, target.y + 9f * dpUnit)
    val rest = Offset(tip.x + 17f * dpUnit, tip.y + 33f * dpUnit)
    val now = Offset(
        rest.x + (tip.x - rest.x) * press,
        rest.y + (tip.y - rest.y) * press,
    )

    // Rings pulse out from the point of contact.
    for (i in 0 until 2) {
        val rp = ((t * 1.7f) - i * 0.20f).coerceIn(0f, 1f)
        if (rp > 0f && rp < 1f) {
            drawCircle(
                Color.White.copy(alpha = (1f - rp) * 0.6f * press),
                (14f + 32f * rp) * dpUnit,
                tip,
                style = Stroke(width = 3f * dpUnit)
            )
        }
    }

    // The hand itself, tilted so the finger aims up at the balloon.
    rotate(degrees = -18f, pivot = now) {
        drawPointingHand(now, 9.5f * dpUnit, dpUnit)
    }
}

/**
 * A cartoon pointing hand: index finger up, the other three fingers folded into
 * a fist with knuckle creases, thumb tucked along the side, and a sleeve cuff.
 * [tip] is the fingertip; [u] scales the whole hand.
 */
private fun DrawScope.drawPointingHand(tip: Offset, u: Float, dpUnit: Float) {
    val skin = Color(0xFFFFD9AE)
    val skinDark = Color(0xFFE9B98C)
    val outline = Palette.Ink.copy(alpha = 0.8f)
    fun p(x: Float, y: Float) = Offset(tip.x + x * u, tip.y + y * u)

    // Sleeve cuff behind the wrist.
    val cuff = Path().apply {
        moveTo(p(-1.75f, 4.35f).x, p(-1.75f, 4.35f).y)
        lineTo(p(1.75f, 4.35f).x, p(1.75f, 4.35f).y)
        quadraticTo(p(1.95f, 5.55f).x, p(1.95f, 5.55f).y, p(1.55f, 5.75f).x, p(1.55f, 5.75f).y)
        lineTo(p(-1.55f, 5.75f).x, p(-1.55f, 5.75f).y)
        quadraticTo(p(-1.95f, 5.55f).x, p(-1.95f, 5.55f).y, p(-1.75f, 4.35f).x, p(-1.75f, 4.35f).y)
        close()
    }
    drawPath(cuff, Color(0xFF4D9BFF))
    drawPath(cuff, outline, style = Stroke(width = 2f * dpUnit))

    // The hand silhouette: one finger up, three folded, thumb at the left.
    val hand = Path().apply {
        moveTo(p(-0.42f, 0.60f).x, p(-0.42f, 0.60f).y)
        // Rounded fingertip.
        quadraticTo(p(-0.42f, 0f).x, p(-0.42f, 0f).y, p(0f, 0f).x, p(0f, 0f).y)
        quadraticTo(p(0.42f, 0f).x, p(0.42f, 0f).y, p(0.42f, 0.60f).x, p(0.42f, 0.60f).y)
        // Down the right side of the index finger.
        lineTo(p(0.42f, 1.75f).x, p(0.42f, 1.75f).y)
        // Over the folded middle finger.
        quadraticTo(p(0.95f, 1.80f).x, p(0.95f, 1.80f).y, p(1.68f, 2.10f).x, p(1.68f, 2.10f).y)
        // Three knuckle bumps down the right edge.
        quadraticTo(p(2.00f, 2.55f).x, p(2.00f, 2.55f).y, p(1.62f, 2.95f).x, p(1.62f, 2.95f).y)
        quadraticTo(p(2.00f, 3.25f).x, p(2.00f, 3.25f).y, p(1.62f, 3.60f).x, p(1.62f, 3.60f).y)
        quadraticTo(p(1.92f, 3.95f).x, p(1.92f, 3.95f).y, p(1.55f, 4.40f).x, p(1.55f, 4.40f).y)
        // Wrist.
        lineTo(p(-1.55f, 4.40f).x, p(-1.55f, 4.40f).y)
        // Thumb tucked along the left.
        quadraticTo(p(-1.95f, 4.05f).x, p(-1.95f, 4.05f).y, p(-2.10f, 3.45f).x, p(-2.10f, 3.45f).y)
        quadraticTo(p(-2.35f, 2.80f).x, p(-2.35f, 2.80f).y, p(-1.85f, 2.55f).x, p(-1.85f, 2.55f).y)
        quadraticTo(p(-1.45f, 2.40f).x, p(-1.45f, 2.40f).y, p(-1.30f, 2.75f).x, p(-1.30f, 2.75f).y)
        // Back up the palm to the base of the finger.
        quadraticTo(p(-1.15f, 2.15f).x, p(-1.15f, 2.15f).y, p(-0.90f, 1.95f).x, p(-0.90f, 1.95f).y)
        quadraticTo(p(-0.60f, 1.85f).x, p(-0.60f, 1.85f).y, p(-0.42f, 1.75f).x, p(-0.42f, 1.75f).y)
        close()
    }
    drawPath(hand, skin)
    drawPath(hand, outline, style = Stroke(width = 2.2f * dpUnit))

    // Creases where the three folded fingers meet the palm.
    for (k in 0 until 3) {
        val y = 2.45f + k * 0.62f
        drawLine(
            skinDark,
            p(0.55f, y), p(1.35f, y + 0.06f),
            strokeWidth = 1.8f * dpUnit, cap = StrokeCap.Round
        )
    }
    // Thumb crease.
    drawLine(
        skinDark,
        p(-1.28f, 2.95f), p(-0.75f, 3.30f),
        strokeWidth = 1.8f * dpUnit, cap = StrokeCap.Round
    )
    // The knuckle line at the base of the pointing finger.
    drawLine(
        skinDark,
        p(-0.35f, 1.85f), p(0.35f, 1.85f),
        strokeWidth = 1.6f * dpUnit, cap = StrokeCap.Round
    )
}

/**
 * The app title with every letter bouncing in a gentle wave — the whole word
 * feels alive without being distracting.
 */
@Composable
fun BouncyTitle(text: String = "Balloon Pop & Learn", fontSize: Int = 46) {
    val t = rememberInfiniteTransition(label = "title")
    val v by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "titleWave"
    )
    Row {
        text.forEachIndexed { i, ch ->
            if (ch == ' ') {
                Spacer(Modifier.width(10.dp))
            } else {
                Text(
                    text = ch.toString(),
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    style = TextStyle(
                        shadow = Shadow(Palette.Ink.copy(alpha = 0.4f), Offset(0f, 5f), 8f)
                    ),
                    modifier = Modifier.graphicsLayer {
                        translationY = sin(v * 6.2832f + i * 0.55f) * -5.dp.toPx()
                    }
                )
            }
        }
    }
}
