package com.quietgrid.cli.starbattle

import com.quietgrid.engine.starbattle.classifyStarBattleK2Grade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private const val SOFTENING_SEED = 8

private fun expertK2PuzzleForTest(random: Random, size: Int = 8, maxSeedAttempts: Int = 40): Pair<List<List<Int>>, StarBattleRepairedPuzzle> {
    repeat(maxSeedAttempts) {
        val solution = generateStarBattleSolution(size, 2, random = random) ?: return@repeat
        val initialRegions = growStarBattleRegions(size, solution, random) ?: return@repeat
        val repaired = repairStarBattleRegionsTowardUniqueSolution(size, 2, solution, initialRegions, random = random) ?: return@repeat
        if (classifyStarBattleK2Grade(repaired.solveResult) == "expert") return solution to repaired
    }
    throw AssertionError("Could not naturally produce an expert-grade size-$size k=2 puzzle in $maxSeedAttempts seed attempts")
}

private fun softenedGrade(targetGrade: String, maxStallMutations: Int): String? {
    val random = Random(SOFTENING_SEED)
    val (solution, repaired) = expertK2PuzzleForTest(random)
    val softened = softenStarBattleTowardGrade(
        size = 8,
        k = 2,
        solution = solution,
        initialRegions = repaired.regions,
        initialSolveResult = repaired.solveResult,
        targetGrade = targetGrade,
        maxStallMutations = maxStallMutations,
        random = random,
    )
    return classifyStarBattleK2Grade(softened.solveResult)
}

class StarBattleK2SofteningTest {
    @Test
    fun `naturally repaired k=2 puzzles at size 8 classify as expert confirming the known gap`() {
        val (_, repaired) = expertK2PuzzleForTest(Random(SOFTENING_SEED))
        assertEquals("expert", classifyStarBattleK2Grade(repaired.solveResult))
    }

    @Test
    fun `softenStarBattleTowardGrade can redirect an expert puzzle to medium`() {
        assertEquals("seed $SOFTENING_SEED should soften to medium; pick a new seed if the generator or solver changed", "medium", softenedGrade("medium", maxStallMutations = 3000))
    }

    @Test
    fun `softenStarBattleTowardGrade can redirect an expert puzzle to hard`() {
        assertEquals("seed $SOFTENING_SEED should soften to hard; pick a new seed if the generator or solver changed", "hard", softenedGrade("hard", maxStallMutations = 6000))
    }

    @Test
    fun `softenStarBattleTowardGrade never returns an unsolved puzzle`() {
        val random = Random(SOFTENING_SEED)
        val (solution, repaired) = expertK2PuzzleForTest(random)
        val softened = softenStarBattleTowardGrade(8, 2, solution, repaired.regions, repaired.solveResult, "medium", maxStallMutations = 2000, random = random)
        assertTrue(softened.solveResult.solved)
    }
}
