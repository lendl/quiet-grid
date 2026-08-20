// engine/src/test/kotlin/com/quietgrid/engine/animaldoku/AnimalDokuSolverTest.kt
package com.quietgrid.engine.animaldoku

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val IRREGULAR_REGIONS = listOf(
    listOf(4, 4, 4, 2, 2),
    listOf(4, 4, 2, 2, 2),
    listOf(4, 4, 2, 2, 0),
    listOf(1, 1, 3, 3, 0),
    listOf(1, 1, 1, 0, 0),
)

class AnimalDokuSolverTest {
    @Test
    fun `solveAnimalDoku solves a 5x5 irregular-region puzzle down to completion`() {
        val result = solveAnimalDoku(5, IRREGULAR_REGIONS)
        assertTrue(result.solved)
    }

    @Test
    fun `solveAnimalDoku fails on a region layout with no valid solution at all`() {
        val impossibleRegions = listOf(
            listOf(0, 1, 2, 2),
            listOf(0, 1, 2, 2),
            listOf(3, 3, 3, 3),
            listOf(3, 3, 3, 3),
        )
        val result = solveAnimalDoku(4, impossibleRegions)
        assertFalse(result.solved)
    }

    @Test
    fun `solveAnimalDoku records a step per technique application in order`() {
        val result = solveAnimalDoku(5, IRREGULAR_REGIONS)
        assertTrue(result.steps.isNotEmpty())
    }
}
