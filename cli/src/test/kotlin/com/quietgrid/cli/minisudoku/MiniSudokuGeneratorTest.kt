package com.quietgrid.cli.minisudoku

import com.quietgrid.cli.sudoku.countSudokuSolutions
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.MINI_SUDOKU_SIZE_BY_DIFFICULTY
import com.quietgrid.engine.sudoku.classifyMiniSudoku
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MiniSudokuGeneratorTest {
    @Test
    fun `createMiniSudokuSolution fills a valid six by six grid`() {
        val solution = createMiniSudokuSolution(6)
        assertTrue(solution.all { row -> row.sorted() == (1..6).toList() })
        assertTrue((0 until 6).all { c -> solution.map { it[c] }.sorted() == (1..6).toList() })
        assertEquals(1, countSudokuSolutions(solution, limit = 2))
    }

    @Test
    fun `generateMiniSudokuPuzzle hits every tier with a unique solution`() {
        val random = Random(11)
        for (difficulty in Difficulty.entries) {
            val entry = generateMiniSudokuPuzzle(difficulty, random)
            assertNotNull(difficulty.key, entry)
            assertEquals(difficulty.key, entry!!.difficulty)
            assertEquals(MINI_SUDOKU_SIZE_BY_DIFFICULTY.getValue(difficulty), entry.givens.size)
            assertEquals(1, countSudokuSolutions(entry.givens, limit = 2))
            assertEquals(difficulty, classifyMiniSudoku(entry.givens))
        }
    }
}
