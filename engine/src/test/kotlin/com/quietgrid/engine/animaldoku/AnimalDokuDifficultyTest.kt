// engine/src/test/kotlin/com/quietgrid/engine/animaldoku/AnimalDokuDifficultyTest.kt
package com.quietgrid.engine.animaldoku

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimalDokuDifficultyTest {
    @Test
    fun `classifyAnimalDokuDifficulty returns null for an unsolved result`() {
        val result = AnimalDokuSolveResult(solved = false, steps = emptyList())
        assertNull(classifyAnimalDokuDifficulty(6, result))
    }

    @Test
    fun `analyzeSolveResult returns hardest technique its repeats max chain depth and opening technique`() {
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
        assertEquals(AnimalDokuTechnique.SINGLETON, profile.openingTechnique)
    }

    @Test
    fun `singleton-only within easy size range classifies as easy`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0)),
        )
        assertEquals(Difficulty.EASY, classifyAnimalDokuDifficulty(5, result))
    }

    @Test
    fun `confinement-hardest puzzle at an easy size classifies as medium not easy`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
            ),
        )
        assertEquals(Difficulty.MEDIUM, classifyAnimalDokuDifficulty(4, result))
    }

    @Test
    fun `confinement at size outside every tiers range does not classify as easy`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0)),
        )
        assertNull(classifyAnimalDokuDifficulty(9, result))
    }

    @Test
    fun `pairing K equals 2 repeated four times opening with confinement classifies as hard`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
            ),
        )
        assertEquals(Difficulty.HARD, classifyAnimalDokuDifficulty(7, result))
    }

    @Test
    fun `pairing repeated only three times falls short of hards raised repeat floor`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
            ),
        )
        assertNull(classifyAnimalDokuDifficulty(7, result))
    }

    @Test
    fun `chain depth 3 repeated twice within expert size range classifies as expert`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3),
            ),
        )
        assertEquals(Difficulty.EXPERT, classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `chain depth 2 no longer reaches experts raised depth floor`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 2),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 2),
            ),
        )
        assertNull(classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `a lone chain step opening the puzzle fails on both opening cap and repeat count`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3)),
        )
        assertNull(classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `size outside every tiers range returns null`() {
        val result = AnimalDokuSolveResult(solved = true, steps = listOf(AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0)))
        assertNull(classifyAnimalDokuDifficulty(20, result))
    }

    @Test
    fun `chain outside expert size range does not classify as expert`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3)),
        )
        assertNull(classifyAnimalDokuDifficulty(6, result))
    }

    @Test
    fun `opening step harder than a tiers maxOpeningTechnique excludes that tier`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
            ),
        )
        assertNull(classifyAnimalDokuDifficulty(7, result))
    }

    @Test
    fun `hardest technique firing only once fails a tiers minHardestTechniqueRepeats`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
            ),
        )
        assertNull(classifyAnimalDokuDifficulty(6, result))
    }

    @Test
    fun `both new checks passing alongside the existing ceiling check classifies as hard`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
            ),
        )
        assertEquals(Difficulty.HARD, classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `chain hardest at depth 3 opening at the PAIRING_3 cap boundary classifies as expert`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3),
            ),
        )
        assertEquals(Difficulty.EXPERT, classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `chain hardest firing only once no longer classifies as expert now that its repeat floor is back to 2`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3),
            ),
        )
        assertNull(classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `real competitor puzzle trace classifies as expert under the recalibrated profile`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 6),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 6),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 6),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
            ),
        )
        assertEquals(Difficulty.EXPERT, classifyAnimalDokuDifficulty(8, result))
    }
}
