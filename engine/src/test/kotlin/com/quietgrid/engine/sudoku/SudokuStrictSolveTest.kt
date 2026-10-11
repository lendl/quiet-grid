package com.quietgrid.engine.sudoku

import com.quietgrid.engine.sudoku.SudokuGradingFixtures.parse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuStrictSolveTest {
    private fun techniquesUpTo(step: SudokuSkillStep) = SudokuTechnique.entries.filter { it.skillStep <= step }

    @Test
    fun `skill step membership matches the ladder table`() {
        val expected = mapOf(
            SudokuTechnique.NAKED_SINGLE to SudokuSkillStep.NAKED_SINGLES,
            SudokuTechnique.HIDDEN_SINGLE to SudokuSkillStep.HIDDEN_SINGLES,
            SudokuTechnique.NAKED_PAIR to SudokuSkillStep.SUBSETS_AND_LOCKED_CANDIDATES,
            SudokuTechnique.HIDDEN_PAIR to SudokuSkillStep.SUBSETS_AND_LOCKED_CANDIDATES,
            SudokuTechnique.POINTING_PAIR_TRIPLE to SudokuSkillStep.SUBSETS_AND_LOCKED_CANDIDATES,
            SudokuTechnique.BOX_LINE_REDUCTION to SudokuSkillStep.SUBSETS_AND_LOCKED_CANDIDATES,
            SudokuTechnique.X_WING to SudokuSkillStep.FISH_AND_WINGS,
            SudokuTechnique.SWORDFISH to SudokuSkillStep.FISH_AND_WINGS,
            SudokuTechnique.XY_WING to SudokuSkillStep.FISH_AND_WINGS,
            SudokuTechnique.XYZ_WING to SudokuSkillStep.CHAINS_AND_COLORING,
            SudokuTechnique.COLORING to SudokuSkillStep.CHAINS_AND_COLORING,
            SudokuTechnique.CHAINS to SudokuSkillStep.CHAINS_AND_COLORING,
        )
        assertEquals(expected, SudokuTechnique.entries.associateWith { it.skillStep })
    }

    @Test
    fun `strict solve required step equals the smallest step set that solves`() {
        for (cells in SudokuGradingFixtures.ALL) {
            val grid = parse(cells)
            val trace = traceStrictSolve(grid)
            assertTrue(cells, trace.solved)
            val required = trace.moves.maxOfOrNull { it.technique.skillStep } ?: SudokuSkillStep.NAKED_SINGLES
            assertTrue("$cells needs $required", traceHumanSolve(grid, techniquesUpTo(required)).solved)
            if (required > SudokuSkillStep.NAKED_SINGLES) {
                val below = SudokuSkillStep.entries[required.ordinal - 1]
                assertFalse("$cells solved below $required", traceHumanSolve(grid, techniquesUpTo(below)).solved)
            }
        }
    }

    @Test
    fun `strict solve stops at maxStep`() {
        assertFalse(traceStrictSolve(parse(SudokuGradingFixtures.S3_TWO_STUCK_POINTS), SudokuSkillStep.HIDDEN_SINGLES).solved)
    }

    @Test
    fun `strict solve never uses a step above maxStep`() {
        val trace = traceStrictSolve(parse(SudokuGradingFixtures.S5_ONE_STUCK_POINT), SudokuSkillStep.FISH_AND_WINGS)
        assertTrue(trace.moves.all { it.technique.skillStep <= SudokuSkillStep.FISH_AND_WINGS })
    }
}
