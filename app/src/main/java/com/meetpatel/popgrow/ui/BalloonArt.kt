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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
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
    val ry = r * 1.18f
    val top = Offset(c.x - rx, c.y - ry)
    val bodySize = Size(rx * 2f, ry * 2f)
    val knotY = c.y + ry

    // A real balloon is a teardrop: round on top, tapering to the neck.
    val body = balloonPath(c, rx, ry)

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
    translate(3f * dpUnit, 5f * dpUnit) {
        drawPath(body, Color.Black.copy(alpha = 0.10f))
    }

    clipPath(body) {
        if (rainbow) {
            val bands = Palette.Rainbow + Palette.Warm[3]
            val bw = bodySize.width / bands.size
            bands.forEachIndexed { i, col ->
                drawRect(col, Offset(top.x + i * bw, top.y), Size(bw + 1f, bodySize.height))
            }
        } else {
            // Base colour, lit from the upper left.
            drawRect(
                Brush.radialGradient(
                    colors = listOf(
                        lerp(color, Color.White, 0.52f),
                        lerp(color, Color.White, 0.12f),
                        color,
                    ),
                    center = Offset(c.x - rx * 0.34f, c.y - ry * 0.42f),
                    radius = ry * 1.45f,
                ),
                topLeft = top, size = bodySize,
            )
        }
        // The underside falls into shadow, which is what gives it volume.
        drawRect(
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, lerp(color, Color.Black, 0.34f).copy(alpha = 0.55f)),
                startY = c.y + ry * 0.05f,
                endY = c.y + ry,
            ),
            topLeft = top, size = bodySize,
        )
        // Light bouncing back up the lower-right edge — the rim light.
        drawPath(
            body,
            Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.55f)),
                start = Offset(c.x, c.y),
                end = Offset(c.x + rx, c.y + ry * 0.75f),
            ),
            style = Stroke(width = 3.2f * dpUnit),
        )
        // A broad, soft sheen down the left shoulder.
        drawOval(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.34f), Color.Transparent),
                center = Offset(c.x - rx * 0.40f, c.y - ry * 0.30f),
                radius = rx * 0.78f,
            ),
            topLeft = Offset(c.x - rx * 0.95f, c.y - ry * 0.85f),
            size = Size(rx * 1.05f, ry * 1.15f),
        )
    }

    // The neck, then the knot on top of it.
    val neck = lerp(color, Color.Black, 0.22f)
    drawPath(
        Path().apply {
            moveTo(c.x - rx * 0.11f, knotY - ry * 0.02f)
            lineTo(c.x + rx * 0.11f, knotY - ry * 0.02f)
            lineTo(c.x + rx * 0.15f, knotY + r * 0.13f)
            lineTo(c.x - rx * 0.15f, knotY + r * 0.13f)
            close()
        },
        if (rainbow) Color(0xFFE84D8A) else neck,
    )
    drawOval(
        if (rainbow) Color(0xFFD8437C) else lerp(color, Color.Black, 0.3f),
        topLeft = Offset(c.x - r * 0.10f, knotY + r * 0.06f),
        size = Size(r * 0.20f, r * 0.13f),
    )

    // Highlights last: a soft one, then the sharp catch-light.
    drawOval(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.0f)),
            center = Offset(c.x - rx * 0.42f, c.y - ry * 0.50f),
            radius = rx * 0.34f,
        ),
        topLeft = Offset(c.x - rx * 0.74f, c.y - ry * 0.80f),
        size = Size(rx * 0.62f, ry * 0.46f),
    )
    drawOval(
        Color.White.copy(alpha = 0.92f),
        topLeft = Offset(c.x - rx * 0.52f, c.y - ry * 0.60f),
        size = Size(rx * 0.20f, ry * 0.13f),
    )

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
fun DrawScope.drawTapHand(target: Offset, phase: Float, dpUnit: Float, measurer: TextMeasurer) {
    val t = ((phase % 1f) + 1f) % 1f
    // Ease in to the tap, then ease back out.
    val press = if (t < 0.45f) {
        val k = t / 0.45f
        k * k * (3f - 2f * k)
    } else {
        val k = (t - 0.45f) / 0.55f
        1f - k * k * (3f - 2f * k)
    }

    // Rings pulse out from the balloon at the moment of contact.
    for (i in 0 until 2) {
        val rp = ((t * 1.7f) - i * 0.20f).coerceIn(0f, 1f)
        if (rp > 0f && rp < 1f) {
            drawCircle(
                Color.White.copy(alpha = (1f - rp) * 0.6f * press),
                (14f + 32f * rp) * dpUnit,
                target,
                style = Stroke(width = 3f * dpUnit)
            )
        }
    }

    // The hand is the platform's own pointing-hand emoji — a professionally
    // drawn glyph every phone ships with, so it looks right on every device
    // and costs the APK nothing. It dips onto the balloon, then lifts away.
    val layout = measurer.measure(
        text = "👆",
        style = TextStyle(fontSize = 52.sp)
    )
    val lift = (1f - press) * 30f * dpUnit
    // The glyph's fingertip sits at its top, slightly left of centre.
    drawText(
        layout,
        topLeft = Offset(
            target.x - layout.size.width * 0.34f,
            target.y - 4f * dpUnit + lift,
        )
    )
}

