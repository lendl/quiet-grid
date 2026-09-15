package com.quietgrid.cli.starbattle

import com.quietgrid.engine.starbattle.isValidStarBattleRegionGrid
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StarBattleK2GeneratorTest {
    @Test
    fun `grows a valid region grid from a k=2 solution`() {
        val solution = generateStarBattleSolution(8, 2)
        assertNotNull(solution)
        val regions = growStarBattleRegions(8, solution!!)
        assertNotNull(regions)
        assertTrue(isValidStarBattleRegionGrid(8, 2, regions!!))
    }

    @Test
    fun `repair eventually reaches a uniquely-solvable region grid`() {
        val solution = generateStarBattleSolution(8, 2)!!
        val initialRegions = growStarBattleRegions(8, solution)!!
        val repaired = repairStarBattleRegionsTowardUniqueSolution(8, 2, solution, initialRegions)
        assertNotNull(repaired)
        assertTrue(repaired!!.solveResult.solved)
    }
}
