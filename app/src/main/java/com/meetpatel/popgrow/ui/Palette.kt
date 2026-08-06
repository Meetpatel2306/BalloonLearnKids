package com.meetpatel.popgrow.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Two colour families, one per player.
 *
 * They are deliberately far apart in hue (warm vs cool) so a three-year-old can
 * tell "my bubbles" from "your bubbles" at a glance, without any label, number
 * or instruction. Every colour is high-chroma but light enough that white
 * highlights and dark outlines both read on top of it.
 */
object Palette {

    /** Player 1 — warm: strawberry, tangerine, sunflower, bubblegum, cherry. */
    val Warm = listOf(
        Color(0xFFFF6B81),
        Color(0xFFFF9F43),
        Color(0xFFFFD32A),
        Color(0xFFFF7FB6),
        Color(0xFFFF5252),
    )

    /** Player 2 — cool: sky, mint, lavender, teal, blueberry. */
    val Cool = listOf(
        Color(0xFF54C7FC),
        Color(0xFF3DDC97),
        Color(0xFFA98BF0),
        Color(0xFF2BD9D9),
        Color(0xFF5C8DFF),
    )

    fun forLane(lane: Int, twoPlayer: Boolean): List<Color> =
        if (!twoPlayer) Warm + Cool else if (lane == 0) Warm else Cool

    // ------------------------------------------------------------------ world

    /**
     * The sky drifts slowly through four moods on a two-minute loop: morning,
     * midday, golden hour, dusk. Nothing about it is interactive — it is there so
     * a long session never looks like the same screen twice.
     */
    val SkyTops = listOf(
        Color(0xFF8ED8F8),
        Color(0xFF6EC6F5),
        Color(0xFFFFB067),
        Color(0xFF8E7BD8),
    )
    val SkyBottoms = listOf(
        Color(0xFFDFF6FF),
        Color(0xFFC6ECFB),
        Color(0xFFFFE2B8),
        Color(0xFFD7C9F5),
    )

    val HillFar = Color(0xFF7FCf8A)
    val HillNear = Color(0xFF57BE6B)
    val Grass = Color(0xFF3EA855)
    val GrassDark = Color(0xFF2F8E45)
    val Stem = Color(0xFF2F8E45)
    val Leaf = Color(0xFF4FBF63)
    val Cloud = Color(0xFFFFFFFF)
    val Sun = Color(0xFFFFF0A8)

    val FlowerCenters = listOf(
        Color(0xFFFFF3C4),
        Color(0xFFFFE066),
        Color(0xFFFFFFFF),
    )

    val Ink = Color(0xFF2C3A57)
    val InkSoft = Color(0xFF5A6B8C)

    // -------------------------------------------------------------------- themes

    /** A whole scene palette. Each level wears a different one, so levelling up
     * visibly changes the world — sky, hills and ground all at once. */
    data class SkyTheme(
        val skyTop: Color,
        val skyBottom: Color,
        val hillFar: Color,
        val hillNear: Color,
        val grass: Color,
        val grassDark: Color,
        val night: Boolean = false,
        val sun: Color = Sun,
        val cloudAlpha: Float = 0.82f,
    )

    /** Levels cycle through these in order, then wrap around. */
    val Themes = listOf(
        // Meadow — the classic bright day.
        SkyTheme(Color(0xFF8ED8F8), Color(0xFFDFF6FF), Color(0xFF7FCF8A), Color(0xFF57BE6B), Color(0xFF3EA855), Color(0xFF2F8E45)),
        // Sunset — warm golden hour.
        SkyTheme(Color(0xFFFFB067), Color(0xFFFFE2B8), Color(0xFFC58BC0), Color(0xFF9E6FA8), Color(0xFF6E8E5A), Color(0xFF56744A), sun = Color(0xFFFFE0A0)),
        // Night — deep blue with a moon and stars.
        SkyTheme(Color(0xFF2A2A5E), Color(0xFF4B4B82), Color(0xFF2E3B5E), Color(0xFF283452), Color(0xFF2E7D46), Color(0xFF1F5E33), night = true, cloudAlpha = 0.35f),
        // Candy — pastel pinks.
        SkyTheme(Color(0xFFFFC3E1), Color(0xFFFFE6F3), Color(0xFFF3A6D0), Color(0xFFE87FBE), Color(0xFF8ED6A0), Color(0xFF6FC488)),
        // Ocean — cool water over a sandy floor.
        SkyTheme(Color(0xFF3FA9C9), Color(0xFFAEE6EF), Color(0xFF2C8FB0), Color(0xFF1F7C9A), Color(0xFFE7D9A6), Color(0xFFD3C48C)),
        // Space — a starry deep-purple void.
        SkyTheme(Color(0xFF1B1440), Color(0xFF3A2A6B), Color(0xFF241A52), Color(0xFF2E2160), Color(0xFF3A2E6B), Color(0xFF241A52), night = true, cloudAlpha = 0.18f),
    )

