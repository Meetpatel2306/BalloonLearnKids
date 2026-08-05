package com.meetpatel.popgrow.ui

import androidx.compose.ui.graphics.Color

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
}
