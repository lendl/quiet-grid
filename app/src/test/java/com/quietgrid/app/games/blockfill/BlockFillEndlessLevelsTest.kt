package com.quietgrid.app.games.blockfill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockFillEndlessLevelsTest {

    @Test
    fun `levels never get easier`() {
        val levels = BLOCKFILL_ENDLESS_LEVELS
        for (index in 1 until levels.size) {
            val previous = levels[index - 1].rules
            val current = levels[index].rules
            assertTrue("candidates drop at x${index + 1}", current.candidateCount >= previous.candidateCount)
            assertTrue("guarantee loosens at x${index + 1}", current.guarantee.placeableCount <= previous.guarantee.placeableCount)
        }
    }

    @Test
    fun `every level differs from the one before`() {
        val levels = BLOCKFILL_ENDLESS_LEVELS
        assertEquals(10, levels.size)
        for (index in 1 until levels.size) {
            val previous = levels[index - 1].rules
            val current = levels[index].rules
            val differs = current.weights != previous.weights ||
                current.candidateCount != previous.candidateCount ||
                current.guarantee != previous.guarantee
            assertTrue("x${index + 1} plays the same as x$index", differs)
        }
    }

    @Test
    fun `endlessLevelFor clamps`() {
        assertEquals(BLOCKFILL_ENDLESS_LEVELS[9], endlessLevelFor(15))
        assertEquals(BLOCKFILL_ENDLESS_LEVELS[0], endlessLevelFor(0))
        assertEquals(BLOCKFILL_ENDLESS_LEVELS[4], endlessLevelFor(5))
    }

    @Test
    fun `steer chance ramps after the calm window`() {
        val first = endlessLevelFor(1)
        assertEquals(0, endlessSteerChancePercent(17, first))
        assertEquals(10, endlessSteerChancePercent(18, first))
        assertEquals(20, endlessSteerChancePercent(21, first))
        assertEquals(80, endlessSteerChancePercent(400, first))
        assertEquals(6, endlessSteerChancePercent(18, endlessLevelFor(7)))
    }
}