    fun themeFor(level: Int): SkyTheme = Themes[((level % Themes.size) + Themes.size) % Themes.size]

    /** Smoothly blend two scenes, for the background that drifts through colours
     * on its own over time. */
    fun blendTheme(a: SkyTheme, b: SkyTheme, f: Float): SkyTheme = SkyTheme(
        skyTop = lerp(a.skyTop, b.skyTop, f),
        skyBottom = lerp(a.skyBottom, b.skyBottom, f),
        hillFar = lerp(a.hillFar, b.hillFar, f),
        hillNear = lerp(a.hillNear, b.hillNear, f),
        grass = lerp(a.grass, b.grass, f),
        grassDark = lerp(a.grassDark, b.grassDark, f),
        night = if (f < 0.5f) a.night else b.night,
        sun = lerp(a.sun, b.sun, f),
        cloudAlpha = a.cloudAlpha + (b.cloudAlpha - a.cloudAlpha) * f,
    )

    /** The scene at a given time — the whole world drifts through the themes,
     * changing colour continuously so the background is always alive. */
    fun themeAt(time: Float): SkyTheme {
        val pos = time / THEME_SECONDS
        val i = pos.toInt()
        val f = pos - i
        return blendTheme(Themes[i % Themes.size], Themes[(i + 1) % Themes.size], f)
    }

    private const val THEME_SECONDS = 14f

    // --------------------------------------------------------------- celebration

    /** Bright, cheerful flecks thrown on every pop, mixed with the bubble's own
     * colour so the confetti always feels festive rather than monochrome. */
    val Confetti = listOf(
        Color(0xFFFF6B81),
        Color(0xFFFFC048),
        Color(0xFF3DDC97),
        Color(0xFF54C7FC),
        Color(0xFFA98BF0),
        Color(0xFFFF7FB6),
        Color(0xFFFFFFFF),
    )

    /** Six-band rainbow — the special bubble, its confetti, and the sky arc. */
    val Rainbow = listOf(
        Color(0xFFFF5D5D),
        Color(0xFFFF9F43),
        Color(0xFFFFD32A),
        Color(0xFF3DDC97),
        Color(0xFF54C7FC),
        Color(0xFFA98BF0),
    )

    /** Golden twinkles that rise from a celebration and dot the dusk sky. */
    val Sparkle = listOf(
        Color(0xFFFFF3B0),
        Color(0xFFFFE066),
        Color(0xFFFFFFFF),
    )

    /** Star and gold accents for the star-shaped bubble. */
    val Gold = Color(0xFFFFD54A)

    /** Soft, low-chroma flecks that drift across the sky — petals and leaves.
     * Kept pale so they read as calm background, never competing with a bubble. */
    val Petal = listOf(
        Color(0xFFFFC1D6),
        Color(0xFFFFE0EC),
        Color(0xFFFFFFFF),
        Color(0xFFC6EFCB),
        Color(0xFFFFF1C2),
    )

    // Visitor body colours, so a bee looks like a bee and a ladybug like a ladybug.
    val Bee = Color(0xFFFFC931)
    val BeeStripe = Color(0xFF3A3320)
    val Ladybug = Color(0xFFF0463E)
    val Bird = Color(0xFF5FB0FF)
    val Twinkle = Color(0xFFFFFFFF)

    /** The night visitors: a warm moon face and glowing fireflies at dusk. */
    val Moon = Color(0xFFFFF6D8)
    val MoonFace = Color(0xFF9A8B6A)
    val SunFace = Color(0xFFCC7A2B)
    val Cheek = Color(0xFFFF9AA8)
    val Firefly = Color(0xFFFFF19A)

    // Ground animals that stroll along the grass.
    val Puppy = Color(0xFFC98A5E)
    val Bunny = Color(0xFFF3EFEA)
    val CatColors = listOf(
        Color(0xFFEFA65B),
        Color(0xFF9AA0A6),
        Color(0xFF5B5B5B),
    )
}
