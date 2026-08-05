package com.meetpatel.popgrow.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * These tests encode the design rules that make the game safe for a two-year-old.
 * If one of them fails, the game has stopped being age-appropriate — not just
 * stopped working.
 */
class GameWorldTest {

    /** A typical phone in landscape at 2x density. */
    private fun world(twoPlayer: Boolean = false, seed: Int = 7) =
        GameWorld(twoPlayer, density = 2f, random = Random(seed)).apply {
            resize(1600f, 800f)
        }

    private fun GameWorld.settle(seconds: Float = 6f) {
        var t = 0f
        while (t < seconds) {
            update(1f / 60f)
            t += 1f / 60f
        }
    }

    @Test
    fun `bubbles spawn up to the target count`() {
        val w = world()
        w.settle()
        assertEquals(9, w.bubbles.size)
    }

    @Test
    fun `two player mode fills both lanes`() {
        val w = world(twoPlayer = true)
        w.settle()
        assertEquals(6, w.bubbles.count { it.lane == 0 })
        assertEquals(6, w.bubbles.count { it.lane == 1 })
    }

    @Test
    fun `two player bubbles never cross the divider`() {
        val w = world(twoPlayer = true)
        var t = 0f
        while (t < 30f) {
            w.update(1f / 60f)
            for (b in w.bubbles) {
                val left = w.laneStart(b.lane)
                val right = w.laneEnd(b.lane)
                assertTrue(
                    "lane ${b.lane} bubble at ${b.x} escaped [$left, $right]",
                    b.x >= left - 1f && b.x <= right + 1f
                )
            }
            t += 1f / 60f
        }
    }

    @Test
    fun `every bubble is at least a 2cm touch target`() {
        val w = world()
        w.settle(20f)
        // 160 dp = 1 inch, so 2 cm ~= 126 dp -> a 63 dp radius. The tap slop counts
        // towards the target, which is what the child actually has to hit.
        val minTargetPx = 63f * 2f
        for (b in w.bubbles) {
            val effectiveRadius = b.radius + 20f * 2f
            assertTrue(
                "radius ${b.radius}px + slop is under a 2cm target",
                effectiveRadius >= minTargetPx
            )
        }
    }

    @Test
    fun `tapping empty sky does nothing at all`() {
        val w = world()
        w.settle()
        val before = w.bubbles.size
        val result = w.popAt(5f, 5f)
        assertNull(result)
        assertEquals(before, w.bubbles.size)
        assertEquals(0, w.flowers.size)
    }

    @Test
    fun `a pop removes the bubble, plants a flower and returns a note`() {
        val w = world()
        w.settle()
        val target = w.bubbles.first()
        val pop = w.popAt(target.x, target.y)

        assertNotNull(pop)
        assertTrue(w.bubbles.none { it.id == target.id })
        assertEquals(1, w.flowers.size)
        assertTrue(pop!!.noteIndex in 0..9)
        assertTrue(pop.loudness in 0f..1f)
        assertTrue(w.particles.isNotEmpty())
    }

    @Test
    fun `a near miss still pops, thanks to tap slop`() {
        val w = world()
        w.settle()
        val target = w.bubbles.first()
        // 15 dp outside the bubble edge: a genuine miss for an adult, a hit here.
        val pop = w.popAt(target.x + target.radius + 15f * 2f, target.y)
        assertNotNull(pop)
    }

    @Test
    fun `bigger bubbles play lower notes`() {
        val w = world()
        w.settle(20f)
        val sorted = w.bubbles.sortedBy { it.radius }
        if (sorted.size >= 2) {
            val smallest = sorted.first()
            val largest = sorted.last()
            assertTrue(
                "small=${smallest.noteIndex} large=${largest.noteIndex}",
                smallest.noteIndex >= largest.noteIndex
            )
        }
    }

    @Test
    fun `an escaping bubble is replaced, never punished`() {
        val w = world()
        w.settle()
        val escapee = w.bubbles.first()
        escapee.y = -escapee.radius - 1f
        w.update(1f / 60f)
        assertTrue(w.bubbles.none { it.id == escapee.id })
        w.settle(4f)
        assertEquals("the sky must refill itself", 9, w.bubbles.size)
    }

    @Test
    fun `the garden stops growing instead of overflowing`() {
        val w = world()
        repeat(60) {
            w.settle(1f)
            w.bubbles.firstOrNull()?.let { w.popAt(it.x, it.y) }
        }
        val living = w.flowers.count { it.fadeStartedAt < 0f }
        assertTrue("garden held $living flowers", living <= 25)
    }

    @Test
    fun `a butterfly arrives every ten flowers`() {
        val w = world()
        var butterflies = 0
        repeat(10) {
            w.settle(1f)
            val b = w.bubbles.firstOrNull() ?: return@repeat
            if (w.popAt(b.x, b.y)?.butterfly == true) butterflies++
        }
        assertEquals(1, butterflies)
        assertTrue(w.butterflies.isNotEmpty())
    }

    @Test
    fun `a long pause cannot teleport the world`() {
        val w = world()
        w.settle()
        val before = w.bubbles.map { it.y }
        // Simulate coming back from the background after five minutes.
        w.update(300f)
        val moved = before.zip(w.bubbles.map { it.y }).maxOf { (a, b) -> a - b }
        assertTrue("bubbles jumped ${moved}px in one frame", moved < 20f)
    }

    @Test
    fun `flowers land inside their own lane`() {
        val w = world(twoPlayer = true)
        repeat(20) {
            w.settle(0.6f)
            w.bubbles.firstOrNull()?.let { w.popAt(it.x, it.y) }
        }
        for (f in w.flowers) {
            assertTrue(f.x >= w.laneStart(f.lane) && f.x <= w.laneEnd(f.lane))
        }
    }
}
