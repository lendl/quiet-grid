package com.quietgrid.engine.starbattle

import org.junit.Assert.assertEquals
import org.junit.Test

class StarBattleBruteForceSolverTest {
    @Test
    fun `counts exactly one solution for a puzzle known to be unique`() {
        val regions = listOf(
            listOf(2, 2, 2, 1),
            listOf(2, 2, 1, 1),
            listOf(3, 0, 1, 1),
            listOf(3, 0, 0, 1),
        )
        assertEquals(1, countStarBattleSolutions(4, 1, regions))
    }

    @Test
    fun `caps the count instead of enumerating every solution of a wide-open grid`() {
        val regions = listOf(
            listOf(0, 0, 0, 0, 0),
            listOf(1, 1, 1, 1, 1),
            listOf(2, 2, 2, 2, 2),
            listOf(3, 3, 3, 3, 3),
            listOf(4, 4, 4, 4, 4),
        )
        assertEquals(14, countStarBattleSolutions(5, 1, regions, cap = 1000))
        assertEquals(2, countStarBattleSolutions(5, 1, regions, cap = 2))
    }

    @Test
    fun `agrees with the technique solver on every puzzle the technique solver solves`() {
        val regions = listOf(
            listOf(4, 1, 1, 2, 2),
            listOf(4, 1, 1, 0, 0),
            listOf(3, 3, 1, 0, 0),
            listOf(3, 3, 3, 0, 0),
            listOf(3, 3, 3, 0, 0),
        )
        val techniqueResult = solveStarBattle(5, 1, regions)
        if (techniqueResult.solved) {
            assertEquals(1, countStarBattleSolutions(5, 1, regions))
        }
    }
}
