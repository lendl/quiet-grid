package com.quietgrid.cli.starbattle

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.starbattle.StarBattleTechnique
import com.quietgrid.engine.starbattle.classifyStarBattleK2Grade
import com.quietgrid.engine.starbattle.solveStarBattle
import org.junit.Assert.assertTrue
import org.junit.Test

class StarBattleTraceSanityTest {
    @Test
    fun `easy tier (reused AnimalDoku Medium) has no chain steps`() {
        var checked = 0
        repeat(5) {
            val entry = generateStarBattleK1PuzzleForTier(5, Difficulty.MEDIUM, "easy", "sbtrace") ?: return@repeat
            val result = solveStarBattle(entry.size, 1, entry.regions)
            assertTrue(result.solved)
            assertTrue(result.steps.none { it.technique == StarBattleTechnique.CHAIN })
            checked++
        }
        assertTrue("expected at least one entry to be generated for this tier", checked > 0)
    }

    @Test
    fun `a k=2 expert-grade solve trace actually contains a deep or repeated chain, not a lucky shallow one`() {
        var found = false
        repeat(20) {
            val size = (8..9).random()
            val solution = generateStarBattleSolution(size, 2) ?: return@repeat
            val initialRegions = growStarBattleRegions(size, solution) ?: return@repeat
            val repaired = repairStarBattleRegionsTowardUniqueSolution(size, 2, solution, initialRegions) ?: return@repeat
            if (classifyStarBattleK2Grade(repaired.solveResult) == "expert") {
                found = true
                val profile = com.quietgrid.engine.starbattle.analyzeStarBattleSolveResult(repaired.solveResult)
                assertTrue(profile.maxChainDepth >= 5 || profile.chainRepeats >= 5)
            }
        }
        assertTrue("expected at least one of 20 attempts to reach expert grade — if this fails consistently, Task 6's thresholds are miscalibrated for what this generator actually produces", found)
    }
}
