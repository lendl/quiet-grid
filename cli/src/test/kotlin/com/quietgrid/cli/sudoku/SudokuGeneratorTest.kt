package com.quietgrid.cli.sudoku

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.gradeSudoku
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuGeneratorTest {
    private fun assertGeneratedAs(difficulty: Difficulty, seed: Int, givenRange: IntRange) {
        val entry = generateSudokuPuzzle(difficulty, "s9", Random(seed))
        assertNotNull(entry)
        assertEquals(difficulty.key, entry!!.difficulty)
        assertEquals(difficulty, gradeSudoku(entry.givens)?.difficulty)
        assertEquals(1, countSudokuSolutions(entry.givens, limit = 2))
        val givenCount = entry.givens.sumOf { row -> row.count { it != null } }
        assertTrue("$givenCount givens", givenCount in givenRange)
    }

    @Test
    fun `generated easy puzzle grades easy with a unique solution`() {
        assertGeneratedAs(Difficulty.EASY, seed = 1, givenRange = 36..45)
    }

    @Test
    fun `generated medium puzzle grades medium with a unique solution`() {
        assertGeneratedAs(Difficulty.MEDIUM, seed = 2, givenRange = 30..36)
    }
}
