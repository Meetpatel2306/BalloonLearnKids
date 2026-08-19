package com.meetpatel.balloonlearnkids.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The arithmetic behind the five game balloons on the menu.
 *
 * This lives on its own, free of Compose, for one reason: two phones shipped
 * with the "Shapes" and "Animals" labels sliced off the bottom of the screen,
 * and both times the cause was a sizing rule that looked right and was not.
 * As plain functions the rules can be tested against a whole shelf of devices
 * without a phone in the room — see MenuLayoutTest.
 *
 * The contract is simply: **the grid must never be taller than the box it was
 * given.** Everything else is a preference.
 */
object MenuLayout {

    /** How much taller a balloon is than it is wide. */
    const val BALLOON_RATIO = 1.26f

    /** The gap between balloons, and between the two rows. */
    val GAP = 6.dp

    /** Height of a name pill at the normal 13sp, rounded up. */
    val LABEL_TALL = 36.dp

    /** …and at the 11sp used once the balloons get small. */
    val LABEL_SHORT = 28.dp

    /** Below this width a balloon switches to the smaller label. */
    val SMALL_BELOW = 68.dp

    /** A balloon is never wider than this, however much room there is. */
    val MAX_BALLOON = 92.dp

    /**
     * The smallest a balloon may be. This is deliberately tiny: any floor at all
     * overrides the fit calculation, so it has to sit below anything a real
     * screen will ask for. A 32dp floor was what pushed the labels off the
     * bottom on a short screen.
     */
    val MIN_BALLOON = 10.dp

    /** True once the balloons are small enough to wear the shorter label. */
    fun usesShortLabel(balloonW: Dp): Boolean = balloonW < SMALL_BELOW

    /** The label height that goes with a given balloon width. */
    fun labelFor(balloonW: Dp): Dp = if (usesShortLabel(balloonW)) LABEL_SHORT else LABEL_TALL

    /**
     * The width to draw each balloon at, given the box the grid was handed.
     * Two rows of balloons plus two labels plus one gap must fit inside
     * [maxHeight]; three balloons plus two gaps must fit inside [maxWidth].
     */
    fun balloonWidth(maxWidth: Dp, maxHeight: Dp): Dp {
        fun fitFor(labelH: Dp): Dp {
            val byHeight = (maxHeight - labelH * 2 - GAP) / 2 / BALLOON_RATIO
            val byWidth = (maxWidth - GAP * 2) / 3
            return minOf(byHeight, byWidth)
        }
        // Assume full-size labels first. If that already forces small balloons,
        // the labels shrink too, which frees a little height for a second pass.
        //
        // The second pass has to stay in the range it assumed: if the extra room
        // pushed the balloon back up to full-label size, the grid would be
        // measured with the short label and drawn with the tall one, and the
        // difference is exactly the sliver that used to fall off the screen.
        var w = fitFor(LABEL_TALL)
        if (w < SMALL_BELOW) {
            w = minOf(fitFor(LABEL_SHORT), SMALL_BELOW - 0.5.dp)
        }
        return w.coerceIn(MIN_BALLOON, MAX_BALLOON)
    }

    /** How tall the finished grid actually is at [balloonW]. */
    fun gridHeight(balloonW: Dp): Dp =
        balloonW * BALLOON_RATIO * 2 + labelFor(balloonW) * 2 + GAP

    /** How wide the finished grid actually is. */
    fun gridWidth(balloonW: Dp): Dp = balloonW * 3 + GAP * 2
}
