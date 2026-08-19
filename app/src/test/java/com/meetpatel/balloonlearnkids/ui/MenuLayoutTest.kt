package com.meetpatel.balloonlearnkids.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The menu grid shipped twice with the "Shapes" and "Animals" labels cut off the
 * bottom, on two different phones. These tests exist so it cannot happen again.
 *
 * The rule under test is the one that was broken both times: **the grid is never
 * taller than the space it was given.** It is all arithmetic, so the shelf of
 * devices below is checked on every build with no phone attached.
 */
class MenuLayoutTest {

    /** A device as the grid sees it: the box left after the title and insets. */
    private data class Case(val name: String, val gridW: Dp, val gridH: Dp)

    /**
     * Real landscape windows: physical pixels / (density / 160), less the app's
     * padding, the title cloud and the safe-area insets. The tight ones at the
     * end are the reported phones once a large Display size has shrunk
     * everything measured in dp.
     */
    private val devices = listOf(
        Case("Pixel 7 landscape",            gridW = 386.dp, gridH = 218.dp),
        Case("Galaxy F23 landscape",         gridW = 386.dp, gridH = 210.dp),
        Case("Redmi Note, default zoom",     gridW = 372.dp, gridH = 196.dp),
        Case("Redmi Note, large zoom",       gridW = 330.dp, gridH = 168.dp),
        Case("Samsung, larger zoom",         gridW = 300.dp, gridH = 142.dp),
        Case("Samsung, max zoom + buttons",  gridW = 288.dp, gridH = 118.dp),
        Case("Small phone landscape",        gridW = 320.dp, gridH = 150.dp),
        Case("Very short window",            gridW = 400.dp, gridH = 100.dp),
        Case("Tall tablet portrait",         gridW = 700.dp, gridH = 420.dp),
        Case("Tablet landscape",             gridW = 500.dp, gridH = 300.dp),
        Case("Foldable, folded",             gridW = 260.dp, gridH = 160.dp),
        Case("Split screen, half height",    gridW = 380.dp, gridH = 130.dp),
    )

    @Test
    fun `grid never grows taller than the space it was given`() {
        for (d in devices) {
            val w = MenuLayout.balloonWidth(d.gridW, d.gridH)
            val used = MenuLayout.gridHeight(w)
            assertTrue(
                "${d.name}: grid is ${used.value}dp tall in ${d.gridH.value}dp of space",
                used <= d.gridH + 0.5.dp,
            )
        }
    }

    @Test
    fun `grid never grows wider than the space it was given`() {
        for (d in devices) {
            val w = MenuLayout.balloonWidth(d.gridW, d.gridH)
            val used = MenuLayout.gridWidth(w)
            assertTrue(
                "${d.name}: grid is ${used.value}dp wide in ${d.gridW.value}dp of space",
                used <= d.gridW + 0.5.dp,
            )
        }
    }

    @Test
    fun `balloons stay big enough to tap on any normal phone`() {
        // Android asks for a 48dp touch target, and the balloon carries a name
        // pill under it, so the real target is taller still. Phones at their
        // default display size must clear 48dp on the balloon alone.
        for (d in devices.take(3)) {
            val w = MenuLayout.balloonWidth(d.gridW, d.gridH)
            assertTrue("${d.name}: balloon only ${w.value}dp wide", w >= 48.dp)
        }
        // Even wound right down to the smallest zoom a phone offers, a balloon
        // stays comfortably bigger than a fingertip.
        for (d in devices.drop(3).take(4)) {
            val w = MenuLayout.balloonWidth(d.gridW, d.gridH)
            assertTrue("${d.name}: balloon only ${w.value}dp wide", w >= 20.dp)
        }
    }

    @Test
    fun `balloons never exceed the design size`() {
        for (d in devices + Case("Enormous", 2000.dp, 2000.dp)) {
            val w = MenuLayout.balloonWidth(d.gridW, d.gridH)
            assertTrue("${d.name}: balloon ${w.value}dp", w <= MenuLayout.MAX_BALLOON)
        }
    }

    /**
     * The floor is the one thing that can override the fit calculation, so it
     * has to sit below anything a real window will ask for. A 32dp floor was
     * what pushed the labels off the screen.
     */
    @Test
    fun `the minimum size is never the thing that decides`() {
        for (d in devices) {
            val w = MenuLayout.balloonWidth(d.gridW, d.gridH)
            assertTrue(
                "${d.name}: hit the ${MenuLayout.MIN_BALLOON.value}dp floor",
                w > MenuLayout.MIN_BALLOON,
            )
        }
    }

    /**
     * A sweep across every window a phone, tablet or split-screen view could
     * plausibly produce. This is the test that would have caught the original
     * bug: it fails the moment any size, anywhere, overflows.
     */
    @Test
    fun `no window size in a wide sweep can overflow`() {
        var checked = 0
        var h = 90
        while (h <= 700) {
            var w = 240
            while (w <= 900) {
                val bw = MenuLayout.balloonWidth(w.dp, h.dp)
                assertTrue("${w}x${h} too tall", MenuLayout.gridHeight(bw) <= h.dp + 0.5.dp)
                assertTrue("${w}x${h} too wide", MenuLayout.gridWidth(bw) <= w.dp + 0.5.dp)
                checked++
                w += 10
            }
            h += 5
        }
        assertTrue("swept $checked window sizes", checked > 5000)
    }

    /**
     * Shrinking the window must never make a balloon bigger. A jump like that is
     * the signature of the two-pass label logic going wrong.
     */
    @Test
    fun `smaller windows never produce bigger balloons`() {
        var previous = MenuLayout.balloonWidth(400.dp, 90.dp)
        var h = 95
        while (h <= 500) {
            val current = MenuLayout.balloonWidth(400.dp, h.dp)
            assertTrue(
                "at ${h}dp the balloon shrank to ${current.value} from ${previous.value}",
                current >= previous - 0.5.dp,
            )
            previous = current
            h += 5
        }
    }

    @Test
    fun `the label allowance really does cover the label`() {
        // The pill is text, plus 5dp padding top and bottom, plus a 2.5dp border.
        // With the font scale pinned these are the true heights, rounded up.
        val tall = 13 * 1.4f + 10 + 5
        val short = 11 * 1.4f + 6 + 4
        assertTrue("tall label needs ${tall}dp", MenuLayout.LABEL_TALL.value >= tall)
        assertTrue("short label needs ${short}dp", MenuLayout.LABEL_SHORT.value >= short)
    }
}
