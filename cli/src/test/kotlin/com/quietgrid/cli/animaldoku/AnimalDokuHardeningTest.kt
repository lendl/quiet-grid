// cli/src/test/kotlin/com/quietgrid/cli/animaldoku/AnimalDokuHardeningTest.kt
package com.quietgrid.cli.animaldoku

import com.quietgrid.engine.animaldoku.AnimalDokuSolveResult
import com.quietgrid.engine.animaldoku.AnimalDokuSolveStep
import com.quietgrid.engine.animaldoku.AnimalDokuTechnique
import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

private fun repairedPuzzleWithinCeilingForTest(
    size: Int,
    maxHardestTechniqueOrdinal: Int,
    maxSeedAttempts: Int = 20,
): Pair<IntArray, AnimalDokuRepairedPuzzle> {
    repeat(maxSeedAttempts) {
        val (solution, repaired) = repairedPuzzleForTest(size)
        val profile = com.quietgrid.engine.animaldoku.analyzeSolveResult(repaired.solveResult)
        if (profile.hardestTechnique.ordinal <= maxHardestTechniqueOrdinal) return solution to repaired
    }
    throw AssertionError("Could not find a size-$size baseline within the ordinal-$maxHardestTechniqueOrdinal ceiling in $maxSeedAttempts seed attempts")
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
    fun `expert has no ceiling so a mutation reaching chain is always acceptable if not worse`() {
        val maxOrdinal = maxHardestTechniqueOrdinalFor(Difficulty.EXPERT)
        assertNull(maxOrdinal)
        val best = HardnessKey(hardestTechniqueOrdinal = AnimalDokuTechnique.PAIRING_3.ordinal, hardestTechniqueRepeatCount = 1, maxChainDepth = 0)
        val candidate = HardnessKey(hardestTechniqueOrdinal = AnimalDokuTechnique.CHAIN.ordinal, hardestTechniqueRepeatCount = 1, maxChainDepth = 3)
        assertTrue(isAcceptableHardeningCandidate(candidate, best, maxOrdinal))
    }

    @Test
    fun `hard rejects a mutation that would cross into chain territory even though it scores higher`() {
        val maxOrdinal = maxHardestTechniqueOrdinalFor(Difficulty.HARD)
        assertEquals(AnimalDokuTechnique.PAIRING_3.ordinal, maxOrdinal)
        val best = HardnessKey(hardestTechniqueOrdinal = AnimalDokuTechnique.PAIRING_3.ordinal, hardestTechniqueRepeatCount = 1, maxChainDepth = 0)
        val candidate = HardnessKey(hardestTechniqueOrdinal = AnimalDokuTechnique.CHAIN.ordinal, hardestTechniqueRepeatCount = 1, maxChainDepth = 3)
        assertFalse(isAcceptableHardeningCandidate(candidate, best, maxOrdinal))
    }

    @Test
    fun `hard accepts a mutation with more repeats as long as it stays within the pairing 3 ceiling`() {
        val maxOrdinal = maxHardestTechniqueOrdinalFor(Difficulty.HARD)
        val best = HardnessKey(hardestTechniqueOrdinal = AnimalDokuTechnique.PAIRING_3.ordinal, hardestTechniqueRepeatCount = 1, maxChainDepth = 0)
        val candidate = HardnessKey(hardestTechniqueOrdinal = AnimalDokuTechnique.PAIRING_3.ordinal, hardestTechniqueRepeatCount = 3, maxChainDepth = 0)
        assertTrue(isAcceptableHardeningCandidate(candidate, best, maxOrdinal))
    }

    @Test
    fun `a candidate that would score lower than the current best is rejected regardless of tier`() {
        val best = HardnessKey(hardestTechniqueOrdinal = AnimalDokuTechnique.PAIRING_3.ordinal, hardestTechniqueRepeatCount = 4, maxChainDepth = 0)
        val worse = HardnessKey(hardestTechniqueOrdinal = AnimalDokuTechnique.PAIRING_2.ordinal, hardestTechniqueRepeatCount = 4, maxChainDepth = 0)
        assertFalse(isAcceptableHardeningCandidate(worse, best, maxHardestTechniqueOrdinalFor(Difficulty.HARD)))
        assertFalse(isAcceptableHardeningCandidate(worse, best, maxHardestTechniqueOrdinalFor(Difficulty.EXPERT)))
    }

    @Test
    fun `an equal candidate counts as acceptable a plateau not a regression`() {
        val best = HardnessKey(hardestTechniqueOrdinal = AnimalDokuTechnique.PAIRING_3.ordinal, hardestTechniqueRepeatCount = 4, maxChainDepth = 0)
        val same = best.copy()
        assertTrue(isAcceptableHardeningCandidate(same, best, maxHardestTechniqueOrdinalFor(Difficulty.HARD)))
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
    fun `hardenTowardDifficulty for hard never lets the hardest technique exceed pairing 3`() {
        val (solution, repaired) = repairedPuzzleWithinCeilingForTest(7, AnimalDokuTechnique.PAIRING_3.ordinal)

        val hardened = hardenTowardDifficulty(7, solution, repaired.regions, repaired.solveResult, Difficulty.HARD, maxStallMutations = 800)

        val profile = com.quietgrid.engine.animaldoku.analyzeSolveResult(hardened.solveResult)
        assertTrue(
            "hardest technique ${profile.hardestTechnique} exceeded PAIRING_3 during HARD hardening",
            profile.hardestTechnique.ordinal <= AnimalDokuTechnique.PAIRING_3.ordinal,
        )
    }

    @Test
    fun `hardenTowardDifficulty stops early once the target tier is actually reached`() {
        val (solution, repaired) = repairedPuzzleForTest(6)
        val alreadyClassified = com.quietgrid.engine.animaldoku.classifyAnimalDokuDifficulty(6, repaired.solveResult)

        if (alreadyClassified != null) {
            val hardened = hardenTowardDifficulty(6, solution, repaired.regions, repaired.solveResult, alreadyClassified, maxStallMutations = 500)
            assertEquals(repaired.regions, hardened.regions)
        }
    }
}
