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
