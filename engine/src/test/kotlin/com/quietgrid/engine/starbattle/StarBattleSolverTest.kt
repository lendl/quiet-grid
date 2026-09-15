package com.quietgrid.engine.starbattle

import org.junit.Assert.assertTrue
import org.junit.Test

class StarBattleSolverTest {
    @Test
    fun `solves a trivial k=1 5x5 puzzle by forced placement and confinement alone`() {
        val regions = listOf(
            listOf(4, 1, 1, 2, 2),
            listOf(4, 1, 1, 0, 0),
            listOf(3, 3, 1, 0, 0),
            listOf(3, 3, 3, 0, 0),
            listOf(3, 3, 3, 0, 0),
        )
        val result = solveStarBattle(5, 1, regions)
        assertTrue(result.solved)
        assertTrue(result.steps.none { it.technique == StarBattleTechnique.CHAIN })
    }

    @Test
    fun `chain contradiction can resolve a puzzle forced placement and confinement alone cannot`() {
        val regions = listOf(
            listOf(3, 3, 3, 3, 0),
            listOf(3, 4, 4, 4, 0),
            listOf(3, 4, 4, 4, 0),
            listOf(3, 1, 2, 0, 0),
            listOf(1, 1, 2, 0, 0),
        )
        val result = solveStarBattle(5, 1, regions)
        assertTrue(result.solved)
        assertTrue(result.steps.any { it.technique == StarBattleTechnique.CHAIN })
    }
}
