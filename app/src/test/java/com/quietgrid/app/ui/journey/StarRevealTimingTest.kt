package com.quietgrid.app.ui.journey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StarRevealTimingTest {
    private fun start(revealIndex: Int) = (GENTLE_POP_START_MS + revealIndex * GENTLE_POP_STAGGER_MS).toLong()

    @Test
    fun `reveal order is middle, left, right`() {
        assertEquals(listOf(1, 0, 2), STAR_REVEAL_SLOTS)
    }

    @Test
    fun `star is hidden before its start`() {
        assertEquals(StarFrame(0f, 0f), gentlePopFrame(0, 0))
        assertEquals(StarFrame(0f, 0f), gentlePopFrame(start(1) - 1, 1))
    }

    @Test
    fun `star overshoots then settles at 1`() {
        val peak = (0..GENTLE_POP_DURATION_MS).maxOf { gentlePopFrame(start(0) + it, 0).scale }
        assertTrue("peak $peak", peak > 1.05f && peak < 1.3f)
        assertEquals(1f, gentlePopFrame(start(0) + GENTLE_POP_DURATION_MS, 0).scale, 0.0001f)
        assertEquals(StarFrame(1f, 1f), gentlePopFrame(start(2) + 5_000, 2))
    }

    @Test
    fun `alpha is full after the first third`() {
        assertTrue(gentlePopFrame(start(0) + GENTLE_POP_DURATION_MS / 6, 0).alpha in 0.3f..0.7f)
        assertEquals(1f, gentlePopFrame(start(0) + GENTLE_POP_DURATION_MS / 3 + 1, 0).alpha, 0.0001f)
    }

    @Test
    fun `total time covers the last earned star`() {
        assertEquals(0L, gentlePopTotalMs(0))
        assertEquals(start(0) + GENTLE_POP_DURATION_MS, gentlePopTotalMs(1))
        assertEquals(start(2) + GENTLE_POP_DURATION_MS, gentlePopTotalMs(3))
    }

    @Test
    fun `goal rows brighten with earned stars, missed rows with the last one`() {
        val goals = listOf(
            JourneyGoal(JourneyGoalKind.CLEAR, true),
            JourneyGoal(JourneyGoalKind.CLEAN, false),
            JourneyGoal(JourneyGoalKind.TIME, true),
        )
        assertEquals(listOf(start(0), start(1), start(1)), goalBrightenMs(goals))
        assertEquals(listOf(0L, 0L, 0L), goalBrightenMs(goals.map { it.copy(met = false) }))
    }
}
