// engine/src/test/kotlin/com/quietgrid/engine/animaldoku/AnimalDokuDifficultyTest.kt
package com.quietgrid.engine.animaldoku

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun stepsOf(vararg techniques: AnimalDokuTechnique): List<AnimalDokuSolveStep> =
    techniques.map { AnimalDokuSolveStep(it, 0) }

private fun padded(count: Int, technique: AnimalDokuTechnique = AnimalDokuTechnique.SINGLETON): List<AnimalDokuSolveStep> =
    List(count) { AnimalDokuSolveStep(technique, 0) }

class AnimalDokuDifficultyTest {
    @Test
    fun `classifyAnimalDokuDifficulty returns null for an unsolved result`() {
        val result = AnimalDokuSolveResult(solved = false, steps = emptyList())
        assertNull(classifyAnimalDokuDifficulty(6, result))
    }

    @Test
    fun `analyzeSolveResult returns hardest technique its repeats max chain depth and chain repeats`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 4),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 7),
            ),
        )
        val profile = analyzeSolveResult(result)
        assertEquals(AnimalDokuTechnique.CHAIN, profile.hardestTechnique)
        assertEquals(2, profile.hardestTechniqueRepeats)
        assertEquals(7, profile.maxChainDepth)
        assertEquals(2, profile.chainRepeats)
    }

    @Test
    fun `singleton-only puzzle within easy size and step range classifies as easy`() {
        val result = AnimalDokuSolveResult(solved = true, steps = padded(4))
        assertEquals(Difficulty.EASY, classifyAnimalDokuDifficulty(5, result))
    }

    @Test
    fun `too few steps for an easy size falls short of easys step floor`() {
        val result = AnimalDokuSolveResult(solved = true, steps = padded(3))
        assertNull(classifyAnimalDokuDifficulty(5, result))
    }

    @Test
    fun `a structural confinement step is as trivial as singleton and still counts as easy`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = stepsOf(AnimalDokuTechnique.STRUCTURAL_CONFINEMENT) + padded(5),
        )
        assertEquals(Difficulty.EASY, classifyAnimalDokuDifficulty(5, result))
    }

    @Test
    fun `a genuine dynamic confinement step is medium, not easy, even at an easy size`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = stepsOf(AnimalDokuTechnique.CONFINEMENT) + padded(5),
        )
        assertEquals(Difficulty.MEDIUM, classifyAnimalDokuDifficulty(5, result))
    }

    @Test
    fun `confinement-hardest puzzle padded into mediums step window classifies as medium`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = stepsOf(AnimalDokuTechnique.SINGLETON, AnimalDokuTechnique.CONFINEMENT) + padded(4),
        )
        assertEquals(Difficulty.MEDIUM, classifyAnimalDokuDifficulty(4, result))
    }

    @Test
    fun `confinement-hardest puzzle too short for mediums step floor no longer overlaps easy`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = stepsOf(AnimalDokuTechnique.SINGLETON, AnimalDokuTechnique.CONFINEMENT),
        )
        assertNull(classifyAnimalDokuDifficulty(4, result))
    }

    private fun chainResult(chains: Int, depth: Int, stepsBeforeFirstPlacement: Int, trailingSingletons: Int = 5): AnimalDokuSolveResult {
        val lead = padded(stepsBeforeFirstPlacement - minOf(chains, stepsBeforeFirstPlacement), AnimalDokuTechnique.CONFINEMENT) +
            List(minOf(chains, stepsBeforeFirstPlacement)) { AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, depth) }
        val rest = List(chains - minOf(chains, stepsBeforeFirstPlacement)) { AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, depth) }
        return AnimalDokuSolveResult(solved = true, steps = lead + padded(1) + rest + padded(trailingSingletons))
    }

    @Test
    fun `analyzeSolveResult counts the steps before the first placement`() {
        val profile = analyzeSolveResult(chainResult(chains = 4, depth = 3, stepsBeforeFirstPlacement = 6))
        assertEquals(6, profile.stepsBeforeFirstPlacement)
        assertEquals(4, profile.chainRepeats)
    }

    @Test
    fun `analyzeSolveResult counts every step when nothing is ever placed`() {
        val result = AnimalDokuSolveResult(solved = true, steps = padded(3, AnimalDokuTechnique.CONFINEMENT))
        assertEquals(3, analyzeSolveResult(result).stepsBeforeFirstPlacement)
    }

    @Test
    fun `enough chains and a delayed first placement classify as hard at a hard size`() {
        assertEquals(Difficulty.HARD, classifyAnimalDokuDifficulty(8, chainResult(chains = 3, depth = 3, stepsBeforeFirstPlacement = 3)))
    }

    @Test
    fun `too few chains is not hard`() {
        assertNull(classifyAnimalDokuDifficulty(8, chainResult(chains = 2, depth = 3, stepsBeforeFirstPlacement = 3)))
    }

    @Test
    fun `an early first placement is not hard`() {
        assertNull(classifyAnimalDokuDifficulty(8, chainResult(chains = 3, depth = 3, stepsBeforeFirstPlacement = 2)))
    }

    @Test
    fun `pairing without chains no longer reaches hard`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = stepsOf(AnimalDokuTechnique.CONFINEMENT) + padded(4, AnimalDokuTechnique.PAIRING_2) + padded(8),
        )
        assertNull(classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `a hard grade puzzle below hards sizes is rejected`() {
        assertNull(classifyAnimalDokuDifficulty(7, chainResult(chains = 3, depth = 3, stepsBeforeFirstPlacement = 3)))
    }

    @Test
    fun `many deep chains and a long opening classify as expert`() {
        val result = chainResult(chains = 10, depth = 6, stepsBeforeFirstPlacement = 8)
        assertTrue(isExpertGradeChain(analyzeSolveResult(result)))
        assertEquals(Difficulty.EXPERT, classifyAnimalDokuDifficulty(10, result))
    }

    @Test
    fun `expert needs every floor so a shallow chain falls back to hard`() {
        val result = chainResult(chains = 10, depth = 5, stepsBeforeFirstPlacement = 8)
        assertFalse(isExpertGradeChain(analyzeSolveResult(result)))
        assertEquals(Difficulty.HARD, classifyAnimalDokuDifficulty(9, result))
    }

    @Test
    fun `expert needs every floor so too few chains falls back to hard`() {
        val result = chainResult(chains = 9, depth = 6, stepsBeforeFirstPlacement = 8)
        assertEquals(Difficulty.HARD, classifyAnimalDokuDifficulty(9, result))
    }

    @Test
    fun `expert needs every floor so an early placement falls back to hard`() {
        val result = chainResult(chains = 10, depth = 6, stepsBeforeFirstPlacement = 7)
        assertEquals(Difficulty.HARD, classifyAnimalDokuDifficulty(9, result))
    }

    @Test
    fun `expert-grade chain outside experts size range does not fall back to a lower tier`() {
        assertNull(classifyAnimalDokuDifficulty(8, chainResult(chains = 10, depth = 6, stepsBeforeFirstPlacement = 8)))
    }

    @Test
    fun `size outside every tiers range returns null`() {
        val result = AnimalDokuSolveResult(solved = true, steps = padded(4))
        assertNull(classifyAnimalDokuDifficulty(20, result))
    }

    @Test
    fun `hard covers sizes 8 and 9 and expert covers 9 and 10`() {
        assertEquals(listOf(8, 9), ANIMALDOKU_SIZES_BY_DIFFICULTY.getValue(Difficulty.HARD))
        assertEquals(listOf(9, 10), ANIMALDOKU_SIZES_BY_DIFFICULTY.getValue(Difficulty.EXPERT))
    }

    private fun regionsWithSizes(size: Int, sizes: List<Int>): List<List<Int>> {
        val flat = sizes.flatMapIndexed { region, count -> List(count) { region } }
        return flat.chunked(size)
    }

    @Test
    fun `hard rejects a region smaller than three cells`() {
        assertFalse(meetsAnimalDokuRegionShape(Difficulty.HARD, 8, regionsWithSizes(8, listOf(2, 10, 8, 8, 8, 8, 10, 10))))
        assertTrue(meetsAnimalDokuRegionShape(Difficulty.HARD, 8, regionsWithSizes(8, listOf(3, 9, 8, 8, 8, 8, 10, 10))))
    }

    @Test
    fun `expert needs every region at least four cells and one covering a quarter of the grid`() {
        assertTrue(meetsAnimalDokuRegionShape(Difficulty.EXPERT, 10, regionsWithSizes(10, listOf(4, 25, 8, 8, 8, 8, 9, 10, 10, 10))))
        assertFalse(meetsAnimalDokuRegionShape(Difficulty.EXPERT, 10, regionsWithSizes(10, listOf(3, 26, 8, 8, 8, 8, 9, 10, 10, 10))))
        assertFalse(meetsAnimalDokuRegionShape(Difficulty.EXPERT, 10, regionsWithSizes(10, listOf(10, 10, 10, 10, 10, 10, 10, 10, 10, 10))))
    }

    @Test
    fun `classifyAnimalDokuPuzzle drops a puzzle whose regions miss its tiers shape`() {
        val result = chainResult(chains = 10, depth = 6, stepsBeforeFirstPlacement = 8)
        val flat = regionsWithSizes(10, List(10) { 10 })
        val shaped = regionsWithSizes(10, listOf(4, 25, 8, 8, 8, 8, 9, 10, 10, 10))
        assertNull(classifyAnimalDokuPuzzle(10, flat, result))
        assertEquals(Difficulty.EXPERT, classifyAnimalDokuPuzzle(10, shaped, result))
    }
}
