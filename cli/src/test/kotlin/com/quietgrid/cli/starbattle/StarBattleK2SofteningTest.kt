package com.quietgrid.cli.starbattle

import com.quietgrid.engine.starbattle.classifyStarBattleK2Grade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun expertK2PuzzleForTest(size: Int = 8, maxSeedAttempts: Int = 40): Pair<List<List<Int>>, StarBattleRepairedPuzzle> {
    repeat(maxSeedAttempts) {
        val solution = generateStarBattleSolution(size, 2) ?: return@repeat
        val initialRegions = growStarBattleRegions(size, solution) ?: return@repeat
        val repaired = repairStarBattleRegionsTowardUniqueSolution(size, 2, solution, initialRegions) ?: return@repeat
        if (classifyStarBattleK2Grade(repaired.solveResult) == "expert") return solution to repaired
    }
    throw AssertionError("Could not naturally produce an expert-grade size-$size k=2 puzzle in $maxSeedAttempts seed attempts")
}

class StarBattleK2SofteningTest {
    @Test
    fun `naturally repaired k=2 puzzles at size 8 classify as expert confirming the known gap`() {
        val (_, repaired) = expertK2PuzzleForTest()
        assertEquals("expert", classifyStarBattleK2Grade(repaired.solveResult))
    }

    @Test
    fun `softenStarBattleTowardGrade can redirect an expert puzzle to medium`() {
        val maxAttempts = 10
        var reached = false
        var lastGrade: String? = null
        repeat(maxAttempts) {
            if (reached) return@repeat
            val (solution, repaired) = expertK2PuzzleForTest()
            val softened = softenStarBattleTowardGrade(
                size = 8,
                k = 2,
                solution = solution,
                initialRegions = repaired.regions,
                initialSolveResult = repaired.solveResult,
                targetGrade = "medium",
                maxStallMutations = 3000,
            )
            lastGrade = classifyStarBattleK2Grade(softened.solveResult)
            if (lastGrade == "medium") reached = true
        }
        assertTrue(
            "expected softenStarBattleTowardGrade to reach medium within $maxAttempts fresh-puzzle attempts, last grade was $lastGrade",
            reached,
        )
    }

    @Test
    fun `softenStarBattleTowardGrade can redirect an expert puzzle to hard`() {
        val maxAttempts = 15
        var reached = false
        var lastGrade: String? = null
        repeat(maxAttempts) {
            if (reached) return@repeat
            val (solution, repaired) = expertK2PuzzleForTest()
            val softened = softenStarBattleTowardGrade(
                size = 8,
                k = 2,
                solution = solution,
                initialRegions = repaired.regions,
                initialSolveResult = repaired.solveResult,
                targetGrade = "hard",
                maxStallMutations = 6000,
            )
            lastGrade = classifyStarBattleK2Grade(softened.solveResult)
            if (lastGrade == "hard") reached = true
        }
        assertTrue(
            "expected softenStarBattleTowardGrade to reach hard within $maxAttempts fresh-puzzle attempts, last grade was $lastGrade",
            reached,
        )
    }

    @Test
    fun `softenStarBattleTowardGrade never returns an unsolved puzzle`() {
        val (solution, repaired) = expertK2PuzzleForTest()
        val softened = softenStarBattleTowardGrade(8, 2, solution, repaired.regions, repaired.solveResult, "medium", maxStallMutations = 2000)
        assertTrue(softened.solveResult.solved)
    }
}
