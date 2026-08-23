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

    @Test
    fun `pairing-hardest puzzle padded into hards step window classifies as hard`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = stepsOf(AnimalDokuTechnique.CONFINEMENT) + padded(4, AnimalDokuTechnique.PAIRING_2) + padded(4),
        )
        assertEquals(Difficulty.HARD, classifyAnimalDokuDifficulty(7, result))
    }

    @Test
    fun `too few steps for a hard size falls short of hards step floor`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = stepsOf(AnimalDokuTechnique.CONFINEMENT) + padded(3, AnimalDokuTechnique.PAIRING_2),
        )
        assertNull(classifyAnimalDokuDifficulty(7, result))
    }

    @Test
    fun `shallow chain under both expert floors no longer overlaps expert and lands in hard`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = stepsOf(AnimalDokuTechnique.CONFINEMENT) + stepsOf(AnimalDokuTechnique.CHAIN, AnimalDokuTechnique.CHAIN)
                .map { it.copy(chainDepth = 3) } + padded(7),
        )
        assertFalse(isExpertGradeChain(analyzeSolveResult(result)))
        assertEquals(Difficulty.HARD, classifyAnimalDokuDifficulty(7, result))
    }

    @Test
    fun `chain depth at experts depth floor classifies as expert regardless of repeat count`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = stepsOf(AnimalDokuTechnique.SINGLETON, AnimalDokuTechnique.CHAIN).map {
                if (it.technique == AnimalDokuTechnique.CHAIN) it.copy(chainDepth = 4) else it
            },
        )
        assertTrue(isExpertGradeChain(analyzeSolveResult(result)))
        assertEquals(Difficulty.EXPERT, classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `chain repeated at experts repeat floor classifies as expert even with shallow depth`() {
        val steps = mutableListOf(AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0))
        repeat(4) { steps.add(AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 2)) }
        val result = AnimalDokuSolveResult(solved = true, steps = steps)
        assertTrue(isExpertGradeChain(analyzeSolveResult(result)))
        assertEquals(Difficulty.EXPERT, classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `expert-grade chain outside experts size range does not fall back to a lower tier`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 5)),
        )
        assertNull(classifyAnimalDokuDifficulty(6, result))
    }

    @Test
    fun `size outside every tiers range returns null`() {
        val result = AnimalDokuSolveResult(solved = true, steps = padded(4))
        assertNull(classifyAnimalDokuDifficulty(20, result))
    }

    @Test
    fun `single confinement step at a hard size falls short of the step floor`() {
        val result = AnimalDokuSolveResult(solved = true, steps = stepsOf(AnimalDokuTechnique.CONFINEMENT))
        assertNull(classifyAnimalDokuDifficulty(9, result))
    }

    @Test
    fun `hard and expert now both allow size 9`() {
        assertTrue(9 in ANIMALDOKU_SIZES_BY_DIFFICULTY.getValue(Difficulty.HARD))
        assertTrue(9 in ANIMALDOKU_SIZES_BY_DIFFICULTY.getValue(Difficulty.EXPERT))
        assertFalse(6 in ANIMALDOKU_SIZES_BY_DIFFICULTY.getValue(Difficulty.HARD))
    }
}
