package com.quietgrid.engine.starbattle

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class StarBattleDifficultyTest {
    @Test
    fun `unsolved result classifies as null`() {
        val result = StarBattleSolveResult(solved = false, steps = emptyList())
        assertNull(classifyStarBattleK2Grade(result))
    }

    @Test
    fun `a solve with no chain steps at all classifies as null (not medium-eligible by forced placement alone)`() {
        val result = StarBattleSolveResult(
            solved = true,
            steps = listOf(StarBattleSolveStep(StarBattleTechnique.FORCED_PLACEMENT, 0)),
        )
        assertNull(classifyStarBattleK2Grade(result))
    }

    @Test
    fun `a single shallow chain step clears medium grade`() {
        val result = StarBattleSolveResult(
            solved = true,
            steps = listOf(StarBattleSolveStep(StarBattleTechnique.CHAIN, 1)),
        )
        assertEquals("medium", classifyStarBattleK2Grade(result))
    }

    @Test
    fun `a deep chain step clears expert grade`() {
        val result = StarBattleSolveResult(
            solved = true,
            steps = listOf(StarBattleSolveStep(StarBattleTechnique.CHAIN, 6)),
        )
        assertEquals("expert", classifyStarBattleK2Grade(result))
    }
}
