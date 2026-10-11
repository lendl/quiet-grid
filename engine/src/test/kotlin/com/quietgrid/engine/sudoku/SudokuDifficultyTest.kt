package com.quietgrid.engine.sudoku

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SudokuDifficultyTest {
    private val solved: SudokuGrid = listOf(
        listOf(5, 3, 4, 6, 7, 8, 9, 1, 2),
        listOf(6, 7, 2, 1, 9, 5, 3, 4, 8),
        listOf(1, 9, 8, 3, 4, 2, 5, 6, 7),
        listOf(8, 5, 9, 7, 6, 1, 4, 2, 3),
        listOf(4, 2, 6, 8, 5, 3, 7, 9, 1),
        listOf(7, 1, 3, 9, 2, 4, 8, 5, 6),
        listOf(9, 6, 1, 5, 3, 7, 2, 8, 4),
        listOf(2, 8, 7, 4, 1, 9, 6, 3, 5),
        listOf(3, 4, 5, 2, 8, 6, 1, 7, 9),
    )

    private fun grade(cells: String) = gradeSudoku(SudokuGradingFixtures.parse(cells))

    @Test
    fun `naked singles only grades easy`() {
        assertEquals(SudokuGrade(SudokuSkillStep.NAKED_SINGLES, 0, Difficulty.EASY), grade(SudokuGradingFixtures.S1_EASY))
    }

    @Test
    fun `few-clue naked singles puzzle grades easy`() {
        assertEquals(SudokuGrade(SudokuSkillStep.NAKED_SINGLES, 0, Difficulty.EASY), grade(SudokuGradingFixtures.S1_FROM_MEDIUM_LABEL))
    }

    @Test
    fun `three forced hidden singles grade medium`() {
        assertEquals(SudokuGrade(SudokuSkillStep.HIDDEN_SINGLES, 3, Difficulty.MEDIUM), grade(SudokuGradingFixtures.S2_THREE_USES))
    }

    @Test
    fun `one forced hidden single has no tier`() {
        assertEquals(SudokuGrade(SudokuSkillStep.HIDDEN_SINGLES, 1, null), grade(SudokuGradingFixtures.S2_ONE_USE))
    }

    @Test
    fun `two locked-candidate stuck points grade hard`() {
        assertEquals(SudokuGrade(SudokuSkillStep.SUBSETS_AND_LOCKED_CANDIDATES, 2, Difficulty.HARD), grade(SudokuGradingFixtures.S3_TWO_STUCK_POINTS))
    }

    @Test
    fun `many eliminations at one stuck point count once`() {
        assertEquals(
            SudokuGrade(SudokuSkillStep.SUBSETS_AND_LOCKED_CANDIDATES, 1, null),
            grade(SudokuGradingFixtures.S3_ONE_STUCK_POINT_MANY_ELIMINATIONS),
        )
    }

    @Test
    fun `one fish or wing stuck point has no tier`() {
        assertEquals(SudokuGrade(SudokuSkillStep.FISH_AND_WINGS, 1, null), grade(SudokuGradingFixtures.S4_ONE_STUCK_POINT))
    }

    @Test
    fun `two fish or wing stuck points grade expert`() {
        assertEquals(SudokuGrade(SudokuSkillStep.FISH_AND_WINGS, 2, Difficulty.EXPERT), grade(SudokuGradingFixtures.S4_TWO_STUCK_POINTS))
    }

    @Test
    fun `one chain stuck point grades expert`() {
        assertEquals(SudokuGrade(SudokuSkillStep.CHAINS_AND_COLORING, 1, Difficulty.EXPERT), grade(SudokuGradingFixtures.S5_ONE_STUCK_POINT))
    }

    @Test
    fun `blank grid cannot be graded`() {
        assertNull(grade(".".repeat(81)))
    }

    @Test
    fun `contradictory givens cannot be graded`() {
        assertNull(grade("11......." + ".".repeat(72)))
    }

    @Test
    fun `full grid grades easy with no uses`() {
        assertEquals(SudokuGrade(SudokuSkillStep.NAKED_SINGLES, 0, Difficulty.EASY), gradeSudoku(solved))
    }

    @Test
    fun `grade is null when maxStep is below the required step`() {
        assertNull(gradeSudoku(SudokuGradingFixtures.parse(SudokuGradingFixtures.S3_TWO_STUCK_POINTS), SudokuSkillStep.HIDDEN_SINGLES))
    }
}
