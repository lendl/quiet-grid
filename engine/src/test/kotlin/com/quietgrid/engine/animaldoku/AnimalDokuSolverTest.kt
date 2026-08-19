// engine/src/test/kotlin/com/quietgrid/engine/animaldoku/AnimalDokuSolverTest.kt
package com.quietgrid.engine.animaldoku

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 5x5 grid with an irregular region layout that has exactly one valid solution
 * (row -> col): (3, 1, 4, 2, 0). Verified by brute-force enumeration over every row/column
 * permutation under the region and king-adjacency (no two placements orthogonally or
 * diagonally adjacent) constraints -- exactly one permutation satisfies both.
 *
 * The originally drafted fixture for this test was the classic 4x4 quadrant-region layout
 * (four 2x2 blocks), reused from AnimalDokuTechniquesTest/AnimalDokuSolverStateTest. Brute-force
 * enumeration shows that layout actually has *two* valid solutions -- (1,3,0,2) and (2,0,3,1) --
 * related by a top/bottom row-reversal symmetry the quadrant partition doesn't break. Since sound
 * techniques can never eliminate a cell that's part of a genuine valid solution, no combination of
 * singleton/confinement/pairing/chain can ever converge that layout to a unique placement; it was
 * replaced with this irregular one instead.
 */
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
        // Regions 0, 1, and 2 are all confined entirely to rows 0-1 (region 0 = column 0,
        // region 1 = column 1, region 2 = the 2x2 block spanning cols 2-3), but rows 0-1 hold
        // only two animals total (one per row). Three regions each requiring exactly one animal
        // cannot be satisfied by only two available marks, so this layout has zero valid
        // solutions outright -- not merely an ambiguous/multi-solution case -- and the dispatcher
        // must be unable to resolve it to a full solve.
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