/**
 * The app title with every letter bouncing in a gentle wave — the whole word
 * feels alive without being distracting.
 */
/**
 * The outline of a real balloon: a full round top that narrows through the
 * shoulders and pinches in to a small neck. Everything else — the shading, the
 * rim light, the knot — is drawn against this shape.
 */
fun balloonPath(c: Offset, rx: Float, ry: Float): Path = Path().apply {
    moveTo(c.x, c.y - ry)
    // Right shoulder, down to the widest point.
    cubicTo(
        c.x + rx * 0.60f, c.y - ry,
        c.x + rx, c.y - ry * 0.52f,
        c.x + rx, c.y - ry * 0.02f,
    )
    // Right side tapering in towards the neck.
    cubicTo(
        c.x + rx, c.y + ry * 0.50f,
        c.x + rx * 0.46f, c.y + ry * 0.82f,
        c.x + rx * 0.12f, c.y + ry * 0.99f,
    )
    lineTo(c.x - rx * 0.12f, c.y + ry * 0.99f)
    // Mirror back up the left side.
    cubicTo(
        c.x - rx * 0.46f, c.y + ry * 0.82f,
        c.x - rx, c.y + ry * 0.50f,
        c.x - rx, c.y - ry * 0.02f,
    )
    cubicTo(
        c.x - rx, c.y - ry * 0.52f,
        c.x - rx * 0.60f, c.y - ry,
        c.x, c.y - ry,
    )
    close()
}

/** Storybook colours for the title letters. */
private val TITLE_COLORS = listOf(
    Color(0xFFFF5252), Color(0xFFFF9F43), Color(0xFFFFD32A),
    Color(0xFF35C978), Color(0xFF4D9BFF), Color(0xFFA98BF0), Color(0xFFFF7FB6),
)

@Composable
fun BouncyTitle(text: String = "Bubble Learn Kids", fontSize: Int = 46) {
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
                // Every letter takes a colour from the rainbow and rides the
                // wave — playful and storybook-ish rather than plain and flat.
                Text(
                    text = ch.toString(),
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Cursive,
                    color = TITLE_COLORS[i % TITLE_COLORS.size],
                    style = TextStyle(
                        shadow = Shadow(Palette.Ink.copy(alpha = 0.55f), Offset(0f, 4f), 7f)
                    ),
                    modifier = Modifier.graphicsLayer {
                        translationY = sin(v * 6.2832f + i * 0.55f) * -6.dp.toPx()
                        rotationZ = sin(v * 6.2832f + i * 0.55f) * 5f
                    }
                )
            }
        }
    }
}
