// cli/src/test/kotlin/com/quietgrid/cli/animaldoku/AnimalDokuHardeningTest.kt
package com.quietgrid.cli.animaldoku

import com.quietgrid.engine.animaldoku.AnimalDokuSolveResult
import com.quietgrid.engine.animaldoku.AnimalDokuSolveStep
import com.quietgrid.engine.animaldoku.AnimalDokuTechnique
import com.quietgrid.engine.animaldoku.classifyAnimalDokuDifficulty
import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun repairedPuzzleForTest(size: Int, maxSeedAttempts: Int = 20): Pair<IntArray, AnimalDokuRepairedPuzzle> {
    repeat(maxSeedAttempts) {
        val solution = generateSolutionPermutation(size) ?: return@repeat
        val initialRegions = growRegions(size, solution) ?: return@repeat
        val repaired = repairRegionsTowardUniqueSolution(size, solution, initialRegions, 400) ?: return@repeat
        return solution to repaired
    }
    throw AssertionError("Could not repair a size-$size layout to uniqueness in $maxSeedAttempts seed attempts")
}

class AnimalDokuHardeningTest {
    @Test
    fun `hardnessKeyOf reflects hardest technique its repeat count and max chain depth`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 4),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 6),
            ),
        )
        val key = hardnessKeyOf(result)
        assertEquals(AnimalDokuTechnique.CHAIN.ordinal, key.hardestTechniqueOrdinal)
        assertEquals(2, key.hardestTechniqueRepeatCount)
        assertEquals(6, key.maxChainDepth)
    }

    @Test
    fun `HardnessKey compares lexicographically by technique ordinal first`() {
        val weakerTechnique = HardnessKey(hardestTechniqueOrdinal = 1, hardestTechniqueRepeatCount = 99, maxChainDepth = 99)
        val strongerTechnique = HardnessKey(hardestTechniqueOrdinal = 2, hardestTechniqueRepeatCount = 0, maxChainDepth = 0)
        assertTrue(strongerTechnique > weakerTechnique)
    }

    @Test
    fun `HardnessKey compares by repeat count when technique ordinal ties`() {
        val fewerRepeats = HardnessKey(hardestTechniqueOrdinal = 3, hardestTechniqueRepeatCount = 2, maxChainDepth = 0)
        val moreRepeats = HardnessKey(hardestTechniqueOrdinal = 3, hardestTechniqueRepeatCount = 5, maxChainDepth = 0)
        assertTrue(moreRepeats > fewerRepeats)
    }

    @Test
    fun `HardnessKey compares by chain depth when technique and repeat count tie`() {
        val shallower = HardnessKey(hardestTechniqueOrdinal = 5, hardestTechniqueRepeatCount = 2, maxChainDepth = 3)
        val deeper = HardnessKey(hardestTechniqueOrdinal = 5, hardestTechniqueRepeatCount = 2, maxChainDepth = 6)
        assertTrue(deeper > shallower)
    }

    @Test
    fun `expert has no ceiling so a candidate that already classifies as expert is still acceptable`() {
        val size = 8
        val candidateResult = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 4)),
        )
        assertEquals(Difficulty.EXPERT, classifyAnimalDokuDifficulty(size, candidateResult))
        val best = HardnessKey(hardestTechniqueOrdinal = AnimalDokuTechnique.PAIRING_3.ordinal, hardestTechniqueRepeatCount = 1, maxChainDepth = 0)
        val candidate = hardnessKeyOf(candidateResult)
        assertTrue(isAcceptableHardeningCandidate(size, candidateResult, candidate, best, Difficulty.EXPERT))
    }

    @Test
    fun `hard rejects a candidate that would already classify as expert`() {
        val size = 8
        val candidateResult = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 4)),
        )
        assertEquals(Difficulty.EXPERT, classifyAnimalDokuDifficulty(size, candidateResult))
        val best = HardnessKey(hardestTechniqueOrdinal = AnimalDokuTechnique.PAIRING_3.ordinal, hardestTechniqueRepeatCount = 1, maxChainDepth = 0)
        val candidate = hardnessKeyOf(candidateResult)
        assertFalse(isAcceptableHardeningCandidate(size, candidateResult, candidate, best, Difficulty.HARD))
    }

    @Test
    fun `hard accepts a candidate with a shallow chain that still classifies as hard`() {
        val size = 8
        val steps = mutableListOf(AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0))
        repeat(2) { steps.add(AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3)) }
        repeat(7) { steps.add(AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0)) }
        val candidateResult = AnimalDokuSolveResult(solved = true, steps = steps)
        assertEquals(Difficulty.HARD, classifyAnimalDokuDifficulty(size, candidateResult))
        val best = HardnessKey(hardestTechniqueOrdinal = AnimalDokuTechnique.PAIRING_3.ordinal, hardestTechniqueRepeatCount = 1, maxChainDepth = 0)
        val candidate = hardnessKeyOf(candidateResult)
        assertTrue(isAcceptableHardeningCandidate(size, candidateResult, candidate, best, Difficulty.HARD))
    }

    @Test
    fun `a candidate that would score lower than the current best is rejected regardless of tier`() {
        val size = 8
        val worseResult = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0)),
        )
        val best = HardnessKey(hardestTechniqueOrdinal = AnimalDokuTechnique.PAIRING_3.ordinal, hardestTechniqueRepeatCount = 4, maxChainDepth = 0)
        val worse = hardnessKeyOf(worseResult)
        assertFalse(isAcceptableHardeningCandidate(size, worseResult, worse, best, Difficulty.HARD))
        assertFalse(isAcceptableHardeningCandidate(size, worseResult, worse, best, Difficulty.EXPERT))
    }

    @Test
    fun `an equal candidate counts as acceptable a plateau not a regression`() {
        val size = 8
        val sameResult = AnimalDokuSolveResult(
            solved = true,
            steps = List(4) { AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0) },
        )
        val best = hardnessKeyOf(sameResult)
        assertTrue(isAcceptableHardeningCandidate(size, sameResult, best, best, Difficulty.HARD))
    }

    @Test
    fun `hardenTowardDifficulty never returns a puzzle easier than the one it started with`() {
        val (solution, repaired) = repairedPuzzleForTest(6)

        val initialKey = hardnessKeyOf(repaired.solveResult)
        val hardened = hardenTowardDifficulty(6, solution, repaired.regions, repaired.solveResult, Difficulty.HARD, maxStallMutations = 500)
        val hardenedKey = hardnessKeyOf(hardened.solveResult)

        assertTrue(hardenedKey >= initialKey)
        assertTrue(hardened.solveResult.solved)
    }

    @Test
    fun `hardenTowardDifficulty for hard never lets the result classify as expert`() {
        val (solution, repaired) = repairedPuzzleForTest(7)

        val hardened = hardenTowardDifficulty(7, solution, repaired.regions, repaired.solveResult, Difficulty.HARD, maxStallMutations = 800)

        assertNotEquals(Difficulty.EXPERT, classifyAnimalDokuDifficulty(7, hardened.solveResult))
    }

    @Test
    fun `hardenTowardDifficulty stops early once the target tier is actually reached`() {
        val (solution, repaired) = repairedPuzzleForTest(6)
        val alreadyClassified = classifyAnimalDokuDifficulty(6, repaired.solveResult)

        if (alreadyClassified != null) {
            val hardened = hardenTowardDifficulty(6, solution, repaired.regions, repaired.solveResult, alreadyClassified, maxStallMutations = 500)
            assertEquals(repaired.regions, hardened.regions)
        }
    }
}
